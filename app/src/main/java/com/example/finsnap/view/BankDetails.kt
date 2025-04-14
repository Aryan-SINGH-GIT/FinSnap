package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.ActivityBankDetailsBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.UserDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class BankDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBankDetailsBinding
    private lateinit var database: UserDatabase
    private lateinit var myfinanaceViewModel: FinanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        myfinanaceViewModel= ViewModelProvider(this).get(FinanceViewModel::class.java)

        database= Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "finsnap_database"
        ).fallbackToDestructiveMigration().build()



        val bankNames = resources.getStringArray(R.array.bank_list) // bank_list should be in strings.xml
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bankNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.bankSpinner.adapter = adapter

        // Save user data to the database when button is clicked
        binding.btnGetStarted.setOnClickListener {
            val selectedBankName = binding.bankSpinner.selectedItem.toString()
            val currentAmount = binding.currentAmount.text.toString().toDouble()

            GlobalScope.launch {
                val userBank =myfinanaceViewModel.InsertBankDetail(selectedBankName,currentAmount)
                database.UsersDao().insertUserBank(userBank)
                startActivity(Intent(this@BankDetails, MainActivity::class.java))



            }

    }
}


}