package com.example.stockt.db

import androidx.room.Embedded
import androidx.room.Relation

data class ShelfWithItemsRelation(
    @Embedded val shelfEntity: ShelfEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "shelfId"
    )
    val itemEntities: List<ItemEntity>
)