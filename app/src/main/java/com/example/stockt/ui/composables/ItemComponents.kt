package com.example.stockt.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.stockt.data.Item
import com.example.stockt.data.SafetyStatus
import com.example.stockt.data.SafetyUtils
import com.example.stockt.data.UserPreferences
import com.example.stockt.ui.*
import java.io.File

@Composable
fun ItemTicket(item: Item, userPrefs: UserPreferences?) {
    val days = getDaysRemaining(item.expiryDate)
    val baseColor = Color(0xFF1E293B)
    val safetyStatus = if (userPrefs != null) SafetyUtils.checkSafety(item, userPrefs) else SafetyStatus.UNKNOWN

    Card(
        colors = CardDefaults.cardColors(containerColor = baseColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(100.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row {
                if (item.imagePath != null) {
                    Box(modifier = Modifier.padding(12.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)
                        AsyncImage(model = File(item.imagePath), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                    }
                }

                Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 124.dp))
                        Text(text = getFullExpiryText(days), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFADADAD))
                    }
                    if (safetyStatus != SafetyStatus.UNKNOWN && (item.analysisTags != null || item.allergenTags != null)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SafetyBadge(status = safetyStatus)
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxHeight().width(40.dp).background(color = getExpiryColor(item.expiryDate)),
                contentAlignment = Alignment.Center
            ) {
                val txt = when (getExpiryColor(item.expiryDate)) {
                    ColorWarning -> "Soon"
                    ColorExpired -> "Expired"
                    else -> "Fresh"
                }
                Text(text = txt, maxLines = 1, softWrap = false, modifier = Modifier.rotate(90f), style = TextStyle(fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailRow(item: Item, userPrefs: UserPreferences?, onDelete: (Item) -> Unit, onEdit: (Item) -> Unit) {
    val days = getDaysRemaining(item.expiryDate)
    val baseColor = Color(0xFF1E293B)
    val safetyStatus = if (userPrefs != null) SafetyUtils.checkSafety(item, userPrefs) else SafetyStatus.UNKNOWN

    Card(
        colors = CardDefaults.cardColors(containerColor = baseColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(100.dp),
        onClick = { onEdit(item) }
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (item.imagePath != null) {
                    Box(modifier = Modifier.padding(12.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)
                        AsyncImage(model = File(item.imagePath), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                    }
                }

                Column(modifier = Modifier.padding(12.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = getFullExpiryText(days), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFADADAD))
                    }
                    if (safetyStatus != SafetyStatus.UNKNOWN && (item.analysisTags != null || item.allergenTags != null)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SafetyBadge(status = safetyStatus)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxHeight().width(40.dp).background(color = getExpiryColor(item.expiryDate)))
        }
    }
}

@Composable
fun SafetyBadge(status: SafetyStatus) {
    val color = if (status == SafetyStatus.SAFE) Color(0xFF1AB387) else Color(0xFFEF5350)
    val text = if (status == SafetyStatus.SAFE) "Safe" else "Risk"
    val icon = if (status == SafetyStatus.SAFE) Icons.Default.Check else Icons.Default.Warning

    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(999.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}