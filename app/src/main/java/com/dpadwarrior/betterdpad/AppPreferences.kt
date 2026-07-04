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
    private val FOCUS_HIGHLIGHT_ENABLED = booleanPreferencesKey("focus_highlight_enabled")
    private val JUMP_TO_FIRST_KEY_CODE = intPreferencesKey("jump_to_first_key_code")
    private val JUMP_TO_LAST_KEY_CODE = intPreferencesKey("jump_to_last_key_code")
    private val JUMP_TO_FAB_KEY_CODE = intPreferencesKey("jump_to_fab_key_code")
    private val DPAD_UP_KEY_CODE = intPreferencesKey("dpad_up_key_code")
    private val DPAD_DOWN_KEY_CODE = intPreferencesKey("dpad_down_key_code")
    private val DPAD_LEFT_KEY_CODE = intPreferencesKey("dpad_left_key_code")
    private val DPAD_RIGHT_KEY_CODE = intPreferencesKey("dpad_right_key_code")
    private val DPAD_SELECT_KEY_CODE = intPreferencesKey("dpad_select_key_code")
    private val INPUT_MODE_MODIFIER_KEY_CODE = intPreferencesKey("input_mode_modifier_key_code")

    private val NO_BINDING = -1

    val isAppEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[APP_ENABLED] ?: true }

    val isDebugModeEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[DEBUG_MODE] ?: false }

    val isFocusHighlightEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[FOCUS_HIGHLIGHT_ENABLED] ?: false }

    val jumpToFirstKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_FIRST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val jumpToLastKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_LAST_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val jumpToFabKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[JUMP_TO_FAB_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val dpadUpKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[DPAD_UP_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val dpadDownKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[DPAD_DOWN_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val dpadLeftKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[DPAD_LEFT_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val dpadRightKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[DPAD_RIGHT_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val dpadSelectKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[DPAD_SELECT_KEY_CODE]?.takeIf { it != NO_BINDING } }

    val inputModeModifierKeyCode: Flow<Int?> =
        dataStore.data.map { prefs -> prefs[INPUT_MODE_MODIFIER_KEY_CODE]?.takeIf { it != NO_BINDING } }

    suspend fun setAppEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[APP_ENABLED] = enabled }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DEBUG_MODE] = enabled }
    }

    suspend fun setFocusHighlightEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[FOCUS_HIGHLIGHT_ENABLED] = enabled }
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

    suspend fun setDpadUpKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[DPAD_UP_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setDpadDownKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[DPAD_DOWN_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setDpadLeftKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[DPAD_LEFT_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setDpadRightKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[DPAD_RIGHT_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setDpadSelectKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[DPAD_SELECT_KEY_CODE] = keyCode ?: NO_BINDING }
    }

    suspend fun setInputModeModifierKeyCode(keyCode: Int?) {
        dataStore.edit { prefs -> prefs[INPUT_MODE_MODIFIER_KEY_CODE] = keyCode ?: NO_BINDING }
    }
}
