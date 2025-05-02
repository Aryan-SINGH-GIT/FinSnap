package com.example.finsnap.viewmodel

import com.example.finsnap.model.UserData
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.finsnap.model.UserAmount
import com.example.finsnap.model.UserBank
import com.example.finsnap.model.UserCash
import com.example.finsnap.model.UserWithBank

//import com.example.finsnap.model.UserWithBank


@Dao
interface UsersDao {
    @Insert
    suspend fun insertUser(userData: UserData)

    @Update
    suspend fun updateUser(userData: UserData)

    @Delete
    suspend fun deleteUser(userData: UserData)

//
//    @Query("SELECT * FROM students WHERE name = :name")
//    suspend fun getStudentByName(name: String): Students?
//
//    @Query("SELECT * FROM students WHERE password = :password")
//    suspend fun getStudentByPassword(password: String): Students?

    // Add a method to validate credentials
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :useremail AND userpassword = :password)")
    suspend fun validateCredentials(useremail: String, password: String): Boolean

    @Query("SELECT currentAmount FROM userBank WHERE userId = :userId")
    suspend fun getCurrentAmount(userId: Int): Double

    @Query("SELECT bankName FROM userBank WHERE userId = :userId")
    suspend fun getBankName(userId: Int): String

    @Query("SELECT * FROM users")
    fun getAllStudents(): LiveData<List<UserData>>

    @Query("SELECT ID FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String):Number?

    @Insert
    suspend fun insertUserBank(userBank: UserBank)

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserWithBank(userId: Int): LiveData<UserWithBank>

    @Insert
    suspend fun insertUserCash(userCash: UserCash)

    @Query("SELECT * FROM userCash ORDER BY id DESC")
    suspend fun getAllCashTransactions(): List<UserCash>

    @Insert
    suspend fun insertUserAmount(userAmount: UserAmount)

    @Update
    suspend fun updateUserAmount(userAmount: UserAmount)

    @Query("SELECT * FROM user_amounts ORDER BY id DESC")
    fun getAllUserAmounts(): LiveData<List<UserAmount>>

    @Query("SELECT * FROM user_amounts ORDER BY id DESC")
    suspend fun getAllUserAmountsNow(): List<UserAmount>

    // In UsersDao.kt, change:
//    @Query("SELECT ID FROM users WHERE email = :email LIMIT 1")
//    suspend fun getUserByEmail(email: String): UserData? // Return the full user object or null


}