package com.example.stockt.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// 1. UPDATED DATA STRUCTURE
data class ProductResponse(val product: ProductData?)

data class ProductData(
    val product_name: String?,
    val image_url: String?,
    // New fields for dietary info:
    val ingredients_analysis_tags: List<String>?, // e.g., ["en:vegetarian", "en:palm-oil-free"]
    val allergens_tags: List<String>?,            // e.g., ["en:milk", "en:gluten"]
    val labels_tags: List<String>?                // e.g., ["en:gluten-free"]
)

// 2. INTERFACE (Unchanged)
interface OpenFoodFactsService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

// 3. OBJECT (Unchanged)
object OpenFoodFactsApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenFoodFactsService = retrofit.create(OpenFoodFactsService::class.java)
}