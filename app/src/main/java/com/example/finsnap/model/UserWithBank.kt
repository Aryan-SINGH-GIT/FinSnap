package com.example.finsnap.model

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithBank(
    @Embedded val user: UserData,

    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val bank: UserBank
)
