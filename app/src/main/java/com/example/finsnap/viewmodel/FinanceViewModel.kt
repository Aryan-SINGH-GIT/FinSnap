package com.example.finsnap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.finsnap.model.Amout_Repository
import com.example.finsnap.model.UserAmount
import com.example.finsnap.model.UserBank
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
}