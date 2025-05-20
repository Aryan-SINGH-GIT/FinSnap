package com.example.finsnap.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.Transaction
import com.example.finsnap.model.Amout_Repository
import com.example.finsnap.model.UserAmount
import com.example.finsnap.model.UserBank
import com.example.finsnap.model.UserCash
import com.example.finsnap.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.finsnap.utils.TransactionUtils
import java.util.Calendar

import com.example.finsnap.viewmodel.DatabaseManager

// Change to AndroidViewModel to access application context
class FinanceViewModel(application: Application): AndroidViewModel(application) {

    // Initialize repository with application context
    private val repository = Amout_Repository(application)

    // Get a reference to the UserDatabase
    private val database:UserDatabase = DatabaseManager.getDatabase(application.applicationContext)

    private val _smsTransactions = MutableLiveData<List<UserAmount>>()
    val smsTransactions: LiveData<List<UserAmount>> = _smsTransactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentBalance = MutableLiveData<Double>()
    val currentBalance: LiveData<Double> = _currentBalance

    private val _cashTransactions = MutableLiveData<List<UserCash>>()
    val cashTransactions: LiveData<List<UserCash>> = _cashTransactions

    private val _cashBalance = MutableLiveData<Double>()
    val cashBalance: LiveData<Double> = _cashBalance

    // Event to notify a specific item was updated
    private val _updatedTransaction = MutableLiveData<UserAmount>()
    val updatedTransaction: LiveData<UserAmount> = _updatedTransaction

    private val _savingsTarget = MutableLiveData<Double>()
    val savingsTarget: LiveData<Double> = _savingsTarget

    // Shared filter state
    private val _filteredTransactions = MutableLiveData<List<UserAmount>>()
    val filteredTransactions: LiveData<List<UserAmount>> = _filteredTransactions

    private var currentFilterPosition = 0
    private var currentFilterDate: Date? = null

    private val _filteredCashTransactions = MutableLiveData<List<UserCash>>()
    val filteredCashTransactions: LiveData<List<UserCash>> = _filteredCashTransactions

    // Add flag to track if balances have been calculated
    private var balancesCalculated = false
    
    // Add timestamp for last cash update to prevent frequent recalculations
    private var lastCashUpdateTimestamp = 0L
    private val CASH_UPDATE_THRESHOLD_MS = 5000 // 5 seconds between updates

    // Initialize data when ViewModel is created
    init {
        // Make sure we have data available soon after app starts
        viewModelScope.launch {
            fetchBankSmsAndSaveIfNeeded()
            loadUserBankDetails()
            loadInitialCashBalance() // Add initial cash balance loading
        }
    }

    fun InsertBankDetail(bankName: String, currentAmount: Double,cashAmount:Double,savingTarget:Double): UserBank {
        val userId = SessionManager.getUserToken()

        _cashBalance.postValue(cashAmount)

        return UserBank(
            bankName = bankName,
            currentAmount = currentAmount,
            userId = userId.toString(),
            cashAmount = cashAmount.toString(),
            savingTarget = savingTarget.toString()
        )
    }

    fun loadUserBankDetails() {
        viewModelScope.launch {
            try {
                // Skip if balances already calculated
                if (balancesCalculated) {
                    android.util.Log.d("FinanceViewModel", "Skipping loadUserBankDetails, balances already calculated")
                    return@launch
                }
                
                val userId = SessionManager.getUserToken().toString()
                withContext(Dispatchers.IO) {
                    val userBank = database.UsersDao().getUserBankByUserId(userId)
                    userBank?.let {
                        withContext(Dispatchers.Main) {
                            SessionManager.saveBankSetupTime(System.currentTimeMillis()) // fallback if missing
                            if (SessionManager.getBankSetupTime() == 0L) {
                                // Estimate setup time as now if not previously stored
                                SessionManager.saveBankSetupTime(System.currentTimeMillis())
                            }

                            _currentBalance.value = it.currentAmount
                            _cashBalance.value = it.cashAmount.toDoubleOrNull() ?: 0.0
                            _savingsTarget.value = it.savingTarget.toDoubleOrNull() ?: 0.0
                            balancesCalculated = true
                            android.util.Log.d("FinanceViewModel", "Bank details loaded, balances calculated")
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load bank details: ${e.message}"
            }
        }
    }

    fun loadSmsTransactions() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                android.util.Log.d("FinanceViewModel", "Loading SMS transactions")
                val transactions = withContext(Dispatchers.IO) {
                    // This will fetch SMS based on progressive loading strategy
                    repository.getBankSmsTransactions()
                }
                // Update both transaction lists
                _smsTransactions.value = transactions
                resetBalanceFlag() // Reset flag when new transactions are loaded
                applyFilters() // Apply current filters to new data
                _isLoading.value = false
                android.util.Log.d("FinanceViewModel", "Loaded ${transactions.size} transactions")
            } catch (e: Exception) {
                _error.value = "Failed to load SMS transactions: ${e.message}"
                _isLoading.value = false
                android.util.Log.e("FinanceViewModel", "Error loading transactions", e)
            }
        }
    }

    fun InsertUserData(email: String, password: String): UserData {
        return UserData(
            email = email,
            userpassword = password
        )
    }

    fun calculateCurrentBalance(initialBalance: Double) {
        viewModelScope.launch {
            try {
                val setupTime = SessionManager.getBankSetupTime()
                android.util.Log.d("FinanceViewModel", "Using setup time: $setupTime")
                
                val transactions = withContext(Dispatchers.IO) {
                    repository.getBankSmsTransactions()
                }
                
                android.util.Log.d("FinanceViewModel", "Found ${transactions.size} transactions")
                
                // Filter transactions after setup time and sort by time in descending order
                val filteredTransactions = transactions
                    .filter { txn ->
                        try {
                            val txnTime = TransactionUtils.parseTransactionDate(txn.time)
                            val isAfterSetup = txnTime >= setupTime
                            android.util.Log.d("FinanceViewModel", "Transaction time: ${txn.time} parsed to $txnTime, after setup: $isAfterSetup")
                            isAfterSetup
                        } catch (e: Exception) {
                            android.util.Log.e("FinanceViewModel", "Error parsing date: ${txn.time}", e)
                            false
                        }
                    }
                    .sortedByDescending { txn ->
                        TransactionUtils.parseTransactionDate(txn.time)
                    }

                android.util.Log.d("FinanceViewModel", "Filtered to ${filteredTransactions.size} transactions after setup time")
                
                // Calculate running balance
                var runningBalance = initialBalance
                filteredTransactions.forEach { txn ->
                    if (txn.isCredit) {
                        runningBalance += txn.amount
                        android.util.Log.d("FinanceViewModel", "Adding ${txn.amount} to balance, new balance: $runningBalance")
                    } else {
                        runningBalance -= txn.amount
                        android.util.Log.d("FinanceViewModel", "Subtracting ${txn.amount} from balance, new balance: $runningBalance")
                    }
                }

                // Update balance on main thread
                withContext(Dispatchers.Main) {
                    _currentBalance.value = runningBalance
                    android.util.Log.d("FinanceViewModel", "Final balance set to $runningBalance")
                    balancesCalculated = true
                }
            } catch (e: Exception) {
                _error.value = "Failed to calculate balance: ${e.message}"
                android.util.Log.e("FinanceViewModel", "Error calculating balance", e)
            }
        }
    }

    // Update date filtering functions to sort in descending order
    fun getTransactionsForDate(date: Date): List<UserAmount> {
        val setupTime = SessionManager.getBankSetupTime()
        val transactions = _smsTransactions.value ?: emptyList()
        
        return transactions
            .filter { txn ->
                try {
                    val txnTime = TransactionUtils.parseTransactionDate(txn.time)
                    txnTime >= setupTime
                } catch (e: Exception) {
                    false
                }
            }
            .filter { txn ->
                try {
                    val txnDate = TransactionUtils.parseTransactionDate(txn.time)
                    val targetDate = date.time
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = targetDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfDay = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val endOfDay = calendar.timeInMillis
                    txnDate in startOfDay until endOfDay
                } catch (e: Exception) {
                    false
                }
            }
            .sortedByDescending { txn ->
                TransactionUtils.parseTransactionDate(txn.time)
            }
    }

    fun getTransactionsForDateRange(startDate: Date, endDate: Date): List<UserAmount> {
        val setupTime = SessionManager.getBankSetupTime()
        val transactions = _smsTransactions.value ?: emptyList()
        
        return transactions
            .filter { txn ->
                try {
                    val txnTime = TransactionUtils.parseTransactionDate(txn.time)
                    txnTime >= setupTime
                } catch (e: Exception) {
                    false
                }
            }
            .filter { txn ->
                try {
                    val txnDate = TransactionUtils.parseTransactionDate(txn.time)
                    txnDate in startDate.time..endDate.time
                } catch (e: Exception) {
                    false
                }
            }
            .sortedByDescending { txn ->
                TransactionUtils.parseTransactionDate(txn.time)
            }
    }

    fun updateCashBalanceFromTransactions(transactions: List<UserCash>) {
        // Skip if we've updated recently
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCashUpdateTimestamp < CASH_UPDATE_THRESHOLD_MS && balancesCalculated) {
            android.util.Log.d("FinanceViewModel", "Skipping cash balance update, too soon")
            return
        }
        
        viewModelScope.launch {
            try {
                val userId = SessionManager.getUserToken()

                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        _error.value = "User not logged in"
                    }
                    return@launch
                }

                val userBank = database.UsersDao().getUserBankByUserId(userId.toString())

                if (userBank == null) {
                    withContext(Dispatchers.Main) {
                        _cashBalance.value = 0.0
                        _error.value = "No bank record found for the user"
                    }
                    return@launch
                }

                // Calculate balance directly from transactions only
                val transactionSum = transactions.sumOf { cash ->
                    val amount = cash.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0
                    if (cash.CashamtChange.startsWith("+")) amount else -amount
                }

                // Use stored initial amount instead of adding to current amount
                val storedInitialAmount = userBank.cashAmount?.toDoubleOrNull() ?: 0.0

                // Set the balance to initial amount + sum of all transactions
                val newBalance = storedInitialAmount + transactionSum

                // Check if the balance actually changed before updating DB
                val currentCashBalance = _cashBalance.value ?: 0.0
                if (Math.abs(currentCashBalance - newBalance) > 0.01) {
                    val updatedBank = userBank.copy(cashAmount = newBalance.toString())
                    database.UsersDao().updateUserBank(updatedBank)

                    withContext(Dispatchers.Main) {
                        // Just update the view with the calculated balance
                        _cashBalance.value = newBalance
                        balancesCalculated = true
                        lastCashUpdateTimestamp = System.currentTimeMillis()
                        android.util.Log.d("FinanceViewModel", "Cash balance updated to $newBalance")
                    }
                } else {
                    android.util.Log.d("FinanceViewModel", "Cash balance unchanged, skipping update")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Error updating cash balance: ${e.localizedMessage}"
                }
            }
        }
    }

    fun loadCashTransactions() {
        // Check if we've updated recently to prevent unnecessary loads
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCashUpdateTimestamp < CASH_UPDATE_THRESHOLD_MS && balancesCalculated) {
            android.util.Log.d("FinanceViewModel", "Skipping cash load, last update was ${currentTime - lastCashUpdateTimestamp}ms ago")
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val transactions = withContext(Dispatchers.IO) {
                    repository.getCashTransactions()
                }
                // Sort transactions by date in descending order
                val sortedTransactions = transactions.sortedByDescending { cash ->
                    try {
                        TransactionUtils.parseTransactionDate(cash.cashTime)
                    } catch (e: Exception) {
                        0L
                    }
                }
                _cashTransactions.value = sortedTransactions
                _filteredCashTransactions.value = sortedTransactions
                
                // We don't call updateCashBalanceFromTransactions here anymore
                // Balance is only updated when a new transaction is added
                // through updateCashBalanceWithNewTransaction
                
                // Just update timestamp to prevent frequent reloading
                lastCashUpdateTimestamp = System.currentTimeMillis()

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load cash transactions: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun insertCashTransaction(userCash: UserCash) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                withContext(Dispatchers.IO) {
                    repository.insertCashTransaction(userCash)
                }
                resetBalanceFlag() // Reset flag when new transaction is added
                
                // Load the updated transactions list
                val updatedTransactions = withContext(Dispatchers.IO) {
                    repository.getCashTransactions()
                }
                _cashTransactions.value = updatedTransactions
                _filteredCashTransactions.value = updatedTransactions

                // After inserting a new transaction, explicitly update the cash balance
                updateCashBalanceWithNewTransaction(userCash)
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to save transaction: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Function specifically for updating the balance when a new transaction is added
    private fun updateCashBalanceWithNewTransaction(newTransaction: UserCash) {
        viewModelScope.launch {
            try {
                val userId = SessionManager.getUserToken()
                if (userId == -1) return@launch
                
                val userBank = withContext(Dispatchers.IO) {
                    database.UsersDao().getUserBankByUserId(userId.toString())
                } ?: return@launch
                
                // Calculate new amount from the transaction
                val amount = newTransaction.CashamtChange.substring(1).toDoubleOrNull() ?: 0.0
                val changeAmount = if (newTransaction.CashamtChange.startsWith("+")) amount else -amount
                
                // Get current stored balance
                val storedBalance = userBank.cashAmount.toDoubleOrNull() ?: 0.0
                val newBalance = storedBalance + changeAmount
                
                // Update in database
                val updatedBank = userBank.copy(cashAmount = newBalance.toString())
                withContext(Dispatchers.IO) {
                    database.UsersDao().updateUserBank(updatedBank)
                }
                
                // Update in memory
                _cashBalance.value = newBalance
                android.util.Log.d("FinanceViewModel", "Cash balance updated to $newBalance after new transaction")
                
                // Mark as calculated
                balancesCalculated = true
                lastCashUpdateTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error updating cash balance after new transaction", e)
            }
        }
    }

    // Update transaction sender name and notify via event
    fun updateTransactionSender(updatedTransaction: UserAmount) {
        // Add debugging logs
        android.util.Log.d("FinanceViewModel", "Updating transaction sender: ${updatedTransaction.sender}")
        android.util.Log.d("FinanceViewModel", "Raw message: ${updatedTransaction.rawMessage}")
        android.util.Log.d("FinanceViewModel", "Time: ${updatedTransaction.time}")
        android.util.Log.d("FinanceViewModel", "Amount: ${updatedTransaction.amount}")
        android.util.Log.d("FinanceViewModel", "Category image: ${updatedTransaction.categoryImage}")

        // Update the transaction in the current list
        val currentList = _smsTransactions.value?.toMutableList() ?: mutableListOf()

        // Log current list size
        android.util.Log.d("FinanceViewModel", "Current list size: ${currentList.size}")

        // Generate a unique key for the updated transaction
        val updatedKey = generateUniqueKey(updatedTransaction)
        
        // Find the transaction with the same unique key or ID
        var index = -1
        
        // First try by ID if available
        if (updatedTransaction.id > 0) {
            index = currentList.indexOfFirst { it.id == updatedTransaction.id }
            android.util.Log.d("FinanceViewModel", "Found by ID at index: $index")
        }
        
        // If not found by ID, try by unique key
        if (index == -1) {
            index = currentList.indexOfFirst { 
                generateUniqueKey(it) == updatedKey 
            }
            android.util.Log.d("FinanceViewModel", "Found by unique key at index: $index")
        }
        
        // Last resort, try by raw message
        if (index == -1) {
            index = currentList.indexOfFirst {
                it.rawMessage == updatedTransaction.rawMessage
            }
            android.util.Log.d("FinanceViewModel", "Found by raw message at index: $index")
        }

        android.util.Log.d("FinanceViewModel", "Final index found: $index")

        if (index != -1) {
            // Log before change
            android.util.Log.d("FinanceViewModel", "Old sender: ${currentList[index].sender}")
            android.util.Log.d("FinanceViewModel", "Old category image: ${currentList[index].categoryImage}")

            // Replace the transaction with the updated one
            currentList[index] = updatedTransaction

            // Log after change
            android.util.Log.d("FinanceViewModel", "New sender in list: ${currentList[index].sender}")
            android.util.Log.d("FinanceViewModel", "New category image: ${currentList[index].categoryImage}")

            // Important: Set the value to a NEW list to trigger the observer
            _smsTransactions.value = ArrayList(currentList)
            
            // Also update the filtered list to ensure UI consistency
            val filteredList = _filteredTransactions.value?.toMutableList() ?: mutableListOf()
            val filteredIndex = filteredList.indexOfFirst { 
                it.id == updatedTransaction.id || generateUniqueKey(it) == updatedKey 
            }
            if (filteredIndex != -1) {
                filteredList[filteredIndex] = updatedTransaction
                _filteredTransactions.value = ArrayList(filteredList)
            }

            // Emit event for the specific update - force a new object to trigger observers
            _updatedTransaction.postValue(updatedTransaction.copy())

            android.util.Log.d("FinanceViewModel", "Posted updated transaction event")
        } else {
            android.util.Log.e("FinanceViewModel", "Could not find transaction to update!")
        }
    }
    
    // Helper function to generate a unique key for transactions (matching the Repository impl)
    private fun generateUniqueKey(transaction: UserAmount): String {
        return "${transaction.rawMessage.hashCode()}_${transaction.amount}_${transaction.sender}"
    }

    fun loadSmsTransactionsFromDb(): LiveData<List<UserAmount>> {
        viewModelScope.launch {
            try {
                // First get the transactions from the database
                val transactions = withContext(Dispatchers.IO) {
                    repository.getAllUserAmountsLive().value ?: emptyList()
                }
                
                // Update the main transactions list
                _smsTransactions.value = transactions
                
                // If we have transactions, apply filters
                if (transactions.isNotEmpty()) {
                    applyFilters()
                } else {
                    // If no transactions, try loading from SMS
                    loadSmsTransactions()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load transactions: ${e.message}"
            }
        }
        return filteredTransactions
    }

    fun getAllUserAmountsLive(): LiveData<List<UserAmount>> {
        return repository.getAllUserAmountsLive()
    }

    fun fetchBankSmsAndSaveIfNeeded() {
        viewModelScope.launch {
            try {
                android.util.Log.d("FinanceViewModel", "Fetching bank SMS if needed")
                
                // Check if we have any transactions already
                val storedTransactions = withContext(Dispatchers.IO) {
                    repository.getAllUserAmountsLive().value ?: listOf()
                }

                if (storedTransactions.isEmpty()) {
                    android.util.Log.d("FinanceViewModel", "No stored transactions, processing SMS")
                    // If no stored transactions, process SMS
                    withContext(Dispatchers.IO) {
                        repository.getBankSmsTransactions()
                    }
                } else {
                    // We have transactions, but let's check if we need to fetch new ones
                    val lastFetchTime = SessionManager.getLastSmsFetchTime()
                    val timeSinceLastFetch = System.currentTimeMillis() - lastFetchTime
                    
                    // Only refresh if it's been more than 1 hour since the last fetch
                    if (timeSinceLastFetch > 60 * 60 * 1000) {
                        android.util.Log.d("FinanceViewModel", "Fetching new SMS since last fetch")
                        // This will only fetch SMS since the last fetch time
                        withContext(Dispatchers.IO) {
                            repository.getBankSmsTransactions()
                        }
                    } else {
                        android.util.Log.d("FinanceViewModel", "Using existing transactions (fetch was recent)")
                    }
                    
                    // Filter and sort existing transactions
                    val filteredAndSortedList = storedTransactions
                        .filter { txn ->
                            try {
                                // Support both old and new date formats
                                val formats = listOf(
                                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()),
                                    SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a", Locale.getDefault())
                                )
                                
                                // Try parsing with each format until one succeeds
                                var txnDate: Long? = null
                                for (format in formats) {
                                    try {
                                        txnDate = format.parse(txn.time)?.time
                                        if (txnDate != null) break
                                    } catch (e: Exception) {
                                        // Try next format
                                    }
                                }
                                
                                // Only show transactions from the last week
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                                val oneWeekAgo = calendar.time
                                txnDate != null && txnDate >= oneWeekAgo.time
                            } catch (e: Exception) {
                                false
                            }
                        }
                        .sortedByDescending { txn ->
                            try {
                                // Support both old and new date formats
                                val formats = listOf(
                                    SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()),
                                    SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a", Locale.getDefault())
                                )
                                
                                // Try parsing with each format until one succeeds
                                var txnDate: Long? = null
                                for (format in formats) {
                                    try {
                                        txnDate = format.parse(txn.time)?.time
                                        if (txnDate != null) break
                                    } catch (e: Exception) {
                                        // Try next format
                                    }
                                }
                                
                                txnDate ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    
                    _smsTransactions.postValue(filteredAndSortedList)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to fetch SMS data: ${e.message}")
                android.util.Log.e("FinanceViewModel", "Error fetching bank SMS", e)
            }
        }
    }

    fun updateTransactionInDb(userAmount: UserAmount) {
        viewModelScope.launch {
            try {
                // Log the transaction we're trying to update
                android.util.Log.d("FinanceViewModel", "Trying to update transaction: ${userAmount.id}")
                android.util.Log.d("FinanceViewModel", "Transaction details - Sender: ${userAmount.sender}, Time: ${userAmount.time}")
                
                // First update in database
                withContext(Dispatchers.IO) {
                    // Generate unique key for the transaction
                    val transactionKey = generateUniqueKey(userAmount)
                    
                    // Find existing transaction by ID or unique key
                    val existingTransaction = if (userAmount.id > 0) {
                        // If we have an ID, use it directly
                        database.UsersDao().getUserAmountById(userAmount.id)
                    } else {
                        // If ID is 0 (default value), try to find matching transaction by unique key
                        val allTransactions = database.UsersDao().getAllUserAmountsNow()
                        allTransactions.find { 
                            generateUniqueKey(it) == transactionKey 
                        }
                    }

                    if (existingTransaction != null) {
                        // We found the existing transaction, update it with the new values but keep the same ID
                        val updatedTransaction = userAmount.copy(id = existingTransaction.id)
                        android.util.Log.d("FinanceViewModel", "Found existing transaction with ID: ${existingTransaction.id}")
                        repository.updateUserAmount(updatedTransaction)
                    } else {
                        // Check if a transaction with the same unique key exists
                        val duplicateCheck = database.UsersDao().getAllUserAmountsNow()
                            .find { generateUniqueKey(it) == transactionKey }
                        
                        if (duplicateCheck != null) {
                            // Update the duplicate instead of creating a new one
                            android.util.Log.d("FinanceViewModel", "Found duplicate transaction with ID: ${duplicateCheck.id}")
                            val updatedTransaction = userAmount.copy(id = duplicateCheck.id)
                            repository.updateUserAmount(updatedTransaction)
                        } else {
                            // No existing transaction found, insert as new
                            android.util.Log.d("FinanceViewModel", "No existing transaction found, inserting as new")
                            database.UsersDao().insertUserAmount(userAmount)
                        }
                    }
                }

                // Then update cached lists
                updateTransactionSender(userAmount)
                resetBalanceFlag() // Reset flag when transaction is updated

                // Finally, refresh the LiveData from database to ensure UI consistency
                val updatedList = withContext(Dispatchers.IO) {
                    repository.getAllUserAmountsLive().value ?: listOf()
                }
                _smsTransactions.postValue(updatedList)
                
                // Also refresh filtered transactions
                applyFilters()
                
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error updating transaction", e)
                _error.value = "Failed to update transaction: ${e.message}"
            }
        }
    }

    private val parseFormats = listOf(
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS a", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    // Transaction filter by day (UserAmount)
    fun filterTransactionsByDay(transactions: List<UserAmount>, date: Date): List<UserAmount> {
        val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val target = dayFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.time)
                    parsed != null && dayFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    // Transaction filter by month (UserAmount)
    fun filterTransactionsByMonth(transactions: List<UserAmount>, date: Date): List<UserAmount> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val target = monthFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.time)
                    parsed != null && monthFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    // Cash filter by day (UserCash)
    fun filterCashByDay(transactions: List<UserCash>, date: Date): List<UserCash> {
        val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val target = dayFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.cashTime)
                    parsed != null && dayFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    // Cash filter by month (UserCash)
    fun filterCashByMonth(transactions: List<UserCash>, date: Date): List<UserCash> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val target = monthFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.cashTime)
                    parsed != null && monthFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    // SMS format fallback filter by day (UserAmount, SMS-style time)
    fun filterSmsByDay(transactions: List<UserAmount>, date: Date): List<UserAmount> {
        val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val target = dayFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.time)
                    parsed != null && dayFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    fun filterCashByDate(transactions: List<UserCash>, date: Date): List<UserCash> {
        val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val target = dayFormat.format(date)

        return transactions.filter { txn ->
            parseFormats.any { format ->
                try {
                    val parsed = format.parse(txn.cashTime)
                    parsed != null && dayFormat.format(parsed) == target
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    fun filterTransactionsByDate(date: Date): List<UserAmount> {
        return TransactionUtils.filterTransactionsByDate(_smsTransactions.value ?: emptyList(), date)
    }

    fun updateFilterState(position: Int, date: Date?) {
        currentFilterPosition = position
        currentFilterDate = date
        applyFilters()
    }

    private fun applyFilters() {
        val transactions = _smsTransactions.value ?: emptyList()
        val filtered = when (currentFilterPosition) {
            1 -> { // Daily
                if (currentFilterDate != null) {
                    filterTransactionsByDay(transactions, currentFilterDate!!)
                } else {
                    filterTransactionsByDay(transactions, Date())
                }
            }
            2 -> { // Monthly
                if (currentFilterDate != null) {
                    filterTransactionsByMonth(transactions, currentFilterDate!!)
                } else {
                    filterTransactionsByMonth(transactions, Date())
                }
            }
            else -> transactions // All
        }
        _filteredTransactions.postValue(filtered)
    }

    fun checkCredentials(email: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val isValid = withContext(Dispatchers.IO) {
                    database.UsersDao().validateCredentials(email, password)
                }

                if (isValid) {
                    callback(true)
                } else {
                    _error.value = "Invalid email or password"
                    callback(false)
                }
            } catch (e: Exception) {
                _error.value = "An error occurred: ${e.message}"
                callback(false)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun applyAndStoreCashFilter(transactions: List<UserCash>, filterType: Int, date: Date? = null): List<UserCash> {
        val filteredList = when (filterType) {
            1 -> filterCashByDay(transactions, date ?: Date())
            2 -> filterCashByMonth(transactions, date ?: Date())
            3 -> if (date != null) filterCashByDate(transactions, date) else transactions
            else -> transactions
        }

        _filteredCashTransactions.postValue(filteredList)
        return filteredList
    }

    // Reset balance flag when a transaction is added or updated
    private fun resetBalanceFlag() {
        balancesCalculated = false
        lastCashUpdateTimestamp = 0L // Reset timestamp to force recalculation
        android.util.Log.d("FinanceViewModel", "Balance flags reset - will recalculate balances")
    }

    // Function to load the cash balance just once during app startup
    private fun loadInitialCashBalance() {
        viewModelScope.launch {
            try {
                if (balancesCalculated) {
                    android.util.Log.d("FinanceViewModel", "Skipping initial cash balance load, already calculated")
                    return@launch
                }
                
                val userId = SessionManager.getUserToken()
                if (userId == -1) return@launch
                
                val userBank = withContext(Dispatchers.IO) {
                    database.UsersDao().getUserBankByUserId(userId.toString())
                } ?: return@launch
                
                // Set the balance from the database value
                val cashBalance = userBank.cashAmount.toDoubleOrNull() ?: 0.0
                _cashBalance.value = cashBalance
                
                // Mark balance as calculated
                balancesCalculated = true
                lastCashUpdateTimestamp = System.currentTimeMillis()
                android.util.Log.d("FinanceViewModel", "Initial cash balance loaded: $cashBalance")
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Error loading initial cash balance", e)
            }
        }
    }
}