package com.example.finsnap.view

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.finsnap.R
import com.example.finsnap.databinding.ActivityBankDetailsBinding
import com.example.finsnap.model.UserBank
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BankDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBankDetailsBinding
    private lateinit var database: UserDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityBankDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database= Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "finsnap_database"
        ).build()

        val bankNames = resources.getStringArray(R.array.bank_list) // bank_list should be in strings.xml
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bankNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.bankSpinner.adapter = adapter

        // Save user data to the database when button is clicked
        binding.btnGetStarted.setOnClickListener {
            val selectedBankName = binding.bankSpinner.selectedItem.toString()
            val currentAmount = binding.currentAmount.text.toString().toDouble()

            GlobalScope.launch {
                val userBank = UserBank(
                    bankName = selectedBankName,
                    currentAmount = currentAmount
                )
                database.UsersDao().insertUserBank(userBank)
            }

    }
}
}