package com.example.finsnap.model
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finsnap.R

@Entity(tableName = "userCash")

data class UserCash(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cashSender: String,
    val cashTime: String,
    val CashamtChange: String,
    val cashImage: Int= R.drawable.ic_miscellaneous,
    val categoryName: String = "Miscellaneous"

)