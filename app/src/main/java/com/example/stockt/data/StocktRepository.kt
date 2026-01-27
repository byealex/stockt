package com.example.stockt.data

import com.example.stockt.db.InventoryEntity
import com.example.stockt.db.ItemEntity
import com.example.stockt.db.InventoryWithItemsRelation
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
    fun getInventoriesForStorageUnit(storageUnitId: Int): Flow<List<InventoryWithItems>> {
        return dao.getInventoriesWithItemsForUnit(storageUnitId).map {
            relationList -> relationList.map { it.toDomainModel() }
        }
    }

    // writing
    suspend fun insertInventory(inventory: Inventory): Int {
        return dao.insertInventory(inventory.toEntity()).toInt()
    }

    suspend fun insertItem(item: Item) {
        dao.insertItem(item.toEntity())
    }

    suspend fun deleteItemById(itemId: Int) {
        dao.deleteItemById(itemId)
    }

    suspend fun deleteInventory(inventory: Inventory) {
        dao.deleteInventory(inventory.toEntity())
    }

    suspend fun updateInventoryName(id: Int, newName: String) {
        dao.updateInventoryName(id, newName)
    }
}


private fun InventoryEntity.toDomainModel() = Inventory(
    id = this.id,
    name = this.name,
    storageId = this.storageId
)

private fun Inventory.toEntity() = InventoryEntity(
    id = this.id,
    name = this.name,
    storageId = this.storageId
)



private fun ItemEntity.toDomainModel() = Item(
    id = this.id,
    name = this.name,
    expiryDate = this.expiryDate,
    inventoryId = this.inventoryId,
    imagePath = this.imagePath,
    analysisTags = this.analysisTags,
    allergenTags = this.allergenTags
)

private fun Item.toEntity() = ItemEntity(
    id = this.id,
    name = this.name,
    expiryDate = this.expiryDate,
    inventoryId = this.inventoryId,
    imagePath = this.imagePath,
    analysisTags = this.analysisTags,
    allergenTags = this.allergenTags
)

private fun InventoryWithItemsRelation.toDomainModel() = InventoryWithItems(
    inventory = this.inventoryEntity.toDomainModel(),
    items = this.itemEntities.map { it.toDomainModel() }
)