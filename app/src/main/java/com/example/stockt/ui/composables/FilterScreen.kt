package com.example.stockt.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.stockt.data.Item
import com.example.stockt.data.ShelfWithItems
import com.example.stockt.data.UserPreferences
import com.example.stockt.ui.composables.ItemDetailRow
import com.example.stockt.ui.getDaysRemaining

// Filter Options
enum class FilterOption(val label: String) {
    ALL("All Items"),
    EXPIRED("Expired"),
    TODAY("Expires Today"),
    SOON("Within 7 Days")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    shelves: List<ShelfWithItems>,
    userPrefs: UserPreferences?,
    onBack: () -> Unit,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit
) {
    // Get all items in a big list
    val allItems = remember(shelves) {
        shelves.flatMap { it.items }
            .sortedBy { it.expiryDate } // Always sort by date (soonest first)
    }

    // Check what filter is selected
    var selectedFilter by remember { mutableStateOf(FilterOption.ALL) }

    // Filter the items based on the selected filter
    val displayedItems = remember(allItems, selectedFilter) {
        when (selectedFilter) {
            FilterOption.ALL -> allItems
            FilterOption.EXPIRED -> allItems.filter { getDaysRemaining(it.expiryDate) < 0 }
            FilterOption.TODAY -> allItems.filter { getDaysRemaining(it.expiryDate) == 0L }
            FilterOption.SOON -> allItems.filter {
                val days = getDaysRemaining(it.expiryDate)
                days in 0..7
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Overview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            // Filter Top Row
            ScrollableTabRow(
                selectedTabIndex = selectedFilter.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = {},
                divider = {}
            ) {
                FilterOption.values().forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        leadingIcon = if (selectedFilter == filter) {
                            { Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }

            // List
            if (displayedItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No items found for this filter.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedItems) { item ->
                        ItemDetailRow(
                            item = item,
                            userPrefs = userPrefs,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}