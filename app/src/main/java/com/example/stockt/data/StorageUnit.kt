package com.example.stockt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

class StorageUnit {
    data class StorageUnit(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val name: String
    )
}