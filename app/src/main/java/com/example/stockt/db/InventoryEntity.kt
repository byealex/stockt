package com.example.stockt.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventories",
    foreignKeys = [
        ForeignKey(
            entity = StorageUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val storageId: Int
)