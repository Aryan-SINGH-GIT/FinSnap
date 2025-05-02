package com.example.finsnap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    // Event to notify a specific item was updated
    private val _updatedTransaction = MutableLiveData<UserAmount>()
    val updatedTransaction: LiveData<UserAmount> = _updatedTransaction

    fun InsertBankDetail(bankName: String, currentAmount: Double): UserBank {
        val userId = SessionManager.getUserToken()
        return UserBank(
            bankName = bankName,
            currentAmount = currentAmount,
            userId = userId
        )
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