package com.example.stockt.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.stockt.data.ProductData
import com.example.stockt.data.SafetyStatus
import com.example.stockt.data.SafetyUtils
import com.example.stockt.data.UserPreferences

@Composable
fun ProductPreviewDialog(
    product: ProductData,
    onDismiss: () -> Unit,
    onAddToFridge: () -> Unit,
    userPrefs: UserPreferences?,
) {
    val safetyStatus = if (userPrefs != null) {
        SafetyUtils.checkSafety(product, userPrefs)
    } else SafetyStatus.UNKNOWN

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Product Image
                if (product.image_url != null) {
                    AsyncImage(
                        model = product.image_url,
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Name
                Text(
                    text = product.product_name ?: "Unknown Product",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))
                SafetyCheck(safetyStatus)
                Spacer(modifier = Modifier.height(12.dp))

                val allergens = getAllergensList(product)
                if (allergens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Contains: ${allergens.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Add Button
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAddToFridge) {
                        Text("Add to Inventory")
                    }
                }
            }
        }
    }
}

fun getAllergensList(p: ProductData): List<String> {
    return p.allergens_tags?.map { it.replace("en:", "").replaceFirstChar { char -> char.uppercase() } } ?: emptyList()
}

@Composable
fun SafetyCheck(status: SafetyStatus) {
    val (color, text, icon) = when (status) {
        SafetyStatus.SAFE -> Triple(
            Color(0xFF1AB387),
            "Safe to consume",
            Icons.Default.Check
        )

        SafetyStatus.WARNING -> Triple(
            Color(0xFFEF5350),
            "Dietary conflict detected", // or Dietary restriction alert or Not suitable for you
            Icons.Default.Warning
        )

        SafetyStatus.UNKNOWN -> Triple(
            Color(0xFF9E9E9E),
            "Not enough data to assess safety",
            Icons.Default.Warning
        )
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp).fillMaxWidth(),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}