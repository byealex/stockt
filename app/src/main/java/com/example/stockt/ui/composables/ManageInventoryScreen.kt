package com.example.stockt.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stockt.data.Inventory // Ensure correct import for your model
import com.example.stockt.data.InventoryWithItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInventoryScreen(
    inventories: List<InventoryWithItems>,
    onBack: () -> Unit,
    onDeleteInventory: (Inventory) -> Unit,
    onSaveInventory: (Int, String) -> Unit
) {
    var showEntryDialog by remember { mutableStateOf(false) }
    var inventoryToEdit by remember { mutableStateOf<Inventory?>(null) }
    var inventoryToDelete by remember { mutableStateOf<Inventory?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manage Inventories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                inventoryToEdit = null
                showEntryDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { innerPadding ->
        if (inventories.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No locations found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(inventories) { inventoryWithItems ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = inventoryWithItems.inventory.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = inventoryWithItems.items.size.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6A798C)
                                )
                            }

                            // Edit Button
                            IconButton(onClick = {
                                inventoryToEdit = inventoryWithItems.inventory
                                showEntryDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Inventory",
                                    tint = Color(0xFF9E9E9E)
                                )
                            }

                            // Delete Button
                            IconButton(onClick = { inventoryToDelete = inventoryWithItems.inventory }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }

        }

        if (showEntryDialog) {
            InventoryEntryDialog(
                initialName = inventoryToEdit?.name ?: "",
                isEditing = inventoryToEdit != null,
                onDismissRequest = { showEntryDialog = false },
                onConfirm = { newName ->
                    val id = inventoryToEdit?.id ?: 0
                    onSaveInventory(id, newName)
                    showEntryDialog = false
                }
            )
        }

        if (inventoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { inventoryToDelete = null },
                title = { Text("Delete Inventory?") },
                text = { Text("Are you sure you want to delete '${inventoryToDelete?.name}'? All items inside will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteInventory(inventoryToDelete!!)
                            inventoryToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { inventoryToDelete = null },
                        ) { Text("Cancel") }
                }
            )
        }
    }
}