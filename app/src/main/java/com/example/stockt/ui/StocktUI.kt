package com.example.stockt.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockt.data.ShelfWithItems
import com.example.stockt.data.Item
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// --- COLORS ---
val ColorExpired = Color(0xFFEF5350)
val ColorWarning = Color(0xFFFFCA28)
val ColorSafe = Color(0xFF66BB6A)

@Composable
fun StocktApp() {
    InventoryScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    entryViewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedShelfId by remember { mutableStateOf<Int?>(null) }
    val selectedShelf = uiState.shelves.find { it.shelf.id == selectedShelfId }

    var isFabExpanded by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showShelfDialog by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (isFabExpanded) 45f else 0f, label = "fab")

    BackHandler(enabled = selectedShelfId != null) { selectedShelfId = null }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selectedShelf?.shelf?.name ?: "My Fridge") },
                navigationIcon = {
                    if (selectedShelfId != null) {
                        IconButton(onClick = { selectedShelfId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedShelfId == null) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                            Text("Add Shelf", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
                            SmallFloatingActionButton(onClick = { showShelfDialog = true; isFabExpanded = false }) {
                                Icon(Icons.Default.Edit, contentDescription = "Add Shelf")
                            }
                        }
                    }
                    AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                            Text("Add Item", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 8.dp))
                            SmallFloatingActionButton(onClick = {
                                entryViewModel.resetForm()
                                showItemDialog = true; isFabExpanded = false
                            }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Add Item")
                            }
                        }
                    }
                    FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }) {
                        Icon(Icons.Default.Add, contentDescription = "Expand", modifier = Modifier.rotate(rotationAngle))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).padding(16.dp)) {
            if (selectedShelf == null) {
                // DASHBOARD
                if (uiState.shelves.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Shelves! Add one to start.", style = MaterialTheme.typography.headlineSmall)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        items(uiState.shelves) { shelf ->
                            ShelfDashboardCard(shelf = shelf, onClick = { selectedShelfId = shelf.shelf.id })
                        }
                    }
                }
            } else {
                // DETAIL
                ShelfDetailView(
                    shelf = selectedShelf!!,
                    onDelete = { item -> viewModel.deleteItem(item) },
                    onEdit = { item ->
                        entryViewModel.startEditing(item)
                        showItemDialog = true
                    }
                )
            }
        }

        if (showItemDialog) ItemEntryDialog(onDismissRequest = { showItemDialog = false })
        if (showShelfDialog) ShelfEntryDialog(onDismissRequest = { showShelfDialog = false }, onConfirm = { viewModel.createShelf(it) })
    }
}

// ==========================================
//  UI COMPONENTS WITH IMAGE SUPPORT
// ==========================================
@Composable
fun ItemTicket(item: Item) {
    val days = getDaysRemaining(item.expiryDate)
    val baseColor = getExpiryColor(item.expiryDate)

    Card(
        colors = CardDefaults.cardColors(containerColor = baseColor.copy(alpha = 0.15f)),
        border = BorderStroke(width = 2.dp, color = baseColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(160.dp).height(85.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (item.imagePath != null) {
                // BOX: Holds the Image + Fallback Icon
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Fallback Icon (Visible if image fails)
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)

                    // 2. The Real Image
                    AsyncImage(
                        model = File(item.imagePath),
                        contentDescription = "Item Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = baseColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = getShortExpiryText(days), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = baseColor)
            }
        }
    }
}
@Composable
fun ItemDetailRow(item: Item, onDelete: (Item) -> Unit, onEdit: (Item) -> Unit) {
    val days = getDaysRemaining(item.expiryDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imagePath != null) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    AsyncImage(
                        model = File(item.imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(getExpiryColor(item.expiryDate)))
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = getFullExpiryText(days), style = MaterialTheme.typography.bodyMedium, color = if (days <= 3) ColorExpired else MaterialTheme.colorScheme.onSurface)
            }

            IconButton(onClick = { onEdit(item) }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
// ==========================================
//  UPDATED ITEM DIALOG (CAMERA LOGIC)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEntryDialog(
    onDismissRequest: () -> Unit,
    viewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val availableShelves by viewModel.availableShelves.collectAsState()

    var isShelfDropdownExpanded by remember { mutableStateOf(false) }
    var selectedShelfName by remember { mutableStateOf("Select a Shelf") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // --- NEW CAMERA LOGIC START ---
    // We track the FILE, not just the URI
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoFile != null) {
            // SUCCESS: Save the ABSOLUTE PATH to the database
            viewModel.selectedImagePath = currentPhotoFile!!.absolutePath
        }
    }
    // --- NEW CAMERA LOGIC END ---

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add Item", style = MaterialTheme.typography.headlineSmall)

                // IMAGE PREVIEW CLICK LISTENER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray)
                        .clickable {
                            // 1. Get both File and Uri
                            val (file, uri) = createImageFile(context)
                            // 2. Remember the file so we can save its path later
                            currentPhotoFile = file
                            // 3. Launch Camera with the Uri
                            cameraLauncher.launch(uri)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.selectedImagePath != null) {
                        // Display the saved path
                        AsyncImage(
                            model = File(viewModel.selectedImagePath!!), // Load from File
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Camera", modifier = Modifier.size(40.dp), tint = Color.Gray)
                            Text("Tap to take photo", color = Color.Gray)
                        }
                    }
                }

                // ... (The rest of your Name/Shelf/Date inputs are unchanged) ...
                OutlinedTextField(value = viewModel.itemName, onValueChange = { viewModel.itemName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())

                // Shelf Dropdown logic (unchanged)...
                if (availableShelves.isEmpty()) {
                    Button(onClick = { viewModel.createDefaultShelf() }, modifier = Modifier.fillMaxWidth()) { Text("Create Default Shelf") }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = selectedShelfName, onValueChange = {}, readOnly = true, label = { Text("Shelf") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }, modifier = Modifier.fillMaxWidth())
                        Box(modifier = Modifier.matchParentSize().clickable { isShelfDropdownExpanded = true })
                        DropdownMenu(expanded = isShelfDropdownExpanded, onDismissRequest = { isShelfDropdownExpanded = false }) {
                            availableShelves.forEach { shelf ->
                                DropdownMenuItem(text = { Text(shelf.shelf.name) }, onClick = { viewModel.selectedShelfId = shelf.shelf.id; selectedShelfName = shelf.shelf.name; isShelfDropdownExpanded = false })
                            }
                        }
                    }
                }

                // Date Picker logic (unchanged)...
                OutlinedTextField(value = viewModel.selectedExpiryDate?.let { convertMillisToDate(it) } ?: "", onValueChange = {}, readOnly = true, label = { Text("Expiry Date") }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } }, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    Button(onClick = { viewModel.saveItem(); onDismissRequest() }, enabled = viewModel.itemName.isNotBlank() && viewModel.selectedShelfId != null) { Text("Save") }
                }
            }
        }
    }

    // (Date Picker Dialog unchanged)
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { viewModel.selectedExpiryDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) { DatePicker(state = datePickerState) }
    }
}
// HELPER: Creates a temporary file in the app's cache to store the photo
// Replace the existing createImageFile function
fun createImageFile(context: Context): Pair<File, Uri> {
    // 1. Create a specific folder inside the cache so we know where it is
    val directory = File(context.externalCacheDir, "camera_photos")
    if (!directory.exists()) {
        directory.mkdirs()
    }

    // 2. Create the file there
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val image = File(directory, "IMG_${timeStamp}.jpg")

    // 3. Get the URI
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        image
    )

    return Pair(image, uri)
}
// ==========================================
//  HELPER LOGIC (Unchanged)
// ==========================================
// (Keep your existing ShelfDashboardCard, ShelfDetailView, Helper functions like getDaysRemaining, etc. from previous response.
// Just make sure to use the UPDATED ItemTicket and ItemDetailRow defined above)

// Re-including ShelfDashboardCard for completeness as it calls ItemTicket
@Composable
fun ShelfDashboardCard(shelf: ShelfWithItems, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = shelf.shelf.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, contentDescription = "Open", tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (shelf.items.isEmpty()) {
            Text("Empty shelf", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(shelf.items) { item -> ItemTicket(item) }
            }
        }
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}
// Include your other logic helpers (getDaysRemaining, getExpiryColor, etc.) here...
// ...
fun getDaysRemaining(expiryDate: Long): Long {
    val diff = expiryDate - System.currentTimeMillis()
    return TimeUnit.MILLISECONDS.toDays(diff)
}
fun getExpiryColor(expiryDate: Long): Color {
    val days = getDaysRemaining(expiryDate)
    return when {
        days <= 3 -> ColorExpired
        days <= 7 -> ColorWarning
        else -> ColorSafe
    }
}
fun getShortExpiryText(days: Long): String {
    return when {
        days < 0 -> "Expired!"
        days == 0L -> "Today"
        days == 1L -> "1 day"
        else -> "$days days"
    }
}
fun getFullExpiryText(days: Long): String {
    return when {
        days < 0 -> "Expired ${abs(days)} days ago"
        days == 0L -> "Expires today"
        days == 1L -> "Expires tomorrow"
        else -> "Expires in $days days"
    }
}
@Composable
fun ShelfEntryDialog(onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    var shelfName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add New Shelf", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = shelfName, onValueChange = { shelfName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    Button(onClick = { onConfirm(shelfName); onDismissRequest() }, enabled = shelfName.isNotBlank()) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun ShelfDetailView(
    shelf: ShelfWithItems,
    onDelete: (Item) -> Unit,
    onEdit: (Item) -> Unit
) {
    if (shelf.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This shelf is empty.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shelf.items) { item ->
                // This calls the ItemDetailRow we created in the previous step
                ItemDetailRow(
                    item = item,
                    onDelete = onDelete,
                    onEdit = onEdit
                )
            }
        }
    }
}
fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}