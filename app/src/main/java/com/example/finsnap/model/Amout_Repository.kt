package com.example.finsnap.model



import android.content.Context
import android.provider.Telephony
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Amout_Repository(private val context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        UserDatabase::class.java,
        "UserDatabase"
    ).fallbackToDestructiveMigration().build()

    private val usersDao = database.UsersDao()

    // Add to Amout_Repository.kt
    suspend fun getUserExists(email: String): Boolean {
        return usersDao.getUserByEmail(email) != null
    }

    // ✅ Get all transactions from Room (LiveData)
    fun getAllUserAmountsLive(): LiveData<List<UserAmount>> {
        return usersDao.getAllUserAmounts()
    }

    // ✅ Update a transaction in Room
    suspend fun updateUserAmount(userAmount: UserAmount) {
        usersDao.updateUserAmount(userAmount)
    }

    // ✅ Use this when you want a list without observing
    suspend fun getBankSmsTransactions(): List<UserAmount> {
        val dbData = usersDao.getAllUserAmountsNow()
        if (dbData.isNotEmpty()) {
            return dbData
        }

        // Parse from SMS only if DB is empty
        val parsed = parseSmsTransactions()
        parsed.forEach {
            usersDao.insertUserAmount(it)
        }
        return parsed
    }

    private suspend fun parseSmsTransactions(): List<UserAmount> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<UserAmount>()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -24)
        val timestamp = calendar.timeInMillis

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(timestamp.toString())

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val messageBody = cursor.getStringOrNull(bodyIndex) ?: continue
                    val messageTimestamp = cursor.getLong(dateIndex)

                    if (containsBankName(messageBody)) {
                        val date = Date(messageTimestamp)
                        val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(date)

                        val (amount, isCredit, sender) = extractTransactionInfo(messageBody)

                        val imageResource = R.drawable.logo
                        val amountFormatted = if (isCredit) "+₹$amount" else "-₹$amount"
                        val displaySender = if (sender.isNotEmpty()) sender else if (isCredit) "Unknown Sender" else "Unknown Recipient"
                        val numericAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0

                        smsList.add(
                            UserAmount(
                                sender = displaySender,
                                time = formattedTime,
                                amtChange = amountFormatted,
                                amtImage = imageResource,
                                rawMessage = messageBody,
                                amount = numericAmount,
                                isCredit = isCredit
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext smsList
    }

    private fun extractTransactionInfo(message: String): Triple<String, Boolean, String> {
        var amount = ""
        var isCredit = false
        var recipient = ""

        val amountPattern = Pattern.compile("(?i)(?:Rs\\.?|INR|₹|by|for)\\s*(\\d+(?:[.,]\\d+)?)")
        val amountMatcher = amountPattern.matcher(message)
        if (amountMatcher.find()) amount = amountMatcher.group(1) ?: ""

        val messageLower = message.lowercase()
        isCredit = messageLower.contains("credited") || messageLower.contains("received") ||
                messageLower.contains("added") || messageLower.contains("deposited")

        if (messageLower.contains("trf to ")) {
            val trfPattern = Pattern.compile("(?i)trf\\s+to\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:Refno|Ref|Reference|\\.|$))")
            val trfMatcher = trfPattern.matcher(message)
            if (trfMatcher.find()) recipient = trfMatcher.group(1)?.trim() ?: ""
        }

        if (recipient.isEmpty()) {
            val patterns = if (isCredit)
                listOf(
                    Pattern.compile("(?i)credited\\s+(?:by|from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))"),
                    Pattern.compile("(?i)received\\s+(?:from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))"),
                    Pattern.compile("(?i)from\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))")
                )
            else
                listOf(
                    Pattern.compile("(?i)debited\\s+(?:to|for)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))"),
                    Pattern.compile("(?i)paid\\s+(?:to)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))"),
                    Pattern.compile("(?i)to\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))")
                )

            for (pattern in patterns) {
                val matcher = pattern.matcher(message)
                if (matcher.find()) {
                    recipient = matcher.group(1)?.trim() ?: ""
                    if (recipient.isNotEmpty()) break
                }
            }
        }

        if (recipient.isEmpty()) {
            val upiPattern = Pattern.compile("(?i)([a-zA-Z0-9.]+@[a-zA-Z0-9]+)")
            val upiMatcher = upiPattern.matcher(message)
            if (upiMatcher.find()) recipient = upiMatcher.group(1) ?: ""
        }

        if (recipient.isNotEmpty()) {
            recipient = recipient.replace(Regex("(\\s+(?:on|at|via|using|through|for)\\s+.*$)"), "")
                .replace(Regex("[.,;:]$"), "")
                .trim()
        }

        if (recipient.isEmpty() && message.contains("SBI")) {
            val sbiPattern = Pattern.compile("\\sto\\s+([A-Z][A-Za-z]+)")
            val sbiMatcher = sbiPattern.matcher(message)
            if (sbiMatcher.find()) recipient = sbiMatcher.group(1) ?: ""
        }

        return Triple(amount, isCredit, recipient)
    }

    private fun containsBankName(message: String): Boolean {
        val messageLower = message.lowercase()
        val bankKeywords = mapOf(
            "sbi" to listOf("sbi", "sbipsg", "sbitxn", "sbinbh"),
            "hdfc" to listOf("hdfc", "hdfcbk", "hdfcbn", "hdfctr"),
            "icici" to listOf("icici", "icicib", "icicin", "icictn"),
            "axis" to listOf("axis", "axisbk", "axistn"),
            "pnb" to listOf("pnb", "pnbmsg", "pnbtxn"),
            "bob" to listOf("bob", "bobtxn", "baroda", "bankbd"),
            "kotak" to listOf("kotak", "ktkbnk", "kotknb"),
            "indusind" to listOf("indusind", "indbnk", "indsbn"),
            "yes" to listOf("yesbank", "yesbnk", "yestxn"),
            "union" to listOf("union", "ubin", "unbktx")
        )
        return bankKeywords.values.flatten().any { keyword -> messageLower.contains(keyword) }
    }


    suspend fun insertCashTransaction(userCash: UserCash) {
        usersDao.insertUserCash(userCash)
    }

// Get all cash transactions
    suspend fun getCashTransactions(): List<UserCash> {
        return usersDao.getAllCashTransactions()
    }
}
