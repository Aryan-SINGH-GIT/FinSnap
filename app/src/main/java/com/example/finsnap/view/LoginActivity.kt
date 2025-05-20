package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.finsnap.databinding.ActivityLoginBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.example.finsnap.viewmodel.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: UserDatabase
    private lateinit var viewModel: FinanceViewModel
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        SessionManager.init(this)

        // Inflate binding and set content view
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[FinanceViewModel::class.java]

        // Initialize database
        database = DatabaseManager.getDatabase(applicationContext)

        // Set up login button click listener
        binding.loginSubmit.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (validateInput(email, password)) {
                binding.loginSubmit.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val isValid = withContext(Dispatchers.IO) {
                            database.UsersDao().validateCredentials(email, password)
                        }

                        if (isValid) {
                            // Get user ID and save it
                            val userId = database.UsersDao().getUserByEmail(email)
                            if (userId != null) {
                                SessionManager.saveUserToken(userId.toInt())
                                SessionManager.saveUserEmail(email)
                                SessionManager.setLoggedIn(true)
                                
                                // Now check bank details and navigate
                                val isCompleted = SessionManager.isBankDetailsActuallyCompleted(this@LoginActivity)
                                if (isCompleted) {
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                } else {
                                    startActivity(Intent(this@LoginActivity, BankDetails::class.java))
                                }
                                finish()
                            }
                        } else {
                            Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Login error", e)
                        Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        binding.loginSubmit.isEnabled = true
                    }
                }
            }
        }

        // Sign up text click
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Forgot password click
//        binding.forgotPassword.setOnClickListener {
//            Toast.makeText(this, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
//        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Clear previous errors
        binding.loginEmail.error = null
        binding.loginPassword.error = null

        // Email validation
        if (email.isEmpty()) {
            binding.loginEmail.error = "Email is required"
            binding.loginEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmail.error = "Please enter a valid email address"
            binding.loginEmail.requestFocus()
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.loginPassword.error = "Password is required"
            binding.loginPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.loginPassword.error = "Password must be at least 6 characters"
            binding.loginPassword.requestFocus()
            return false
        }

        return true
    }
}