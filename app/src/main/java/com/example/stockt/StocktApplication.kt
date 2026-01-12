package com.example.stockt

import android.app.Application
import com.example.stockt.data.StocktRepository
import com.example.stockt.db.StocktDatabase

class StocktApplication : Application(){
    val stocktRepository by lazy {
        val stocktDao = StocktDatabase.getDatabase(this).stocktDao()
        StocktRepository(stocktDao)
    }
}