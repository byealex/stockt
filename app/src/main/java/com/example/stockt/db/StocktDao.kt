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

    // --- 1. Inserting Data ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStorageUnit(storageUnit: StorageUnitEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelfEntity: ShelfEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(itemEntity: ItemEntity): Long


    // --- 2. Reading Data ---
    // (Everything else below is fine)

    @Transaction
    @Query("SELECT * FROM shelves WHERE storageid = :storageId")
    fun getShelvesWithItemsForUnit(storageId: Int): Flow<List<ShelfWithItemsRelation>>

    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Int)

    @Delete
    suspend fun deleteShelf(shelfEntity: ShelfEntity)



    @Update
    suspend fun updateItem(itemEntity: ItemEntity)
}