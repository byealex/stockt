package com.example.stockt.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class ProductResponse(val product: ProductData?)

data class ProductData(
    val product_name: String?,
    val image_url: String?,
    val ingredients_analysis_tags: List<String>?,
    val allergens_tags: List<String>?,
    val labels_tags: List<String>?
)

interface OpenFoodFactsService {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

object OpenFoodFactsApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenFoodFactsService = retrofit.create(OpenFoodFactsService::class.java)
}