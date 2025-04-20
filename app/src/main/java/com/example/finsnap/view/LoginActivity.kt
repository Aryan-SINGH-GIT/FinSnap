package com.example.finsnap.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.finsnap.databinding.ActivityLoginBinding
import com.example.finsnap.viewmodel.SessionManager
import com.example.finsnap.viewmodel.UserDatabase


import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var database: UserDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        database= Room.databaseBuilder(
            applicationContext,
            UserDatabase::class.java,
            "finsnap_database"
        ).fallbackToDestructiveMigration().build()


        binding.loginSubmit.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString();
            val biometricManager = BiometricManager.from(this)

            if (email.isNotEmpty() && password.isNotEmpty()) {
                var isValid=false
                GlobalScope.launch {
                    isValid = database.UsersDao().validateCredentials(email, password)
                    if (isValid){
                        var userId=database.UsersDao().getUserByEmail(email)
                       SessionManager.saveUserToken(userId.toString())
                    }


                    runOnUiThread {

                        if (isValid) {

                            Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the next screen after successful login
                           // startActivity(Intent(this@LoginActivity, BankDetails::class.java))

                            when(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)){

                                BiometricManager.BIOMETRIC_SUCCESS->{

                                    authenticateUser();

                                }

                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED->{
                                    Toast.makeText(this@LoginActivity,"Biometric NOt Available",Toast.LENGTH_LONG).show()
                                }
                            }



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

    private fun authenticateUser() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt= BiometricPrompt(this,executor, object : BiometricPrompt.AuthenticationCallback()  {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
//                Toast.makeText(applicationContext,"FingerPrint MATCHED!",Toast.LENGTH_LONG).show()
                startActivity(Intent(applicationContext,BankDetails::class.java))
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext,"FingerPrint MATCHED Failed!",Toast.LENGTH_LONG).show()



            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext,"FingerPrint error: $errString",Toast.LENGTH_LONG).show()
            }

        })

        val promptInfo= BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Use FingerPrint to open app")
            .setNegativeButtonText("cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }



}