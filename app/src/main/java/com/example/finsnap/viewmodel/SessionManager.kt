package com.example.finsnap.viewmodel

import android.content.Context
import android.content.SharedPreferences


object SessionManager {
        private const val PREF_NAME = "user_session"
        private lateinit var prefs: SharedPreferences

        fun init(context: Context) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }

        fun saveUserToken(token: String) {
            prefs.edit().putString("user_token", token).apply()
        }

        fun getUserToken(): String? {
            return prefs.getString("user_token", null)
        }

        fun setLoggedIn(isLoggedIn: Boolean) {
            prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
        }

        fun isLoggedIn(): Boolean {
            return prefs.getBoolean("is_logged_in", false)
        }

        fun logout() {
            prefs.edit().clear().apply()
        }
    }

