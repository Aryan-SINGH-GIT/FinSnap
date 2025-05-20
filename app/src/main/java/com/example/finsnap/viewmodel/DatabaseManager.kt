package com.example.finsnap.viewmodel

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Centralized database manager that handles database creation and migrations
 */
object DatabaseManager {
    private const val TAG = "DatabaseManager"
    private const val DB_NAME = "UserDatabase"
    
    /**
     * Get the database instance with migration handling
     */
    fun getDatabase(context: Context): UserDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            UserDatabase::class.java,
            DB_NAME
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                // When destructive migration happens, set logged in flag to false
                Log.i(TAG, "Database version changed, logging out user")
                SessionManager.setLoggedIn(false)
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }
} 