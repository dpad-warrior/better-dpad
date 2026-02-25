package com.dpadwarrior.betterdpad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class AppPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    private val APP_ENABLED = booleanPreferencesKey("app_enabled")
    private val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    private val JUMP_TO_FIRST_KEY_CODE = intPreferencesKey("jump_to_first_key_code")
    private val JUMP_TO_LAST_KEY_CODE = intPreferencesKey("jump_to_last_key_code")
    private val JUMP_TO_FAB_KEY_CODE = intPreferencesKey("jump_to_fab_key_code")

    private val NO_BINDING = -1

    val isAppEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[APP_ENABLED] ?: true }

    val isDebugModeEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[DEBUG_MODE] ?: false }

    val jumpToFirstKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_FIRST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val jumpToLastKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_LAST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val jumpToFabKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_FAB_KEY_CODE]?.takeIf { it != NO_BINDING } }

    suspend fun setAppEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[APP_ENABLED] = enabled }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DEBUG_MODE] = enabled }
    }

    suspend fun setJumpToFirstKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[JUMP_TO_FIRST_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setJumpToLastKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[JUMP_TO_LAST_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setJumpToFabKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[JUMP_TO_FAB_KEY_CODE] = keyCode ?: NO_BINDING }
    }
}
