package com.example.finsnap.utils

import android.content.Context
import android.graphics.Color
import com.example.finsnap.model.UserAmount
import java.text.SimpleDateFormat
import java.util.*

object TransactionUtils {
    fun formatAmount(amount: Double, isCredit: Boolean): String {
        return if (isCredit) "+₹$amount" else "-₹$amount"
    }

    fun getTransactionColor(isCredit: Boolean): Int {
        return if (isCredit) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
    }

    fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a", Locale.getDefault()).format(date)
    }

    fun calculateBalance(transactions: List<UserAmount>, initialBalance: Double): Double {
        return transactions.fold(initialBalance) { balance, transaction ->
            if (transaction.isCredit) balance + transaction.amount else balance - transaction.amount
        }
    }

    // Date format parsers that handle both old and new formats
    private val DATE_FORMATS = listOf(
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss.S", Locale.getDefault()),  // With counter
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),    // New base format
        SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a", Locale.getDefault()), // Old format with milliseconds
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())      // Legacy format
    )
    
    // Parse a transaction date string safely
    fun parseTransactionDate(dateString: String): Long {
        // Try to extract base date part if it contains our unique suffixes
        val baseDateString = when {
            dateString.contains(" (") -> dateString.substringBefore(" (") // Format with counter display
            dateString.contains("-") -> dateString.substringBefore("-")   // Format with hidden suffix
            else -> dateString  // Original format
        }
        
        // Try parsing with each format
        for (format in DATE_FORMATS) {
            try {
                return format.parse(baseDateString)?.time ?: 0L
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        // Fallback to current time if all parsing fails
        return System.currentTimeMillis()
    }

    fun filterTransactionsByDate(transactions: List<UserAmount>, date: Date): List<UserAmount> {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return transactions.filter { transaction ->
            val transactionDate = parseTransactionDate(transaction.time)
            transactionDate in startOfDay until endOfDay
        }
    }
} 