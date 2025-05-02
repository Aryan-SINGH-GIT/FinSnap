package com.example.finsnap.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import com.example.finsnap.R
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "user_amounts")
data class UserAmount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // required for Room to identify records
    var sender: String,
    val time: String,
    val amtChange: String,
    val amtImage: Int,
    val rawMessage: String,
    val amount: Double = 0.0,
    val isCredit: Boolean = false,
    var category: String = "Miscellaneous",
    var categoryImage: Int = R.drawable.ic_miscellaneous // 👈 NEW FIELD
) : Parcelable
