package com.example.stockt.ui

import android.content.Context
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
import com.example.stockt.data.OpenFoodFactsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import com.example.stockt.data.ProductData

class ItemEntryViewModel(private val repository: StocktRepository) : ViewModel() {

    // If null, we are creating a NEW item. If set, we are EDITING.
    private var currentItemId: Int? = null

    var itemName by mutableStateOf("")

    var isLoading by mutableStateOf(false)

    var scannedProductPreview by mutableStateOf<ProductData?>(null)
    var scanError by mutableStateOf<String?>(null)
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


    fun onScanResult(barcode: String) {
        viewModelScope.launch {
            isLoading = true
            scanError = null
            scannedProductPreview = null // Clear old preview

            try {
                val response = OpenFoodFactsApi.service.getProduct(barcode)
                if (response.product != null) {
                    // SUCCESS: Show the Preview Dialog
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

        // Reset form for clean entry
        resetForm()

        // Pre-fill Name
        itemName = product.product_name ?: "Unknown Product"

        // Download Image
        viewModelScope.launch {
            product.image_url?.let { url ->
                val savedFile = downloadImageToFile(url, context)
                if (savedFile != null) selectedImagePath = savedFile.absolutePath
            }
        }

        // Close preview
        scannedProductPreview = null
    }

    private fun fetchProductDetails(barcode: String, context: Context) {
        viewModelScope.launch {
            isLoading = true
            scanError = null
            try {
                val response = OpenFoodFactsApi.service.getProduct(barcode)
                val product = response.product

                if (product != null) {
                    itemName = product.product_name ?: ""
                    product.image_url?.let { url ->
                        val savedFile = downloadImageToFile(url, context)
                        if (savedFile != null) selectedImagePath = savedFile.absolutePath
                    }
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

    // Helper: Downloads the image from the URL to your app's private storage
    private suspend fun downloadImageToFile(imageUrl: String, context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()

                // Create a file to save it to
                val (file, _) = createImageFile(context) // Reusing your existing helper

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
}