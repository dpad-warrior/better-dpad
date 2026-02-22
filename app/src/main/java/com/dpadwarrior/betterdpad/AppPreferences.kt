package com.dpadwarrior.betterdpad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object AppPreferences {
    val DEBUG_MODE = booleanPreferencesKey("debug_mode")

    fun isDebugModeEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[DEBUG_MODE] ?: false }

    suspend fun setDebugMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DEBUG_MODE] = enabled }
    }
}
