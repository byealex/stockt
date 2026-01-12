package com.example.stockt.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "shelves",
    foreignKeys = [
        ForeignKey(
            entity = StorageUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["storageId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val storageId: Int
)