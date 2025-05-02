package com.example.finsnap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.finsnap.model.Amout_Repository
import com.example.finsnap.model.UserAmount
import com.example.finsnap.model.UserBank
import com.example.finsnap.model.UserCash
import com.example.finsnap.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Change to AndroidViewModel to access application context
class FinanceViewModel(application: Application): AndroidViewModel(application) {

    // Initialize repository with application context
    private val repository = Amout_Repository(application)

    // Get a reference to the UserDatabase
    private val database:UserDatabase =   Room.databaseBuilder(
        application.applicationContext,
        UserDatabase::class.java,
        "UserDatabase"
    ).fallbackToDestructiveMigration().build()

    private val _smsTransactions = MutableLiveData<List<UserAmount>>()
    val smsTransactions: LiveData<List<UserAmount>> = _smsTransactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _currentBalance = MutableLiveData<Double>()
    val currentBalance: LiveData<Double> = _currentBalance

    private val _cashTransactions = MutableLiveData<List<UserCash>>()
    val cashTransactions: LiveData<List<UserCash>> = _cashTransactions

    private val _cashBalance = MutableLiveData<Double>()
    val cashBalance: LiveData<Double> = _cashBalance

    // Event to notify a specific item was updated
    private val _updatedTransaction = MutableLiveData<UserAmount>()
    val updatedTransaction: LiveData<UserAmount> = _updatedTransaction

    fun InsertBankDetail(bankName: String, currentAmount: Double,cashAmount:Double,savingTarget:Double): UserBank {
        val userId = SessionManager.getUserToken()

        _cashBalance.postValue(cashAmount)

        return UserBank(
            bankName = bankName,
            currentAmount = currentAmount,
            userId = userId,
            cashAmount = cashAmount.toString(),
            savingTarget = savingTarget.toString()
        )
    }


    fun loadUserBankDetails() {
        viewModelScope.launch {
            try {
                val userId = SessionManager.getUserToken().toString()
                withContext(Dispatchers.IO) {
                    val userBank = database.UsersDao().getUserBankByUserId(userId)
                    userBank?.let {
                        withContext(Dispatchers.Main) {
                            _currentBalance.value = it.currentAmount
                            _cashBalance.value = it.cashAmount.toDoubleOrNull() ?: 0.0
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
                val transactions = withContext(Dispatchers.IO) {
                    repository.getBankSmsTransactions()
                }
                _smsTransactions.value = transactions
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Failed to load SMS transactions: ${e.message}"
                _isLoading.value = false
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
                val transactions = withContext(Dispatchers.IO) {
                    repository.getBankSmsTransactions()
                }

                var updatedBalance = initialBalance

                // Calculate the new balance based on transactions
                transactions.forEach { transaction ->
                    if (transaction.isCredit) {
                        updatedBalance += transaction.amount
                    } else {
                        updatedBalance -= transaction.amount
                    }
                }

                _currentBalance.value = updatedBalance
            } catch (e: Exception) {
                _error.value = "Failed to calculate balance: ${e.message}"
            }
        }
    }


    fun insertCashTransaction(userCash: UserCash) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.insertCashTransaction(userCash)
                }
                loadCashTransactions()
            } catch (e: Exception) {
                _error.value = "Failed to save transaction: ${e.message}"
            }
        }
    }

    // Load cash transactions
    fun loadCashTransactions() {
        viewModelScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    repository.getCashTransactions()
                }
                _cashTransactions.value = transactions
            } catch (e: Exception) {
                _error.value = "Failed to load cash transactions: ${e.message}"
            }
        }
    }

    // Update cash balance when a transaction is made
    fun updateCashBalance(amount: Double, isCredit: Boolean) {
        viewModelScope.launch {
            try {
                // Get current cash balance
                val currentCashBalance = _cashBalance.value ?: 0.0

                // Calculate new balance based on transaction type
                val newBalance = if (isCredit) {
                    currentCashBalance + amount
                } else {
                    currentCashBalance - amount
                }

                // Update the cash balance in LiveData
                _cashBalance.value = newBalance

                // Update the cash balance in the database
                val userId = SessionManager.getUserToken().toString()
                withContext(Dispatchers.IO) {
                    // Get the current bank details
                    val userBank = database.UsersDao().getUserBankByUserId(userId)

                    // Update the cash amount
                    userBank?.let {
                        val updatedBank = it.copy(
                            cashAmount = newBalance.toString()
                        )
                        database.UsersDao().updateUserBank(updatedBank)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to update cash balance: ${e.message}"
            }
        }
    }


    // Update transaction sender name and notify via event
    fun updateTransactionSender(updatedTransaction: UserAmount) {
        // Add debugging logs
        android.util.Log.d("FinanceViewModel", "Updating transaction sender: ${updatedTransaction.sender}")
        android.util.Log.d("FinanceViewModel", "Raw message: ${updatedTransaction.rawMessage}")
        android.util.Log.d("FinanceViewModel", "Time: ${updatedTransaction.time}")

        // Update the transaction in the current list
        val currentList = _smsTransactions.value?.toMutableList() ?: mutableListOf()

        // Log current list size
        android.util.Log.d("FinanceViewModel", "Current list size: ${currentList.size}")

        // Find the transaction with the same raw message and time
        val index = currentList.indexOfFirst {
            it.rawMessage == updatedTransaction.rawMessage && it.time == updatedTransaction.time
        }

        android.util.Log.d("FinanceViewModel", "Found index: $index")

        if (index != -1) {
            // Log before change
            android.util.Log.d("FinanceViewModel", "Old sender: ${currentList[index].sender}")

            // Replace the transaction with the updated one
            currentList[index] = updatedTransaction

            // Log after change
            android.util.Log.d("FinanceViewModel", "New sender in list: ${currentList[index].sender}")

            // Important: Set the value to a NEW list to trigger the observer
            _smsTransactions.value = ArrayList(currentList)

            // Emit event for the specific update - force a new object to trigger observers
            _updatedTransaction.postValue(updatedTransaction.copy())

            android.util.Log.d("FinanceViewModel", "Posted updated transaction event")
        } else {
            android.util.Log.e("FinanceViewModel", "Could not find transaction to update!")
        }
    }

    fun loadSmsTransactionsFromDb(): LiveData<List<UserAmount>> {
        return repository.getAllUserAmountsLive()
    }

    fun getAllUserAmountsLive(): LiveData<List<UserAmount>> {
        return repository.getAllUserAmountsLive()
    }

    fun updateTransactionInDb(userAmount: UserAmount) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUserAmount(userAmount)
        }
    }



}