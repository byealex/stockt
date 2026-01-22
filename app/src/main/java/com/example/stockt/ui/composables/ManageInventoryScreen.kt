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
import com.example.stockt.data.Shelf // Ensure correct import for your model
import com.example.stockt.data.ShelfWithItems
import com.example.stockt.ui.composables.ShelfEntryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInventoryScreen(
    shelves: List<ShelfWithItems>,
    onBack: () -> Unit,
    onDeleteShelf: (Shelf) -> Unit,
    onSaveShelf: (Int, String) -> Unit
) {
    var showEntryDialog by remember { mutableStateOf(false) }
    var shelfToEdit by remember { mutableStateOf<Shelf?>(null) }
    var shelfToDelete by remember { mutableStateOf<Shelf?>(null) }

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
                shelfToEdit = null
                showEntryDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { innerPadding ->
        if (shelves.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No locations found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(shelves) { shelfWithItems ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
//                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = shelfWithItems.shelf.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = shelfWithItems.items.size.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6A798C)
                                )
                            }

                            // ✏️ EDIT BUTTON
                            IconButton(onClick = {
                                shelfToEdit = shelfWithItems.shelf
                                showEntryDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Inventory",
                                    tint = Color(0xFF9E9E9E)
                                )
                            }

                            // DELETE BUTTON (Triggers local warning)
                            IconButton(onClick = { shelfToDelete = shelfWithItems.shelf }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }

        }

        if (showEntryDialog) {
            ShelfEntryDialog(
                initialName = shelfToEdit?.name ?: "",
                isEditing = shelfToEdit != null,
                onDismissRequest = { showEntryDialog = false },
                onConfirm = { newName ->
                    val id = shelfToEdit?.id ?: 0
                    onSaveShelf(id, newName)
                    showEntryDialog = false
                }
            )
        }

        if (shelfToDelete != null) {
            AlertDialog(
                onDismissRequest = { shelfToDelete = null },
                title = { Text("Delete Inventory?") },
                text = { Text("Are you sure you want to delete '${shelfToDelete?.name}'? All items inside will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteShelf(shelfToDelete!!)
                            shelfToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { shelfToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}