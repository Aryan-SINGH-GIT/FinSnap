package com.example.finsnap.viewmodel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.room.Room
import com.example.finsnap.view.LoginActivity
import com.example.finsnap.viewmodel.DatabaseManager

object SessionManager {
    private const val PREF_NAME = "user_session"
    private lateinit var prefs: SharedPreferences
    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
        prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

//    fun saveUserToken(token: String) {
//        prefs.edit().putString("user_token", token).apply()
//    }
//
//    fun getUserToken(): String? {
//        return prefs.getString("user_token", null)
//    }

    fun saveUserToken(userId: Int) {
        prefs.edit().putInt("user_id", userId).apply()
    }

    fun getUserToken(): Int {
        return prefs.getInt("user_id", -1) // -1 means not found
    }




    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun logout(context: Context? = null) {
        // Clear all preferences
        prefs.edit().clear().apply()

        // Reset logged in state
        setLoggedIn(false)

        // If context is provided, navigate to Login activity
        context?.let {
            val intent = Intent(it, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            it.startActivity(intent)
        }
    }

    fun setBankDetailsCompleted(completed: Boolean) {
        prefs.edit().putBoolean("bank_details_completed", completed).apply()
    }

    suspend fun isBankDetailsActuallyCompleted(context: Context): Boolean {
        return try {
            val database = DatabaseManager.getDatabase(context.applicationContext)

            val userId = SessionManager.getUserToken()
            if (userId == -1) {
                Log.e("SessionManager", "Invalid user ID")
                return false
            }

            val userBank = database.UsersDao().getUserBankByUserId(userId.toString())
            userBank != null
        } catch (e: Exception) {
            Log.e("SessionManager", "Error checking bank details: ${e.message}")
            false
        }
    }



    fun saveUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun saveBankSetupTime(timeMillis: Long) {
        prefs.edit().putLong("bank_setup_time", timeMillis).apply()
    }

    fun getBankSetupTime(): Long {
        return prefs.getLong("bank_setup_time", 0L)
    }
    
    /**
     * Save the timestamp of the last SMS fetch operation 
     * @param timeMillis The timestamp in milliseconds when SMS were last fetched
     */
    fun saveLastSmsFetchTime(timeMillis: Long) {
        prefs.edit().putLong("last_sms_fetch_time", timeMillis).apply()
        
        // Also save a flag indicating whether first fetch has been done
        if (!hasCompletedFirstFetch()) {
            prefs.edit().putBoolean("first_sms_fetch_complete", true).apply()
        }
    }
    
    /**
     * Get the timestamp of when SMS were last fetched
     * @return The timestamp in milliseconds, or 0 if never fetched
     */
    fun getLastSmsFetchTime(): Long {
        return prefs.getLong("last_sms_fetch_time", 0L)
    }
    
    /**
     * Check if the first SMS fetch operation has been completed
     * @return true if the first fetch has been done, false otherwise
     */
    fun hasCompletedFirstFetch(): Boolean {
        return prefs.getBoolean("first_sms_fetch_complete", false) 
    }
}

