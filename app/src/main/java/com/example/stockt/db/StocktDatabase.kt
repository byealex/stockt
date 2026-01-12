package com.example.stockt.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ItemEntity::class, ShelfEntity::class, StorageUnitEntity::class],
    version = 1
)

abstract class StocktDatabase : RoomDatabase() {
    abstract fun stocktDao(): StocktDao

    companion object {
        @Volatile
        private var Instance: StocktDatabase? = null

        fun getDatabase(context: Context): StocktDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room
                    .databaseBuilder(context, StocktDatabase::class.java, "stockt_database")
                    .build()

                Instance = instance
                return instance
            }
        }
    }
}
