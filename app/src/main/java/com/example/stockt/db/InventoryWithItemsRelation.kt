package com.example.stockt.db

import androidx.room.Embedded
import androidx.room.Relation

data class InventoryWithItemsRelation(
    @Embedded val inventoryEntity: InventoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "inventoryId"
    )
    val itemEntities: List<ItemEntity>
)