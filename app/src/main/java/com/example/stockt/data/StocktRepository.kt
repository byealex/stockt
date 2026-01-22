package com.example.stockt.data

import com.example.stockt.db.ItemEntity
import com.example.stockt.db.ShelfEntity
import com.example.stockt.db.ShelfWithItemsRelation
import com.example.stockt.db.StocktDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StocktRepository(private val dao: StocktDao) {

    suspend fun createDefaultInventory() {
        dao.insertStorageUnit(
            com.example.stockt.db.StorageUnitEntity(
                id = 1,
                name = "Personal Inventory"
            )
        )
    }

    // reading
    fun getShelvesForStorageUnit(storageUnitId: Int): Flow<List<ShelfWithItems>> {
        return dao.getShelvesWithItemsForUnit(storageUnitId).map {
            relationList -> relationList.map { it.toDomainModel() }
        }
    }

    // writing
    suspend fun insertShelf(shelf: Shelf): Int {
        return dao.insertShelf(shelf.toEntity()).toInt()
    }

    suspend fun insertItem(item: Item) {
        dao.insertItem(item.toEntity())
    }

    suspend fun deleteItemById(itemId: Int) {
        dao.deleteItemById(itemId)
    }

    suspend fun deleteShelf(shelf: Shelf) {
        dao.deleteShelf(shelf.toEntity())
    }

    suspend fun updateShelfName(id: Int, newName: String) {
        dao.updateShelfName(id, newName)
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
    imagePath = this.imagePath,
    analysisTags = this.analysisTags,
    allergenTags = this.allergenTags
)

private fun Item.toEntity() = ItemEntity(
    id = this.id,
    name = this.name,
    expiryDate = this.expiryDate,
    shelfId = this.shelfId,
    imagePath = this.imagePath,
    analysisTags = this.analysisTags,
    allergenTags = this.allergenTags
)

private fun ShelfWithItemsRelation.toDomainModel() = ShelfWithItems(
    shelf = this.shelfEntity.toDomainModel(),
    items = this.itemEntities.map { it.toDomainModel() }
)