package com.example.stockt.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StocktDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStorageUnit(storageUnit: StorageUnitEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventoryEntity: InventoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(itemEntity: ItemEntity): Long

    @Transaction
    @Query("SELECT * FROM inventories WHERE storageid = :storageId")
    fun getInventoriesWithItemsForUnit(storageId: Int): Flow<List<InventoryWithItemsRelation>>

    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)

    @Delete
    suspend fun deleteInventory(inventoryEntity: InventoryEntity)

    @Query("UPDATE inventories SET name = :newName WHERE id = :inventoryId")
    suspend fun updateInventoryName(inventoryId: Int, newName: String)
    @Update
    suspend fun updateItem(itemEntity: ItemEntity)
}