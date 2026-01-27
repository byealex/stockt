package com.example.stockt.data

data class InventoryWithItems(
    val inventory: Inventory,
    val items: List<Item>
)