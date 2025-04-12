package com.example.finsnap.model

import com.example.finsnap.R

class Amout_Repository {

    fun getAmountItems(): List<UserAmount> {
        return listOf(
            UserAmount(
                "Kariapaa",
                "everyTime",
                "-infinity",
                R.drawable.logo

            )
        )
    }
}