package com.example.stockt.ui.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stockt.ui.convertMillisToDate
import com.example.stockt.ui.createImageFile
import com.example.stockt.ui.util.dashedBorder
import com.example.stockt.ui.viewmodels.AppViewModelProvider
import com.example.stockt.ui.viewmodels.ItemEntryViewModel
import java.io.File

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

    // Camera Logic
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoFile != null) {
            viewModel.selectedImagePath = currentPhotoFile!!.absolutePath
        }
    }

    LaunchedEffect(viewModel.selectedShelfId, availableShelves) {
        if (viewModel.selectedShelfId != null) {
            val shelf = availableShelves.find { it.shelf.id == viewModel.selectedShelfId }
            if (shelf != null) selectedShelfName = shelf.shelf.name
        }
    }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White
        )) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching product info...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Add Item", style = MaterialTheme.typography.headlineSmall)

                    // Camera / Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .then(
                                if (viewModel.selectedImagePath == null) {
                                    Modifier.dashedBorder(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.outline, cornerRadius = 12.dp)
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val (file, uri) = createImageFile(context)
                                currentPhotoFile = file
                                cameraLauncher.launch(uri)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.selectedImagePath != null) {
                            AsyncImage(
                                model = File(viewModel.selectedImagePath!!),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.AddAPhoto, contentDescription = "Camera", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.outline)
                                Text("Tap to take photo", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    if (viewModel.scanError != null) {
                        Text(text = viewModel.scanError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    OutlinedTextField(
                        value = viewModel.itemName,
                        onValueChange = { viewModel.itemName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Shelf Selection
                    if (availableShelves.isEmpty()) {
                        Button(
                            onClick = { viewModel.createDefaultShelf() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) { Text("Create Default Shelf") }
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
                            Box(modifier = Modifier.matchParentSize().clickable { isShelfDropdownExpanded = true })

                            DropdownMenu(expanded = isShelfDropdownExpanded, onDismissRequest = { isShelfDropdownExpanded = false }) {
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

                    OutlinedTextField(
                        value = viewModel.selectedExpiryDate?.let { convertMillisToDate(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expiry Date") },
                        trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, null) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = if(viewModel.currentItemId != null) Arrangement.SpaceBetween else Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        if(viewModel.currentItemId != null) {
                            TextButton(onClick = { onDelete?.invoke(); onDismissRequest() }) { Text("Delete", color = Color.Red) }
                        }
                        Row {
                            TextButton(onClick = onDismissRequest) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.saveItem(); onDismissRequest() }, enabled = viewModel.itemName.isNotBlank() && viewModel.selectedShelfId != null) { Text("Save") }
                        }
                    }
                }
            }
        }
    }

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