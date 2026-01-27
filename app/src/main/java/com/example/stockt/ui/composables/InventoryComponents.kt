package com.example.stockt.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stockt.data.Item
import com.example.stockt.data.InventoryWithItems
import com.example.stockt.data.UserPreferences

@Composable
fun InventoryDashboardCard(inventory: InventoryWithItems, userPrefs: UserPreferences?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = inventory.inventory.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = inventory.items.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF6A798C))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (inventory.items.isEmpty()) {
            Text("Empty inventory", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(inventory.items) { item -> ItemTicket(item, userPrefs) }
            }
        }
    }
}

@Composable
fun InventoryDetailView(
    inventory: InventoryWithItems,
    userPrefs: UserPreferences?,
    onDelete: (Item) -> Unit,
    onEdit: (Item) -> Unit
) {
    if (inventory.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This inventory is empty.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(inventory.items) { item ->
                ItemDetailRow(
                    item = item,
                    userPrefs = userPrefs,
                    onDelete = onDelete,
                    onEdit = onEdit
                )
            }
        }
    }
}