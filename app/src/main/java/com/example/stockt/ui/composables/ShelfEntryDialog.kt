package com.example.stockt.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ShelfEntryDialog(
    initialName: String = "",
    isEditing: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var shelfName by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (isEditing) "Edit Inventory" else "Add New Inventory",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = shelfName,
                    onValueChange = { shelfName = it },
                    label = { Text("Name (e.g., Garage, Office)") }
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    Button(onClick = { onConfirm(shelfName) }, enabled = shelfName.isNotBlank()) {
                        Text(if (isEditing) "Save" else "Add")
                    }
                }
            }
        }
    }
}