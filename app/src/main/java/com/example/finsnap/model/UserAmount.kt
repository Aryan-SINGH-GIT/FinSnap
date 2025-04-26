package com.example.finsnap.model

data class UserAmount(
    val sender: String,
    val time: String,
    val amtChange: String,
    val amtImage: Int,
    val rawMessage: String,
    val amount: Double = 0.0,  // New field for numeric value
    val isCredit: Boolean = false  // Whether this is a credit or debit
)
