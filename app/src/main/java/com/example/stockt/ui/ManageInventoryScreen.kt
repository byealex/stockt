package com.example.stockt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stockt.data.Shelf // Ensure this import matches your Shelf location
import com.example.stockt.data.ShelfWithItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInventoryScreen(
    shelves: List<ShelfWithItems>,
    onBack: () -> Unit,
    onDeleteShelf: (Shelf) -> Unit,
    onEditShelf: (Shelf) -> Unit,
    onAddShelf: () -> Unit
) {
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
            FloatingActionButton(onClick = onAddShelf) {
                Icon(Icons.Default.Add, contentDescription = "Add Inventory")
            }
        }
    ) { innerPadding ->
        if (shelves.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No inventories found.", color = Color.Gray)
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
                            IconButton(onClick = { onEditShelf(shelfWithItems.shelf) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Inventory",
                                    tint = Color(0xFF9E9E9E)
                                )
                            }

                            // 🗑️ DELETE BUTTON
                            IconButton(onClick = { onDeleteShelf(shelfWithItems.shelf) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete Inventory",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}