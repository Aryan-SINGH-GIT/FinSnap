package com.example.finsnap.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    tableName = "userBank",
//    foreignKeys = [
//        ForeignKey(
//            entity = UserData::class,
//            parentColumns = ["id"],
//            childColumns = ["userId"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ]
)
data class UserBank(
    @PrimaryKey(autoGenerate = true)
    val bankid: Int = 0,
//    val userId: Int =0,/////////////////////problemmmmmmmmmm
    val bankName: String,
    val currentAmount: Double
)
