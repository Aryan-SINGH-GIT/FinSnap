package com.example.finsnap.model

import android.content.Context
import android.provider.Telephony
import androidx.core.database.getStringOrNull
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Amout_Repository(private val context: Context) {

    suspend fun getBankSmsTransactions(): List<UserAmount> {
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
                                amount= numericAmount,  // Add the numeric amount
                                isCredit = isCredit      // Add whether it's a credit or debit
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return smsList
    }

    private fun extractTransactionInfo(message: String): Triple<String, Boolean, String> {
        // Default values
        var amount = ""
        var isCredit = false
        var recipient = ""

        // Check for common amount patterns (Rs, INR, ₹)
        val amountPattern = Pattern.compile("(?i)(?:Rs\\.?|INR|₹|by|for)\\s*(\\d+(?:[.,]\\d+)?)")
        val amountMatcher = amountPattern.matcher(message)

        if (amountMatcher.find()) {
            amount = amountMatcher.group(1) ?: ""
        }


        // Determine if credited or debited
        val messageLower = message.lowercase()
        isCredit = messageLower.contains("credited") ||
                messageLower.contains("received") ||
                messageLower.contains("added") ||
                messageLower.contains("deposited")

        // Handle specific SBI format:
        // "A/C X0381 debited by 23.0 on date 16Apr25 trf to HungerBox Refno 510645641369"
        if (messageLower.contains("trf to ")) {
            val trfPattern = Pattern.compile("(?i)trf\\s+to\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:Refno|Ref|Reference|\\.|$))")
            val trfMatcher = trfPattern.matcher(message)
            if (trfMatcher.find()) {
                recipient = trfMatcher.group(1)?.trim() ?: ""
            }
        }

        // If not found via "trf to", try other patterns
        if (recipient.isEmpty()) {
            if (isCredit) {
                // Look for patterns like "credited by [name]", "received from [name]"
                val creditPatterns = listOf(
                    Pattern.compile("(?i)credited\\s+(?:by|from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))"),
                    Pattern.compile("(?i)received\\s+(?:from)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))"),
                    Pattern.compile("(?i)from\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:to|in|on|\\.|$))")
                )

                for (pattern in creditPatterns) {
                    val matcher = pattern.matcher(message)
                    if (matcher.find()) {
                        recipient = matcher.group(1)?.trim() ?: ""
                        if (recipient.isNotEmpty()) break
                    }
                }
            } else {
                // Look for patterns like "debited to [name]", "paid to [name]"
                val debitPatterns = listOf(
                    Pattern.compile("(?i)debited\\s+(?:to|for)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))"),
                    Pattern.compile("(?i)paid\\s+(?:to)\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))"),
                    Pattern.compile("(?i)to\\s+([A-Za-z0-9\\s]+?)(?:\\s+(?:from|in|on|\\.|$))")
                )

                for (pattern in debitPatterns) {
                    val matcher = pattern.matcher(message)
                    if (matcher.find()) {
                        recipient = matcher.group(1)?.trim() ?: ""
                        if (recipient.isNotEmpty()) break
                    }
                }
            }
        }

        // Handle case when message doesn't have clear recipient pattern
        if (recipient.isEmpty()) {
            // Look for UPI IDs which often contain the recipient info
            val upiPattern = Pattern.compile("(?i)([a-zA-Z0-9.]+@[a-zA-Z0-9]+)")
            val upiMatcher = upiPattern.matcher(message)
            if (upiMatcher.find()) {
                recipient = upiMatcher.group(1) ?: ""
            }
        }

        // Clean up the extracted recipient name
        if (recipient.isNotEmpty()) {
            recipient = recipient.replace(Regex("(\\s+(?:on|at|via|using|through|for)\\s+.*$)"), "")
                .replace(Regex("[.,;:]$"), "")
                .trim()
        }

        // For the SBI specific pattern, if still no recipient, try more aggressive extraction
        if (recipient.isEmpty() && message.contains("SBI")) {
            // Extract any word after "to" that looks like a name (capital letter followed by letters)
            val sbiPattern = Pattern.compile("\\sto\\s+([A-Z][A-Za-z]+)")
            val sbiMatcher = sbiPattern.matcher(message)
            if (sbiMatcher.find()) {
                recipient = sbiMatcher.group(1) ?: ""
            }
        }
        val numericAmount = amount.replace(",", "").toDoubleOrNull() ?: 0.0

        return Triple(amount, isCredit, recipient)
    }


    private lateinit var database: UserDatabase


    private fun containsBankName(message: String): Boolean {
        val messageLower = message.lowercase()

        // Map of bank names to their common SMS keywords and sender IDs
        val bankKeywords = mapOf(
            "state bank of india" to listOf("sbi", "sbipsg", "sbitxn", "sbinbh"),
            "hdfc bank" to listOf("hdfc", "hdfcbk", "hdfcbn", "hdfctr"),
            "icici bank" to listOf("icici", "icicib", "icicin", "icictn"),
            "axis bank" to listOf("axis", "axisbk", "axistn"),
            "punjab national bank" to listOf("pnb", "pnbmsg", "pnbtxn"),
            "bank of baroda" to listOf("bob", "bobtxn", "baroda", "bankbd"),
            "kotak mahindra bank" to listOf("kotak", "ktkbnk", "kotknb"),
            "indusind bank" to listOf("indusind", "indbnk", "indsbn"),
            "yes bank" to listOf("yesbank", "yesbnk", "yestxn"),
            "union bank of india" to listOf("union", "ubin", "unbktx")
        )

        // Check if any bank keyword is present in the message
        for (keywords in bankKeywords.values) {
            if (keywords.any { messageLower.contains(it.lowercase()) }) {
                return true
            }
        }

        return false
    }

// Then update getBankSmsTransactions to call this suspended function properly
}
