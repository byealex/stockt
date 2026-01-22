package com.example.stockt.ui.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.OpenFoodFactsApi
import com.example.stockt.data.ProductData
import com.example.stockt.data.StocktRepository
import com.example.stockt.data.Item
import com.example.stockt.data.ShelfWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class ItemEntryViewModel(private val repository: StocktRepository) : ViewModel() {

    // Get the Dropdowns for the available inventories
    val availableShelves: StateFlow<List<ShelfWithItems>> =
        repository.getShelvesForStorageUnit(1)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    var currentItemId by mutableStateOf<Int?>(null)

    var itemName by mutableStateOf("")
    var selectedShelfId by mutableStateOf<Int?>(null)
    var selectedExpiryDate by mutableStateOf<Long?>(null)
    var selectedImagePath by mutableStateOf<String?>(null)

    var scannedAnalysisTags by mutableStateOf<String?>(null)
    var scannedAllergenTags by mutableStateOf<String?>(null)

    var isLoading by mutableStateOf(false)
    var scanError by mutableStateOf<String?>(null)
    var scannedProductPreview by mutableStateOf<ProductData?>(null)

    fun startEditing(item: Item) {
        currentItemId = item.id

        itemName = item.name
        selectedShelfId = item.shelfId
        selectedExpiryDate = item.expiryDate
        selectedImagePath = item.imagePath

        scannedAnalysisTags = item.analysisTags
        scannedAllergenTags = item.allergenTags
    }

    fun resetForm() {
        currentItemId = null

        itemName = ""
        selectedExpiryDate = null
        selectedImagePath = null
        scannedAnalysisTags = null
        scannedAllergenTags = null
        scanError = null
        scannedProductPreview = null
    }

    fun saveItem() {
        if (itemName.isNotBlank() && selectedShelfId != null) {
            val itemToSave = Item(
                id = currentItemId ?: 0,

                name = itemName,
                shelfId = selectedShelfId!!,
                expiryDate = selectedExpiryDate ?: System.currentTimeMillis(),
                imagePath = selectedImagePath,
                analysisTags = scannedAnalysisTags,
                allergenTags = scannedAllergenTags
            )

            viewModelScope.launch {
                repository.insertItem(itemToSave)

                resetForm()
            }
        }
    }

    fun onScanResult(barcode: String) {
        viewModelScope.launch {
            isLoading = true
            scanError = null
            scannedProductPreview = null

            try {
                val response = OpenFoodFactsApi.service.getProduct(barcode)
                if (response.product != null) {
                    scannedProductPreview = response.product
                } else {
                    scanError = "Product not found."
                }
            } catch (e: Exception) {
                scanError = "Check Internet Connection."
            } finally {
                isLoading = false
            }
        }
    }

    fun acceptScannedProduct(context: Context) {
        val product = scannedProductPreview ?: return

        val currentShelf = selectedShelfId
        resetForm()
        selectedShelfId = currentShelf

        itemName = product.product_name ?: "Unknown Product"
        scannedAnalysisTags = product.ingredients_analysis_tags?.joinToString(",")
        scannedAllergenTags = product.allergens_tags?.joinToString(",")

        viewModelScope.launch {
            product.image_url?.let { url ->
                val savedFile = downloadImageToFile(url, context)
                if (savedFile != null) selectedImagePath = savedFile.absolutePath
            }
        }
        scannedProductPreview = null
    }

    private suspend fun downloadImageToFile(imageUrl: String, context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()

                val directory = File(context.externalCacheDir, "camera_photos")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, "IMG_${System.currentTimeMillis()}.jpg")

                val input = connection.getInputStream()
                val output = file.outputStream()
                input.copyTo(output)
                output.close()
                input.close()
                file
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteItem() {
        val id = currentItemId ?: return

        viewModelScope.launch {
            repository.deleteItemById(id)
            resetForm()
        }
    }

    fun createDefaultShelf() {
        // Logic to create a shelf if none exist
        viewModelScope.launch {
            repository.createDefaultInventory()
        }
    }
}