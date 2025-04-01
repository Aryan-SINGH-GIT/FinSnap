package com.example.finsnap.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "userBank")
data class UserBank(
    @PrimaryKey(autoGenerate = true) val
    bankid: Int = 0,
    val bankName: String,
    val currentAmount: Double

)
