package com.example.stockt.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_units")
data class StorageUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
