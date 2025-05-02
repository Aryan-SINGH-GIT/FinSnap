package com.example.finsnap.viewmodel


import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.finsnap.model.UserAmount
import com.example.finsnap.model.UserBank
import com.example.finsnap.model.UserCash
import com.example.finsnap.model.UserData



@Database(entities = [UserData::class, UserBank::class, UserCash::class, UserAmount::class], version = 11)
abstract class UserDatabase : RoomDatabase() {
    abstract fun UsersDao(): UsersDao
}


