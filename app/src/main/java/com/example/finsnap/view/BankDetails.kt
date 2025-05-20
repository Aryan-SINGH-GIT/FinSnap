package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.ActivityBankDetailsBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.example.finsnap.viewmodel.DatabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class BankDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBankDetailsBinding
    private lateinit var database: UserDatabase
    private lateinit var myfinanaceViewModel: FinanceViewModel
    private var isUpdateMode = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SessionManager.init(applicationContext)
        myfinanaceViewModel= ViewModelProvider(this)[FinanceViewModel::class.java]

        database= DatabaseManager.getDatabase(applicationContext)

        val bankNames = resources.getStringArray(R.array.bank_list) // bank_list should be in strings.xml
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bankNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.bankSpinner.adapter = adapter

        // Check if we're in update mode
        isUpdateMode = intent.getBooleanExtra("isUpdate", false)
        if (isUpdateMode) {
            binding.title.text = "Update Bank Details"
            binding.btnGetStarted.text = "Update Details"
            loadExistingBankDetails()
        }

        // Save/Update user data
        binding.btnGetStarted.setOnClickListener {
            val selectedBankName = binding.bankSpinner.selectedItem.toString()
            val currentAmount = binding.currentAmount.text.toString().toDoubleOrNull() ?: 0.0
            val cashAmount = binding.cashAmount.text.toString().toDoubleOrNull() ?: 0.0
            val savingTarget = binding.savingtarget.text.toString().toDoubleOrNull() ?: 0.0

            if (currentAmount < 0 || cashAmount < 0 || savingTarget < 0) {
                Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            GlobalScope.launch {
                val userBank = myfinanaceViewModel.InsertBankDetail(
                    selectedBankName,
                    currentAmount,
                    cashAmount,
                    savingTarget
                )
                
                if (isUpdateMode) {
                    database.UsersDao().updateUserBank(userBank)
                    Toast.makeText(this@BankDetails, "Bank details updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    database.UsersDao().insertUserBank(userBank)
                    SessionManager.saveBankSetupTime(System.currentTimeMillis())
                   SessionManager.setBankDetailsCompleted(true)
                }

                // Update the current balance in ViewModel
                myfinanaceViewModel.calculateCurrentBalance(currentAmount)

                if (!isUpdateMode) {
                    startActivity(Intent(this@BankDetails, MainActivity::class.java))
                }
                finish()
            }
        }
    }

    private fun loadExistingBankDetails() {
        GlobalScope.launch {
            val userId = SessionManager.getUserToken()
            val userBank = database.UsersDao().getUserBankByUserId(userId.toString())


            userBank?.let {
                runOnUiThread {
                    // Set spinner to current bank
                    val bankNames = resources.getStringArray(R.array.bank_list)
                    val position = bankNames.indexOf(it.bankName)
                    if (position != -1) {
                        binding.bankSpinner.setSelection(position)
                    }

                    // Set current values
                    binding.currentAmount.setText(it.currentAmount.toString())
                    binding.cashAmount.setText(it.cashAmount)
                    binding.savingtarget.setText(it.savingTarget)
                }
            }
        }
    }
}