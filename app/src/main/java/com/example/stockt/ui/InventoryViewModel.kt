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

    // 1. UI STATE: Holds the list of all locations and their items
    val uiState: StateFlow<InventoryUiState> =
        repository.getShelvesForStorageUnit(1) // Assuming '1' is the main house/inventory
            .map { InventoryUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState()
            )

    // 2. INITIALIZATION: Called from the UI to create defaults if the list is empty
    fun createDefaultCategoriesIfNeeded() {
        viewModelScope.launch {
            // 1. Ask the Database directly: "Do we have shelves?"
            // .first() waits for the database to give the real answer
            val currentList = repository.getShelvesForStorageUnit(1).first()

            // 2. Only create defaults if the DATABASE is truly empty
            if (currentList.isEmpty()) {
                repository.createDefaultFridge()

                repository.createShelf(Shelf(name = "Pantry", storageId = 1))
                repository.createShelf(Shelf(name = "Fridge", storageId = 1))
                repository.createShelf(Shelf(name = "Basement", storageId = 1))
            }
        }
    }

    // 3. CREATE LOCATION: User adds a custom spot (e.g., "Garage")
    fun createShelf(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.createDefaultFridge() // Safety check
            repository.createShelf(
                Shelf(name = name, storageId = 1)
            )
        }
    }

    // 4. DELETE ITEM: Connects the UI trash button to the Database
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun deleteShelf(shelf: Shelf) {
        viewModelScope.launch {
            repository.deleteShelf(shelf)
        }
    }
}

