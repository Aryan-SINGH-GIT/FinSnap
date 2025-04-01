package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.finsnap.databinding.ActivityLoginBinding
import com.example.finsnap.model.UserData

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: UserDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database= Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "finsnap_database"
        ).build()


        binding.loginSubmit.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString();

            if (email.isNotEmpty() && password.isNotEmpty()) {
                var isValid=false
                GlobalScope.launch {
                    isValid = database.UsersDao().validateCredentials(email, password)

                    runOnUiThread {

                        if (isValid) {
                            Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the next screen after successful login
                            startActivity(Intent(this@LoginActivity, BankDetails::class.java))
                        } else {
                            Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this@LoginActivity, "fill all fields", Toast.LENGTH_SHORT).show()
            }

        }

        binding.loginText.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
        }

    }


}