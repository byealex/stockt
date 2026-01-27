package com.example.stockt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.StocktRepository
import com.example.stockt.data.Item
import com.example.stockt.data.Inventory
import com.example.stockt.data.InventoryWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

// Define the UI State wrapper
data class InventoryUiState(
    val inventories: List<InventoryWithItems> = emptyList()
)

class InventoryViewModel(private val repository: StocktRepository) : ViewModel() {

    // 1. UI STATE
    val uiState: StateFlow<InventoryUiState> =
        repository.getInventoriesForStorageUnit(1)
            .map { inventories ->
                val sortedInventories = inventories.map { inventory ->
                    inventory.copy(
                        items = inventory.items.sortedBy { it.expiryDate } // Sorts: Soonest -> Latest
                    )
                }
                InventoryUiState(sortedInventories)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState()
            )

    // 2. INITIALIZATION
    fun createDefaultCategoriesIfNeeded() {
        viewModelScope.launch {
            val currentList = repository.getInventoriesForStorageUnit(1).first()

            if (currentList.isEmpty()) {
                repository.createDefaultInventory()
                repository.insertInventory(Inventory(name = "Pantry", storageId = 1))
                repository.insertInventory(Inventory(name = "Fridge", storageId = 1))
                repository.insertInventory(Inventory(name = "Basement", storageId = 1))
            }
        }
    }

    // 3. SAVE LOCATION
    fun saveInventory(id: Int, name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            if (id == 0) {
                val inventory = Inventory(name = name, storageId = 1)
                repository.insertInventory(inventory)
            } else {
                repository.updateInventoryName(id, name)
            }
        }
    }

    // 4. DELETE ITEM (Updated to match Repository change)
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItemById(item.id)
        }
    }

    // 5. DELETE INVENTORY
    fun deleteInventory(inventory: Inventory) {
        viewModelScope.launch {
            repository.deleteInventory(inventory)
        }
    }
}