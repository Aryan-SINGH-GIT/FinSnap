package com.example.finsnap.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.finsnap.R
import com.example.finsnap.viewmodel.SessionManager
import java.util.concurrent.Executor
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.core.content.ContextCompat.getMainExecutor

class LauncherActivity : AppCompatActivity() {
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    companion object {
        private const val SMS_PERMISSION_CODE = 100
        private const val TAG = "LaunchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // Initialize SessionManager
        SessionManager.init(applicationContext)

        // Check if user is logged in
        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup executor for biometric authentication
        executor = getMainExecutor(this)

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE
            )
        } else {
            setupBiometricAuth()
        }
    }

    private fun setupBiometricAuth() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Create BiometricPrompt
                biometricPrompt = BiometricPrompt(this, executor,
                    object : AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Log.e(TAG, "Authentication error: $errString (Code: $errorCode)")

                            // Handle specific error codes
                            when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED -> {
                                    // User explicitly canceled
                                    finish()
                                }
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                                    // User clicked negative button
                                    finish()
                                }
                                else -> {
                                    // For other errors, show toast and finish
                                    Toast.makeText(
                                        this@LauncherActivity,
                                        "Authentication error: $errString",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            }
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Log.d(TAG, "Authentication succeeded")

                            // Verify user is still logged in before navigating
                            if (SessionManager.isLoggedIn()) {
                                startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                                finish()
                            } else {
                                // If somehow logged out during auth, redirect to login
                                startActivity(Intent(this@LauncherActivity, LoginActivity::class.java))
                                finish()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Log.w(TAG, "Authentication failed")
                            Toast.makeText(
                                this@LauncherActivity,
                                "Authentication failed. Try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                // Create BiometricPrompt Info
                promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Authentication Required")
                    .setSubtitle("Verify your identity to access FinSnap")
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                // Authenticate
                try {
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "Biometric authentication setup failed", e)
                    Toast.makeText(
                        this,
                        "Failed to start biometric authentication",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "No biometric hardware available")
                proceedToMainActivity()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "Biometric hardware unavailable")
                proceedToMainActivity()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "No biometrics enrolled")
                proceedToMainActivity()
            }
            else -> {
                Log.e(TAG, "Unknown biometric error")
                proceedToMainActivity()
            }
        }
    }

    private fun proceedToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupBiometricAuth()
            } else {
                Toast.makeText(this, "SMS permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}