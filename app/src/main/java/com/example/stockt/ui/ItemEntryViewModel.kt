package com.example.stockt.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.StocktRepository
import com.example.stockt.data.Item
import com.example.stockt.data.Shelf
import com.example.stockt.data.ShelfWithItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemEntryViewModel(private val repository: StocktRepository) : ViewModel() {

    // If null, we are creating a NEW item. If set, we are EDITING.
    private var currentItemId: Int? = null

    var itemName by mutableStateOf("")
    var selectedExpiryDate by mutableStateOf<Long?>(null)
    var selectedShelfId by mutableStateOf<Int?>(null)

    var selectedImagePath by mutableStateOf<String?>(null)

    val availableShelves: StateFlow<List<ShelfWithItems>> =
        repository.getShelvesForStorageUnit(1)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    // --- NEW LOGIC START ---




    fun resetForm() {
        currentItemId = null
        itemName = ""
        selectedExpiryDate = null
        selectedImagePath = null // Reset image
    }

    fun startEditing(item: Item) {
        currentItemId = item.id
        itemName = item.name
        selectedExpiryDate = item.expiryDate
        selectedShelfId = item.shelfId
        selectedImagePath = item.imagePath // Load existing image
    }

    fun saveItem() {
        if (itemName.isNotBlank() && selectedShelfId != null) {
            viewModelScope.launch {
                val item = Item(
                    id = currentItemId ?: 0,
                    name = itemName,
                    expiryDate = selectedExpiryDate ?: System.currentTimeMillis(),
                    shelfId = selectedShelfId!!,
                    imagePath = selectedImagePath // Save the path!
                )

                if (currentItemId == null) {
                    repository.createItem(item)
                } else {
                    repository.updateItem(item)
                }
                resetForm()
            }
        }
    }
    // --- NEW LOGIC END ---

    fun createDefaultShelf() {
        viewModelScope.launch {
            repository.createDefaultFridge()
            repository.createShelf(Shelf(name = "Top Shelf", storageId = 1))
        }
    }
}