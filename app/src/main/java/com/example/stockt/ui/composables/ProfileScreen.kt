package com.example.stockt.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockt.ui.viewmodels.UserSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isFirstTime: Boolean,
    onFinished: () -> Unit,
    viewModel: UserSettingsViewModel = viewModel()
) {
    val prefsState by viewModel.userPreferences.collectAsState()
    val prefs = prefsState ?: return
    var currentPrefs by remember { mutableStateOf(prefs) }

    LaunchedEffect(prefs) { currentPrefs = prefs }

    Scaffold(
        topBar = {
            if (!isFirstTime) {
                CenterAlignedTopAppBar(
                    title = { Text("Dietary Preferences") },
                    navigationIcon = {
                        IconButton(onClick = onFinished) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
        // 👇 STEP 1: REMOVE the 'bottomBar' block entirely.
        // We are moving the button inside the body instead.

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding) // ✅ This padding now protects the button too!
                .verticalScroll(rememberScrollState())
        ) {
            // 1. ONBOARDING HEADER
            if (isFirstTime) {
                HeaderSection()
            }

            // 2. LIFESTYLE SECTION
            SectionTitle("Lifestyle & Diet")
            SwitchRow("Vegetarian", currentPrefs.isVegetarian) {
                currentPrefs = currentPrefs.copy(isVegetarian = it)
            }
            SwitchRow("Vegan", currentPrefs.isVegan) {
                currentPrefs = currentPrefs.copy(isVegan = it)
            }
            SwitchRow("Palm Oil Free", currentPrefs.isPalmOilFree) {
                currentPrefs = currentPrefs.copy(isPalmOilFree = it)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 3. ALLERGENS SECTION
            SectionTitle("Allergens (I avoid...)")
            SwitchRow("Gluten", currentPrefs.avoidGluten) {
                currentPrefs = currentPrefs.copy(avoidGluten = it)
            }
            SwitchRow("Milk / Lactose", currentPrefs.avoidMilk) {
                currentPrefs = currentPrefs.copy(avoidMilk = it)
            }
            SwitchRow("Eggs", currentPrefs.avoidEggs) {
                currentPrefs = currentPrefs.copy(avoidEggs = it)
            }
            SwitchRow("Nuts (Tree Nuts)", currentPrefs.avoidNuts) {
                currentPrefs = currentPrefs.copy(avoidNuts = it)
            }
            SwitchRow("Peanuts", currentPrefs.avoidPeanuts) {
                currentPrefs = currentPrefs.copy(avoidPeanuts = it)
            }
            SwitchRow("Soy", currentPrefs.avoidSoy) {
                currentPrefs = currentPrefs.copy(avoidSoy = it)
            }
            SwitchRow("Fish / Seafood", currentPrefs.avoidFish) {
                currentPrefs = currentPrefs.copy(avoidFish = it)
            }

            // 👇 STEP 2: PASTE THE BUTTON HERE (Inside the Column)
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.savePreferences(currentPrefs)
                    if (isFirstTime) viewModel.completeOnboarding()
                    onFinished()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
            ) {
                Text(if (isFirstTime) "Get Started" else "Save Changes")
            }

            // Optional: Add a little extra space at the very bottom
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Make whole row clickable
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
//            thumbContent = if (checked) {
//                { Icon(Icons.Default.Check, null, Modifier.size(12.dp)) }
//            } else null
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun HeaderSection() {
    Column(modifier = Modifier.padding(24.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Stockt!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tell us about your dietary needs so we can warn you about unsafe ingredients when you scan items.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}