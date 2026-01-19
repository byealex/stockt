package com.example.stockt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.StocktRepository
import com.example.stockt.data.Item
import com.example.stockt.data.Shelf
import com.example.stockt.data.ShelfWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

// Define the UI State wrapper
data class InventoryUiState(
    val shelves: List<ShelfWithItems> = emptyList()
)

class InventoryViewModel(private val repository: StocktRepository) : ViewModel() {

    // 1. UI STATE
    val uiState: StateFlow<InventoryUiState> =
        repository.getShelvesForStorageUnit(1)
            .map { InventoryUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState()
            )

    // 2. INITIALIZATION
    fun createDefaultCategoriesIfNeeded() {
        viewModelScope.launch {
            val currentList = repository.getShelvesForStorageUnit(1).first()

            if (currentList.isEmpty()) {
                repository.createDefaultFridge()
                repository.insertShelf(Shelf(name = "Pantry", storageId = 1))
                repository.insertShelf(Shelf(name = "Fridge", storageId = 1))
                repository.insertShelf(Shelf(name = "Basement", storageId = 1))
            }
        }
    }

    // 3. SAVE LOCATION
    fun saveShelf(id: Int, name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.createDefaultFridge()

            val shelfToSave = Shelf(
                id = id,
                name = name,
                storageId = 1
            )
            repository.insertShelf(shelfToSave)
        }
    }

    // 4. DELETE ITEM (Updated to match Repository change)
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
        }
    }

    // 5. DELETE SHELF
    fun deleteShelf(shelf: Shelf) {
        viewModelScope.launch {
            repository.deleteShelf(shelf)
        }
    }
}