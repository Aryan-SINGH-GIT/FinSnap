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
import com.example.finsnap.databinding.ActivitySignUpBinding
import com.example.finsnap.viewmodel.FinanceViewModel
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase
import com.example.finsnap.viewmodel.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var database: UserDatabase
    private lateinit var viewModel: FinanceViewModel
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        SessionManager.init(this)

        // If user is already logged in, skip signup
        if (SessionManager.isLoggedIn()) {
            navigateBasedOnUserState()
            return
        }

        // Inflate binding and set content view
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel and Database
        viewModel = ViewModelProvider(this)[FinanceViewModel::class.java]
        database = DatabaseManager.getDatabase(applicationContext)

        // Setup UI
        setupUI()
    }

    private fun setupUI() {
        // Sign Up Submit Button
        binding.userSignUpSubmit.setOnClickListener {
            val email = binding.userSignUpEmail.text.toString().trim()
            val password = binding.userSignUpPassword.text.toString()
       //     val confirmPassword = binding.userSignUpConfirmPassword.text.toString()

            // Validate input
            if (validateInput(email, password)) {
                // Show progress
              //  binding.progressBar.visibility = View.VISIBLE
                binding.userSignUpSubmit.isEnabled = false

                // Perform signup
                lifecycleScope.launch {
                    try {
                        // Check if user already exists
                        val userExists = withContext(Dispatchers.IO) {
                            database.UsersDao().getUserByEmail(email) != null
                        }

                        if (userExists) {
                            withContext(Dispatchers.Main) {
                             //   binding.progressBar.visibility = View.GONE
                                binding.userSignUpSubmit.isEnabled = true
                                binding.userSignUpEmail.error = "Email already registered"
                                binding.userSignUpEmail.requestFocus()
                            }
                            return@launch
                        }

                        // Create user data
                        val userData = viewModel.InsertUserData(email, password)

                        // Insert user into database
                        withContext(Dispatchers.IO) {
                            database.UsersDao().insertUser(userData)
                        }

                        // Save user email (but not logged in)
                        SessionManager.saveUserEmail(email)

                        // Navigate to login screen
                        withContext(Dispatchers.Main) {
                         //   binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@SignUpActivity, "Signup successful! Please login.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                         //   binding.progressBar.visibility = View.GONE
                            binding.userSignUpSubmit.isEnabled = true
                            Log.e(TAG, "Signup error", e)
                            Toast.makeText(this@SignUpActivity, "Signup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Navigate to Login
        binding.signUpTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun navigateBasedOnUserState() {
        lifecycleScope.launch {
            try {
                val isCompleted = SessionManager.isBankDetailsActuallyCompleted(this@SignUpActivity)

                withContext(Dispatchers.Main) {
                    if (isCompleted) {
                        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@SignUpActivity, BankDetails::class.java))
                    }
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Navigation error", e)
                    Toast.makeText(this@SignUpActivity, "Error checking account status", Toast.LENGTH_SHORT).show()
                    SessionManager.logout(this@SignUpActivity)
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Clear previous errors
        binding.userSignUpEmail.error = null
        binding.userSignUpPassword.error = null
       // binding.userSignUpConfirmPassword.error = null

        // Email validation
        if (email.isEmpty()) {
            binding.userSignUpEmail.error = "Email is required"
            binding.userSignUpEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.userSignUpEmail.error = "Please enter a valid email address"
            binding.userSignUpEmail.requestFocus()
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            binding.userSignUpPassword.error = "Password is required"
            binding.userSignUpPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.userSignUpPassword.error = "Password must be at least 6 characters"
            binding.userSignUpPassword.requestFocus()
            return false
        }

        // Confirm password validation
//        if (confirmPassword.isEmpty()) {
//         //   binding.userSignUpConfirmPassword.error = "Please confirm your password"
//          //  binding.userSignUpConfirmPassword.requestFocus()
//            return false
//        }
//
//        if (password != confirmPassword) {
//          //  binding.userSignUpConfirmPassword.error = "Passwords do not match"
//           // binding.userSignUpConfirmPassword.requestFocus()
//            return false
//        }

        return true
    }
}