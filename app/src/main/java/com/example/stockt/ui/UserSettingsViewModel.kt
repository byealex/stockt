package com.example.stockt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.UserPreferences
import com.example.stockt.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Initialize the repository
    private val repository = UserPreferencesRepository(application)

    // Expose the preferences as a StateFlow for the UI
    // It starts as null (loading) and then updates whenever data changes on disk
    val userPreferences = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // Mark onboarding as done so the user goes straight to Inventory next time
    fun completeOnboarding() {
        viewModelScope.launch {
            repository.completeOnboarding()
        }
    }

    // Save the entire preferences object
    fun savePreferences(newPrefs: UserPreferences) {
        viewModelScope.launch {
            repository.updatePreferences(newPrefs)
        }
    }
}