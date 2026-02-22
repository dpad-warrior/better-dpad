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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

object AppPreferences {
    private val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    private val JUMP_TO_FIRST_KEY_CODE = intPreferencesKey("jump_to_first_key_code")
    private val JUMP_TO_LAST_KEY_CODE = intPreferencesKey("jump_to_last_key_code")
    private val JUMP_TO_FAB_KEY_CODE = intPreferencesKey("jump_to_fab_key_code")

    private const val NO_BINDING = -1

    fun isDebugModeEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[DEBUG_MODE] ?: false }

    fun getJumpToFirstKeyCode(context: Context): Flow<Int?> =
        context.dataStore.data.map { prefs -> prefs[JUMP_TO_FIRST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    fun getJumpToLastKeyCode(context: Context): Flow<Int?> =
        context.dataStore.data.map { prefs -> prefs[JUMP_TO_LAST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    fun getJumpToFabKeyCode(context: Context): Flow<Int?> =
        context.dataStore.data.map { prefs -> prefs[JUMP_TO_FAB_KEY_CODE]?.takeIf { it != NO_BINDING } }

    suspend fun setDebugMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DEBUG_MODE] = enabled }
    }

    suspend fun setJumpToFirstKeyCode(context: Context, keyCode: Int?) {
        context.dataStore.edit { prefs -> prefs[JUMP_TO_FIRST_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setJumpToLastKeyCode(context: Context, keyCode: Int?) {
        context.dataStore.edit { prefs -> prefs[JUMP_TO_LAST_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setJumpToFabKeyCode(context: Context, keyCode: Int?) {
        context.dataStore.edit { prefs -> prefs[JUMP_TO_FAB_KEY_CODE] = keyCode ?: NO_BINDING }
    }
}
