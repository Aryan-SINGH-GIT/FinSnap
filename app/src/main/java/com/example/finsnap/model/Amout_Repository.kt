package com.example.finsnap.model



import android.content.Context
import android.provider.Telephony
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.example.finsnap.viewmodel.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import android.net.Uri

class Amout_Repository(private val context: Context) {

    private val database = DatabaseManager.getDatabase(context.applicationContext)
    private val usersDao = database.UsersDao()

    // Add to Amout_Repository.kt
    suspend fun getUserExists(email: String): Boolean {
        return usersDao.getUserByEmail(email) != null
    }
//
    // ✅ Get all transactions from Room (LiveData)
    fun getAllUserAmountsLive(): LiveData<List<UserAmount>> {
        return usersDao.getAllUserAmounts()
    }
//
    // ✅ Update a transaction in Room
    suspend fun updateUserAmount(userAmount: UserAmount) {
        usersDao.updateUserAmount(userAmount)
    }

     //✅ Use this when you want a list without observing
    suspend fun getBankSmsTransactions(): List<UserAmount> {
        val dbData = usersDao.getAllUserAmountsNow()
        
        // Track this fetch operation
        val currentTime = System.currentTimeMillis()
        
        // Get SMS based on fetch history
        val parsed = parseSmsTransactions(dbData)
        
        // Create a more unique key using message hash + amount + timestamp
        val existingKeys = dbData.map { generateUniqueKey(it) }.toSet()
        
        // Filter out transactions that already exist in the database
        val newOnes = parsed.filter { generateUniqueKey(it) !in existingKeys }
        
        // Insert new transactions
        newOnes.forEach { usersDao.insertUserAmount(it) }
        
        // Save the current time as last fetch time
        com.example.finsnap.viewmodel.SessionManager.saveLastSmsFetchTime(currentTime)
        
        return usersDao.getAllUserAmountsNow()
    }

    // Force refresh: re-parse all SMS and update DB (optionally clear DB first)
    suspend fun refreshAllSmsTransactions(): List<UserAmount> = withContext(Dispatchers.IO) {
        // Optionally: usersDao.clearAllUserAmounts() // if you want to clear old
        val dbData = usersDao.getAllUserAmountsNow()
        val parsed = parseSmsTransactions(dbData)
        
        // Create a more unique key using message hash + amount + timestamp
        val existingKeys = dbData.map { generateUniqueKey(it) }.toSet()
        
        // Filter out transactions that already exist in the database
        val newOnes = parsed.filter { generateUniqueKey(it) !in existingKeys }
        
        // Insert new transactions
        newOnes.forEach { usersDao.insertUserAmount(it) }
        
        // Save the current time as last fetch time
        com.example.finsnap.viewmodel.SessionManager.saveLastSmsFetchTime(System.currentTimeMillis())
        
        return@withContext usersDao.getAllUserAmountsNow()
    }
    
    // Generate a unique key for transactions
    private fun generateUniqueKey(transaction: UserAmount): String {
        // Create a more robust unique identifier by combining:
        // 1. A hash of the message content (to detect same message)
        // 2. The transaction amount (to distinguish different amounts)
        // 3. The sender name (additional differentiator)
        return "${transaction.rawMessage.hashCode()}_${transaction.amount}_${transaction.sender}"
    }

    // Fetch SMS based on timestamp range
    suspend fun parseSmsTransactions(existingTransactions: List<UserAmount> = emptyList()): List<UserAmount> = withContext(Dispatchers.IO){
        val smsList = mutableListOf<UserAmount>()
        val seenTransactions = mutableSetOf<String>() // To track unique transactions
        val seenTimestamps = mutableMapOf<String, Int>() // To track and make timestamps unique
        
        // Create a set of existing transaction keys for faster lookup
        val existingKeys = existingTransactions.map { generateUniqueKey(it) }.toSet()
        
        // Extract base timestamps from existing transactions to avoid duplication
        existingTransactions.forEach { transaction ->
            val baseTime = if (transaction.time.contains("-")) {
                transaction.time.substringBefore("-")
            } else if (transaction.time.contains("(")) {
                transaction.time.substringBefore(" (")
            } else {
                transaction.time
            }
            // Mark this timestamp as seen
            val baseTimestampKey = baseTime.substringBeforeLast(" ")  // Remove AM/PM part
            if (baseTimestampKey !in seenTimestamps) {
                seenTimestamps[baseTimestampKey] = 0
            } else {
                seenTimestamps[baseTimestampKey] = seenTimestamps[baseTimestampKey]!! + 1
            }
        }
        
        val calendar = Calendar.getInstance()
        
        // Determine fetch range based on first-time status
        val lastFetchTime = com.example.finsnap.viewmodel.SessionManager.getLastSmsFetchTime()
        val isFirstFetch = !com.example.finsnap.viewmodel.SessionManager.hasCompletedFirstFetch()
        
        // Set appropriate fetch window
        if (isFirstFetch) {
            // First fetch: Get SMS from the last 3 months
            calendar.add(Calendar.MONTH, -3)
            android.util.Log.d("Amout_Repository", "First SMS fetch: Getting last 3 months of SMS")
        } else {
            // Subsequent fetches: Get SMS since last fetch
            calendar.timeInMillis = lastFetchTime
            android.util.Log.d("Amout_Repository", "Subsequent fetch: Getting SMS since last fetch")
        }
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        try {
            // Get SMS messages
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("body", "date"),
                "date >= ? AND date <= ?",
                arrayOf(startTime.toString(), endTime.toString()),
                "date DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val messageBody = it.getString(it.getColumnIndexOrThrow("body"))
                    val messageDate = it.getLong(it.getColumnIndexOrThrow("date"))
                    
                    if (containsBankName(messageBody)) {
                        val date = Date(messageDate)
                        val baseTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(date)
                        val baseTimestampKey = baseTime.substringBeforeLast(" ")
                        
                        // Get counter for this timestamp
                        var counter = seenTimestamps[baseTimestampKey] ?: 0
                        seenTimestamps[baseTimestampKey] = counter + 1
                        
                        // Create truly unique timestamp
                        val uniqueFormattedTime = if (counter > 0) {
                            "$baseTime ($counter)"
                        } else {
                            baseTime
                        }
                        
                        val (amount, isCredit, parsedSender) = extractTransactionInfo(messageBody)
                        val imageResource = R.drawable.ic_miscellaneous
                        val amountFormatted = if (isCredit) "+₹$amount" else "-₹$amount"
                        val displaySender = if (parsedSender.isNotEmpty()) parsedSender else if (isCredit) "Unknown Sender" else "Unknown Recipient"
                        val numericAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0
                        
                        // Create a unique key for this transaction
                        val transactionKey = generateUniqueKey(UserAmount(
                            sender = displaySender,
                            time = uniqueFormattedTime,
                            amtChange = amountFormatted,
                            amtImage = imageResource,
                            rawMessage = messageBody,
                            amount = numericAmount,
                            isCredit = isCredit
                        ))
                        
                        // Only add if we haven't seen this transaction before and it's not in existing transactions
                        if (transactionKey !in seenTransactions && transactionKey !in existingKeys) {
                            seenTransactions.add(transactionKey)
                            
                            smsList.add(
                                UserAmount(
                                    sender = displaySender,
                                    time = uniqueFormattedTime,
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
            }
            
            android.util.Log.d("Amout_Repository", "Found ${smsList.size} new bank transactions")
        } catch (e: Exception) {
            android.util.Log.e("Amout_Repository", "Error parsing SMS: ${e.message}")
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
            "sbi" to listOf("sbi", "sbipsg", "sbitxn", "sbinbh","State Bank of India"),
            "hdfc" to listOf("hdfc", "hdfcbk", "hdfcbn", "hdfctr"),
            "icici" to listOf("icici", "icicib", "icicin", "icictn"),
            "axis" to listOf("axis", "axisbk", "axistn"),
            "pnb" to listOf("pnb", "pnbmsg", "pnbtxn"),
            "bob" to listOf("bob" ,"bobtxn", "baroda", "bankbd","Bank of Baroda"),
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
