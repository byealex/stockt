package com.example.stockt.data

import com.example.stockt.db.ItemEntity
import com.example.stockt.db.ShelfEntity
import com.example.stockt.db.ShelfWithItemsRelation
import com.example.stockt.db.StocktDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StocktRepository(private val dao: StocktDao) {

    //temp solution
    suspend fun createDefaultFridge() {
        // We force ID = 1 so our hardcoded app logic works
        dao.insertStorageUnit(
            com.example.stockt.db.StorageUnitEntity(
                id = 1,
                name = "Default Fridge"
            )
        )
    }

    // --- READING ---
    fun getShelvesForStorageUnit(storageUnitId: Int): Flow<List<ShelfWithItems>> {
        return dao.getShelvesWithItemsForUnit(storageUnitId).map {
            relationList -> relationList.map { it.toDomainModel() }
        }
    }

    // --- WRITING ---
    suspend fun createShelf(shelf: Shelf): Int {
        return dao.insertShelf(shelf.toEntity()).toInt()
    }

    suspend fun createItem(item: Item) {
        dao.insertItem(item.toEntity())
    }

    suspend fun deleteItem(item: Item) {
        dao.deleteItem(item.toEntity())
    }

    suspend fun deleteShelf(shelf: Shelf) {
        dao.deleteShelf(shelf.toEntity())
    }

    suspend fun updateItem(item: Item) {
        dao.deleteItem(item.toEntity())
    }

}


private fun ShelfEntity.toDomainModel() = Shelf(
    id = this.id,
    name = this.name,
    storageId = this.storageId
)

private fun Shelf.toEntity() = ShelfEntity(
    id = this.id,
    name = this.name,
    storageId = this.storageId
)



private fun ItemEntity.toDomainModel() = Item(
    id = this.id,
    name = this.name,
    expiryDate = this.expiryDate,
    shelfId = this.shelfId,
    imagePath = this.imagePath
)

private fun Item.toEntity() = ItemEntity(
    id = this.id,
    name = this.name,
    expiryDate = this.expiryDate,
    shelfId = this.shelfId,
    imagePath = this.imagePath
)

private fun ShelfWithItemsRelation.toDomainModel() = ShelfWithItems(
    shelf = this.shelfEntity.toDomainModel(),
    items = this.itemEntities.map { it.toDomainModel() }
)