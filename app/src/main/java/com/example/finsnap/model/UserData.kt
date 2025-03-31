package com.example.finsnap.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity (tableName = "users")
data class UserData (
    @PrimaryKey(autoGenerate = true)
    var id: Int=0,
    var email:String,
    var userpassword:String,
)