package com.example.stockt.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. EXTENSION PROPERTY: This creates the single instance of DataStore
// named "user_settings" on your phone storage.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    // 2. KEYS: These are the unique IDs for each setting
    companion object {
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")

        // DIETS
        val PREF_VEGETARIAN = booleanPreferencesKey("pref_vegetarian")
        val PREF_VEGAN = booleanPreferencesKey("pref_vegan")
        val PREF_PALM_OIL_FREE = booleanPreferencesKey("pref_palm_oil_free")

        // ALLERGENS (True = User wants to AVOID this)
        val AVOID_GLUTEN = booleanPreferencesKey("avoid_gluten")
        val AVOID_MILK = booleanPreferencesKey("avoid_milk")
        val AVOID_EGGS = booleanPreferencesKey("avoid_eggs")
        val AVOID_NUTS = booleanPreferencesKey("avoid_nuts")
        val AVOID_PEANUTS = booleanPreferencesKey("avoid_peanuts")
        val AVOID_SOY = booleanPreferencesKey("avoid_soy")
        val AVOID_FISH = booleanPreferencesKey("avoid_fish")
    }

    // 3. READ DATA: This Flow emits a new UserPreferences object
    // every time something changes on disk.
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isFirstRun = preferences[IS_FIRST_RUN] ?: true, // Default to TRUE for new users
                isVegetarian = preferences[PREF_VEGETARIAN] ?: false,
                isVegan = preferences[PREF_VEGAN] ?: false,
                isPalmOilFree = preferences[PREF_PALM_OIL_FREE] ?: false,
                avoidGluten = preferences[AVOID_GLUTEN] ?: false,
                avoidMilk = preferences[AVOID_MILK] ?: false,
                avoidEggs = preferences[AVOID_EGGS] ?: false,
                avoidNuts = preferences[AVOID_NUTS] ?: false,
                avoidPeanuts = preferences[AVOID_PEANUTS] ?: false,
                avoidSoy = preferences[AVOID_SOY] ?: false,
                avoidFish = preferences[AVOID_FISH] ?: false
            )
        }

    // 4. WRITE DATA: Complete Onboarding (Set First Run = false)
    suspend fun completeOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_RUN] = false
        }
    }

    // 5. WRITE DATA: Update all dietary preferences at once
    suspend fun updatePreferences(newPrefs: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PREF_VEGETARIAN] = newPrefs.isVegetarian
            prefs[PREF_VEGAN] = newPrefs.isVegan
            prefs[PREF_PALM_OIL_FREE] = newPrefs.isPalmOilFree
            prefs[AVOID_GLUTEN] = newPrefs.avoidGluten
            prefs[AVOID_MILK] = newPrefs.avoidMilk
            prefs[AVOID_EGGS] = newPrefs.avoidEggs
            prefs[AVOID_NUTS] = newPrefs.avoidNuts
            prefs[AVOID_PEANUTS] = newPrefs.avoidPeanuts
            prefs[AVOID_SOY] = newPrefs.avoidSoy
            prefs[AVOID_FISH] = newPrefs.avoidFish
        }
    }
}

// 6. MODEL: A simple data class to hold the values in memory
data class UserPreferences(
    val isFirstRun: Boolean = true,
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isPalmOilFree: Boolean = false,
    val avoidGluten: Boolean = false,
    val avoidMilk: Boolean = false,
    val avoidEggs: Boolean = false,
    val avoidNuts: Boolean = false,
    val avoidPeanuts: Boolean = false,
    val avoidSoy: Boolean = false,
    val avoidFish: Boolean = false
)