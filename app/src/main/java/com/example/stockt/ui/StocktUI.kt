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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockt.data.ShelfWithItems
import com.example.stockt.data.Item
import com.example.stockt.R
import com.example.stockt.data.SafetyStatus
import com.example.stockt.data.SafetyUtils
import com.example.stockt.data.UserPreferences
import com.example.stockt.ui.util.dashedBorder
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
fun StocktApp(
    // Inject the new ViewModel here
    settingsViewModel: UserSettingsViewModel = viewModel()
) {
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    // 1. Loading State (Wait for DataStore to read disk)
    if (userPrefs == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    // 2. Onboarding State (First Run)
    else if (userPrefs!!.isFirstRun) {
        ProfileScreen(
            isFirstTime = true,
            onFinished = { /* State updates automatically via Flow */ }
        )
    }
    // 3. Main App State
    else {
        InventoryScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    entryViewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val settingsViewModel: UserSettingsViewModel = viewModel()
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    // 1. DEFINE STATES AT THE TOP
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    // 2. DEFINE DIALOGS IMMEDIATELY (So they are always active)

    // ✅ ITEM DELETE DIALOG
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete '${itemToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(itemToDelete!!)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // ✅ LOADING SPINNER
    if (entryViewModel.isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching Product...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // ✅ SCAN ERROR
    if (entryViewModel.scanError != null) {
        AlertDialog(
            onDismissRequest = { entryViewModel.scanError = null },
            title = { Text("Scan Failed") },
            text = { Text(entryViewModel.scanError!!) },
            confirmButton = { TextButton(onClick = { entryViewModel.scanError = null }) { Text("OK") } }
        )
    }

    if (showItemDialog) {
        ItemEntryDialog(
            onDismissRequest = { showItemDialog = false },
            onDelete = {
                val itemId = entryViewModel.currentItemId
                if (itemId != null) {
                    val item = uiState.shelves.flatMap { it.items }.find { it.id == itemId }
                    itemToDelete = item
                }
            }
        )
    }

    // ✅ SCANNER
    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                BarcodeScanner(onBarcodeFound = { barcode ->
                    showScanner = false
                    entryViewModel.onScanResult(barcode)
                })
                IconButton(onClick = { showScanner = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }
        }
    }

    // ✅ PRODUCT PREVIEW
    if (entryViewModel.scannedProductPreview != null) {
        ProductPreviewDialog(
            product = entryViewModel.scannedProductPreview!!,
            onDismiss = { entryViewModel.scannedProductPreview = null },
            userPrefs = userPrefs,
            onAddToFridge = {
                entryViewModel.acceptScannedProduct(context)
                showItemDialog = true
            }
        )
    }


    // --- NORMAL APP LOGIC STARTS HERE ---

    LaunchedEffect(Unit) {
        viewModel.createDefaultCategoriesIfNeeded()
    }

    var selectedShelfId by remember { mutableStateOf<Int?>(null) }
    var showManageInventories by remember { mutableStateOf(false) }
    var showFilterScreen by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    val selectedShelf = uiState.shelves.find { it.shelf.id == selectedShelfId }
    var isFabExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(if (isFabExpanded) 45f else 0f, label = "fab")

    BackHandler(enabled = selectedShelfId != null || showManageInventories || showProfile || showFilterScreen) {
        when {
            selectedShelfId != null -> selectedShelfId = null
            showManageInventories -> showManageInventories = false
            showFilterScreen -> showFilterScreen = false
            showProfile -> showProfile = false
        }
    }

    // --- SCREEN SWITCHING ---

    if (showProfile) {
        ProfileScreen(isFirstTime = false, onFinished = { showProfile = false })
        return
    }

    if (showManageInventories) {
        ManageInventoryScreen(
            shelves = uiState.shelves,
            onBack = { showManageInventories = false },
            onDeleteShelf = { shelf -> viewModel.deleteShelf(shelf) },
            onSaveShelf = { id, name -> viewModel.saveShelf(id, name) }
        )
        return
    }

    if (showFilterScreen) {
        FilterScreen(
            shelves = uiState.shelves,
            userPrefs = userPrefs,
            onBack = { showFilterScreen = false },
            onEdit = { item ->
                entryViewModel.startEditing(item)
                showItemDialog = true
            },
            // ✅ LINKING FILTER DELETE TO DIALOG
            onDelete = { item -> itemToDelete = item }
        )
        return
    }

    // --- MAIN SCAFFOLD ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(selectedShelf?.shelf?.name ?: "My Inventory") },
                navigationIcon = {
                    if (selectedShelfId != null) {
                        IconButton(onClick = { selectedShelfId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedShelfId == null) {
                        IconButton(onClick = { showFilterScreen = true }) {
                            Icon(Icons.Default.List, contentDescription = "Overview & Filter")
                        }
                    }
                    IconButton(onClick = { showProfile = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedShelfId != null) {
                // INSIDE SHELF FAB
                FloatingActionButton(
                    onClick = {
                        entryViewModel.resetForm()
                        entryViewModel.selectedShelfId = selectedShelfId
                        showItemDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            } else {
                // DASHBOARD EXPANDABLE FAB
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .background(
                                color = if (isFabExpanded) Color(0xFF1F2F4A) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End,
                    ) {

                        // 1. MANAGE INVENTORY ROW
                        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showManageInventories = true
                                        isFabExpanded = false
                                    }
                                    .padding(vertical = 4.dp), // Add touch padding
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Manage Inventory",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = { showManageInventories = true; isFabExpanded = false }
                                ) { Icon(Icons.Default.Edit, contentDescription = "Inventory") }
                            }
                        }

                        if (isFabExpanded) Spacer(modifier = Modifier.height(8.dp))

                        // 2. SCAN BARCODE ROW
                        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showScanner = true
                                        isFabExpanded = false
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scan Barcode",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = { showScanner = true; isFabExpanded = false }
                                ) { Icon(painter = painterResource(id = R.drawable.ic_barcode_scanner), contentDescription = "Scan") }
                            }
                        }

                        if (isFabExpanded) Spacer(modifier = Modifier.height(8.dp))

                        // 3. ADD ITEM ROW
                        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        entryViewModel.resetForm()
                                        showItemDialog = true
                                        isFabExpanded = false
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add Item",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = {
                                        entryViewModel.resetForm()
                                        showItemDialog = true
                                        isFabExpanded = false
                                    }
                                ) { Icon(Icons.Default.ShoppingCart, contentDescription = "Add") }
                            }
                        }
                    }

                    // Main + Button
                    FloatingActionButton(onClick = { isFabExpanded = !isFabExpanded }) {
                        Icon(Icons.Default.Add, contentDescription = "Expand", modifier = Modifier.rotate(rotationAngle))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).padding(16.dp)) {
            if (selectedShelf == null) {
                if (uiState.shelves.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Shelves! Add one to start.", style = MaterialTheme.typography.headlineSmall)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(44.dp)) {
                        items(uiState.shelves) { shelf ->
                            ShelfDashboardCard(
                                shelf = shelf,
                                userPrefs = userPrefs,
                                onClick = { selectedShelfId = shelf.shelf.id }
                            )
                        }
                    }
                }
            } else {
                // DETAIL VIEW
                ShelfDetailView(
                    shelf = selectedShelf!!,
                    userPrefs = userPrefs,

                    onDelete = { item -> itemToDelete = item },

                    onEdit = { item ->
                        entryViewModel.startEditing(item)
                        showItemDialog = true
                    }
                )
            }
        }
    }
}
// ==========================================
//  UI COMPONENTS WITH IMAGE SUPPORT
// ==========================================
@Composable
fun ItemTicket(item: Item, userPrefs: UserPreferences?) {
    val days = getDaysRemaining(item.expiryDate)
//    val baseColor = getExpiryColor(item.expiryDate)
    val baseColor = Color(0xFF1E293B)
    val safetyStatus = if (userPrefs != null) {
        SafetyUtils.checkSafety(item, userPrefs)
    } else SafetyStatus.UNKNOWN

    Card(
        colors = CardDefaults.cardColors(containerColor = baseColor),
//        border = BorderStroke(width = 2.dp, color = baseColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(100.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row {
                if (item.imagePath != null) {
                    // BOX: Holds the Image + Fallback Icon
                    Box(
                        modifier = Modifier
                            //                        .fillMaxSize()
                            //                        .clip(RoundedCornerShape(8.dp))
//                            .background(color = Color.Red)
                            .padding(12.dp)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Fallback Icon (Visible if image fails)
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)

                        // 2. The Real Image
                        AsyncImage(
                            model = File(item.imagePath),
                            contentDescription = "Item Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(12.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column (
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
//                           TODO: decide whether to cap the max width
                            modifier = Modifier.widthIn(max = 124.dp)
                        )
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
fun ItemDetailRow(item: Item, userPrefs: UserPreferences?, onDelete: (Item) -> Unit, onEdit: (Item) -> Unit) {
    val days = getDaysRemaining(item.expiryDate)
    val baseColor = Color(0xFF1E293B)

    val safetyStatus = if (userPrefs != null) {
        SafetyUtils.checkSafety(item, userPrefs)
    } else SafetyStatus.UNKNOWN

    Card(
        colors = CardDefaults.cardColors(containerColor = baseColor),
//        border = BorderStroke(width = 2.dp, color = baseColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(100.dp),
        onClick = { onEdit(item) }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
//            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (item.imagePath != null) {
                    // BOX: Holds the Image + Fallback Icon
                    Box(
                        modifier = Modifier
    //                        .fillMaxSize()
    //                        .clip(RoundedCornerShape(8.dp))
//                            .background(color = Color.Red)
                            .padding(12.dp)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Fallback Icon (Visible if image fails)
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)

                        // 2. The Real Image
                        AsyncImage(
                            model = File(item.imagePath),
                            contentDescription = "Item Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(12.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column (
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = getFullExpiryText(days),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFADADAD)
                        )
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
// ==========================================
//  UPDATED ITEM DIALOG (CAMERA LOGIC)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEntryDialog(
    onDismissRequest: () -> Unit,
    viewModel: ItemEntryViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onDelete: (() -> Unit)? = null
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

    // Auto-select shelf name if editing an existing item
    LaunchedEffect(viewModel.selectedShelfId) {
        if (viewModel.selectedShelfId != null) {
            val shelf = availableShelves.find { it.shelf.id == viewModel.selectedShelfId }
            if (shelf != null) selectedShelfName = shelf.shelf.name
        }
    }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        )) {

            // 1. LOADING STATE (If fetching from API)
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching product info...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                // 2. NORMAL FORM
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add Item", style = MaterialTheme.typography.headlineSmall)

                    // --- CAMERA / IMAGE PREVIEW ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
//                            .border(width = 2.dp, brush = , shape = RoundedCornerShape(12.dp))
                            .then(
                                // Only add dashed border if there's no image
                                if (viewModel.selectedImagePath == null) {
                                    Modifier.dashedBorder(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        cornerRadius = 12.dp
                                    )
                                } else {
                                    // No border
                                    Modifier
                                }
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                // Create file & Launch Camera
                                val (file, uri) = createImageFile(context)
                                currentPhotoFile = file
                                cameraLauncher.launch(uri)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.selectedImagePath != null) {
                            // Show the selected image
                            AsyncImage(
                                model = File(viewModel.selectedImagePath!!),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Show "Tap to take photo" placeholder
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                Icon(Icons.Outlined.AddAPhoto, contentDescription = "Camera", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                                Text("Tap to take photo", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // --- ERROR MESSAGE (If Scan Failed) ---
                    if (viewModel.scanError != null) {
                        Text(
                            text = viewModel.scanError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    // --- NAME INPUT ---
                    OutlinedTextField(
                        value = viewModel.itemName,
                        onValueChange = { viewModel.itemName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // --- SHELF SELECTION ---
                    if (availableShelves.isEmpty()) {
                        Button(
                            onClick = { viewModel.createDefaultShelf() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Create Default Shelf")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedShelfName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Shelf") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Invisible box to catch clicks
                            Box(modifier = Modifier.matchParentSize().clickable { isShelfDropdownExpanded = true })

                            DropdownMenu(
                                expanded = isShelfDropdownExpanded,
                                onDismissRequest = { isShelfDropdownExpanded = false }
                            ) {
                                availableShelves.forEach { shelf ->
                                    DropdownMenuItem(
                                        text = { Text(shelf.shelf.name) },
                                        onClick = {
                                            viewModel.selectedShelfId = shelf.shelf.id
                                            selectedShelfName = shelf.shelf.name
                                            isShelfDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // --- DATE PICKER ---
                    OutlinedTextField(
                        value = viewModel.selectedExpiryDate?.let { convertMillisToDate(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expiry Date") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // --- ACTION BUTTONS ---
                    Row(horizontalArrangement = if(viewModel.currentItemId != null) Arrangement.SpaceBetween else Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        if(viewModel.currentItemId != null) {
                            TextButton(onClick = {
                                onDelete?.invoke()
                                onDismissRequest()
                            }) {
                                Text("Delete", color = Color.Red)
                            }
                        }
                        Row {
                            TextButton(onClick = onDismissRequest) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.saveItem()
                                    onDismissRequest()
                                },
                                enabled = viewModel.itemName.isNotBlank() && viewModel.selectedShelfId != null
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Picker Pop-up
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectedExpiryDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// --- HELPER FUNCTION FOR CAMERA ---
fun createImageFile(context: Context): Pair<File, Uri> {
    val directory = File(context.externalCacheDir, "camera_photos")
    if (!directory.exists()) {
        directory.mkdirs()
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val image = File(directory, "IMG_${timeStamp}.jpg")

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
fun ShelfDashboardCard(shelf: ShelfWithItems, userPrefs: UserPreferences?, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = shelf.shelf.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = shelf.items.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFF6A798C))
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (shelf.items.isEmpty()) {
            Text("Empty shelf", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(shelf.items) { item -> ItemTicket(item, userPrefs) }
            }
        }
//        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
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
        days < 0 -> ColorExpired
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
fun ShelfEntryDialog(
    initialName: String = "",
    isEditing: Boolean = false, // 👈 New parameter
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var shelfName by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 1. DYNAMIC TITLE
                Text(
                    text = if (isEditing) "Edit Inventory" else "Add New Inventory",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = shelfName,
                    onValueChange = { shelfName = it },
                    label = { Text("Name (e.g., Garage, Office)") },
//                    modifier = Modifier.fillMaxWidth().focusRequester()
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }

                    // 2. DYNAMIC BUTTON TEXT
                    Button(
                        onClick = { onConfirm(shelfName) },
                        enabled = shelfName.isNotBlank()
                    ) {
                        Text(if (isEditing) "Save" else "Add")
                    }
                }
            }
        }
    }
}
@Composable
fun SafetyBadge(status: SafetyStatus) {
    val color = if (status == SafetyStatus.SAFE) Color(0xFF1AB387) else Color(0xFFEF5350)
    val text = if (status == SafetyStatus.SAFE) "Safe" else "Risk"
    val icon = if (status == SafetyStatus.SAFE) Icons.Default.Check else Icons.Default.Warning

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(999.dp),
//        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ShelfDetailView(
    shelf: ShelfWithItems,
    userPrefs: UserPreferences?,
    onDelete: (Item) -> Unit,
    onEdit: (Item) -> Unit
) {

    if (shelf.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This shelf is empty.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shelf.items) { item ->
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

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}