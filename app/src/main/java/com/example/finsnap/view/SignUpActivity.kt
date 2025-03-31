package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.finsnap.databinding.ActivitySignUpBinding
import com.example.finsnap.model.UserData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var database: UserDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database= Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "finsnap_database"
        ).build()
        binding.userSignUpSubmit.setOnClickListener {
            val userEmail = binding.userSignUpEmail.text.toString()
            val password = binding.userSignUpPassword.text.toString()

            if (userEmail.isNotEmpty() && password.isNotEmpty()) {
                GlobalScope.launch {
                    database.UsersDao().insertUser(UserData(email = userEmail, userpassword = password))
//                    Toast.makeText(this@SignUpActivity, "Signup successful!", Toast.LENGTH_SHORT).show()
                    Log.d("Signup", "Signup successful!")
                }
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                Toast.makeText(this@SignUpActivity, "fill all fields", Toast.LENGTH_SHORT).show()
            }


        }
        binding.signUpTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }


    }
}