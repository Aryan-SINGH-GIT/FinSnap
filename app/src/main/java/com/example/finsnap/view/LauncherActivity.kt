package com.example.finsnap.view

import android.content.Intent
import android.os.Bundle


import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finsnap.model.Amout_Repository
import com.example.finsnap.viewmodel.SessionManager

import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        // Check if user is logged in according to SharedPreferences
        if (SessionManager.isLoggedIn()) {
            // Validate if the user really exists in database
            validateUserAndNavigate()
        } else {
            navigateToSignUp()
        }
    }

    private fun validateUserAndNavigate() {
        val repository = Amout_Repository(this)
        val userId = SessionManager.getUserToken()

        lifecycleScope.launch {
            try {
                // Try to get user info from database
                val userExists = if (userId != null) {
                    val email = SessionManager.getUserEmail()
                    email?.let { repository.getUserExists(it) } ?: false
                } else {
                    false
                }
                if (!userExists) {
                    // User doesn't exist in database, clear preferences and go to signup
                    Log.d("LauncherActivity", "User doesn't exist in database, resetting session")
                    SessionManager.logout()
                    navigateToSignUp()
                } else if (!SessionManager.isBankDetailsCompleted()) {
                    navigateToBankDetails()
                } else {
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e("LauncherActivity", "Database validation failed: ${e.message}")
                // On any database error, reset session and go to signup
                SessionManager.logout()
                navigateToSignUp()
            }
        }
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
        Log.d("LauncherActivity", "Navigated to SignUpActivity")
        finish()
    }

    private fun navigateToBankDetails() {
        startActivity(Intent(this, BankDetails::class.java))
        Log.d("LauncherActivity", "Navigated to BankDetails")
        finish()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        Log.d("LauncherActivity", "Navigated to MainActivity")
        finish()
    }
}