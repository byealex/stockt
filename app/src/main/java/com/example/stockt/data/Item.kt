package com.example.stockt.data


data class Item(
    val id: Int = 0,
    val name: String,
    val expiryDate : Long,
    val inventoryId: Int,
    val imagePath: String? = null,
    val analysisTags: String? = null,
    val allergenTags: String? = null,
)