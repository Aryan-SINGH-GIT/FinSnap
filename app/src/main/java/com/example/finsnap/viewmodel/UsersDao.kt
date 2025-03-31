package com.example.finsnap.viewmodel

import com.example.finsnap.model.UserData
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


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


    @Query("SELECT * FROM users")
    fun getAllStudents(): LiveData<List<UserData>>

}