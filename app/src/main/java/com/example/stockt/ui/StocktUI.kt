package com.example.stockt.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockt.R
import com.example.stockt.data.Item
import com.example.stockt.ui.composables.* // Import your new folder
import com.example.stockt.ui.viewmodels.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// --- SHARED CONSTANTS ---
val ColorExpired = Color(0xFFEF5350)
val ColorWarning = Color(0xFFFFCA28)
val ColorSafe = Color(0xFF66BB6A)

@Composable
fun StocktApp(
    settingsViewModel: UserSettingsViewModel = viewModel()
) {
    val userPrefs by settingsViewModel.userPreferences.collectAsState()

    if (userPrefs == null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (userPrefs!!.isFirstRun) {
        ProfileScreen(isFirstTime = true, onFinished = { })
    } else {
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

    // 1. DEFINE STATES
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) showScanner = true
    }

    // 2. DIALOGS
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
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } }
        )
    }

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

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
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

    // --- NORMAL APP LOGIC ---

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
                    } else {
                        IconButton(onClick = { showProfile = true }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                },
                actions = {
                    if (selectedShelfId == null) {
                        IconButton(onClick = { showFilterScreen = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
//                    IconButton(onClick = { showProfile = true }) {
//                        Icon(Icons.Default.Person, contentDescription = "Profile")
//                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedShelfId != null) {
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
                                modifier = Modifier.fillMaxWidth().clickable { entryViewModel.resetForm(); showItemDialog = true; isFabExpanded = false },
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Add Item", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                                SmallFloatingActionButton(onClick = { entryViewModel.resetForm(); showItemDialog = true; isFabExpanded = false }) { Icon(Icons.Default.ShoppingCart, contentDescription = "Add") }
                            }
                        }

                        if (isFabExpanded) HorizontalDivider(Modifier.width(180.dp).padding(vertical = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        // 2. SCAN BARCODE ROW
                        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            val onScanClick = {
                                val permission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    showScanner = true
                                } else {
                                    cameraPermissionLauncher.launch(permission)
                                }
                                isFabExpanded = false
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onScanClick() },
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Scan Barcode", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                                SmallFloatingActionButton(onClick = { onScanClick() }) { Icon(painter = painterResource(id = R.drawable.ic_barcode_scanner), contentDescription = "Scan") }
                            }
                        }

                        if (isFabExpanded) HorizontalDivider(Modifier.width(180.dp).padding(vertical = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        // 3. ADD ITEM ROW
                        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showManageInventories = true; isFabExpanded = false },
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Manage Inventory", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
                                SmallFloatingActionButton(onClick = { showManageInventories = true; isFabExpanded = false }) { Icon(Icons.Default.Edit, contentDescription = "Inventory") }
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
                if (uiState.shelves.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Shelves! Add one to start.", style = MaterialTheme.typography.headlineSmall)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(44.dp)) {
                        items(uiState.shelves) { shelf ->
                            ShelfDashboardCard(shelf = shelf, userPrefs = userPrefs, onClick = { selectedShelfId = shelf.shelf.id })
                        }
                    }
                }
            } else {
                ShelfDetailView(
                    shelf = selectedShelf!!,
                    userPrefs = userPrefs,
                    onDelete = { item -> itemToDelete = item },
                    onEdit = { item -> entryViewModel.startEditing(item); showItemDialog = true }
                )
            }
        }
    }
}

// ==========================================
//  HELPER FUNCTIONS
// ==========================================

fun createImageFile(context: Context): Pair<File, Uri> {
    val directory = File(context.externalCacheDir, "camera_photos")
    if (!directory.exists()) directory.mkdirs()
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val image = File(directory, "IMG_${timeStamp}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", image)
    return Pair(image, uri)
}

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

fun getFullExpiryText(days: Long): String {
    return when {
        days < 0 -> "Expired ${abs(days)} days ago"
        days == 0L -> "Expires today"
        days == 1L -> "Expires tomorrow"
        else -> "Expires in $days days"
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}