package com.example.finsnap.viewmodel

import androidx.lifecycle.ViewModel
import com.example.finsnap.model.Amout_Repository
import com.example.finsnap.model.UserBank
import com.example.finsnap.model.UserData
import com.example.finsnap.view.BankDetails

class FinanceViewModel: ViewModel() {
    fun InsertBankDetail(bankName:String,currentAmount:Double): UserBank {
       return UserBank(
            bankName = bankName,
            currentAmount = currentAmount,


            )

    }

    fun getRepo(): Amout_Repository {
        return Amout_Repository()
    }
    fun InsertUserData(email:String,password:String): UserData {
        return UserData(
            email = email,
            userpassword = password
        )

    }

}