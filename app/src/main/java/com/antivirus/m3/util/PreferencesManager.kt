package com.antivirus.m3.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antivirus.m3.data.ProtectionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "antivirus_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        private val KEY_PROTECTION_MODE = intPreferencesKey("protection_mode")
        private val KEY_THANOS_ACTIVATED = booleanPreferencesKey("thanos_activated")
        private val KEY_THANOS_CODE = stringPreferencesKey("thanos_code")
        private val KEY_LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

        private fun longPreferencesKey(name: String) =
            androidx.datastore.preferences.core.longPreferencesKey(name)
    }

    val protectionMode: Flow<ProtectionMode> = context.dataStore.data.map { prefs ->
        ProtectionMode.entries.getOrElse(prefs[KEY_PROTECTION_MODE] ?: 0) { ProtectionMode.OFF }
    }

    val thanosActivated: Flow<Boolean> = context.dataStore.data.map { it[KEY_THANOS_ACTIVATED] ?: false }
    val thanosCode: Flow<String> = context.dataStore.data.map { it[KEY_THANOS_CODE] ?: "" }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: false }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun setProtectionMode(mode: ProtectionMode) {
        context.dataStore.edit { it[KEY_PROTECTION_MODE] = mode.ordinal }
    }

    suspend fun setThanosActivated(activated: Boolean) {
        context.dataStore.edit { it[KEY_THANOS_ACTIVATED] = activated }
    }

    suspend fun setThanosCode(code: String) {
        context.dataStore.edit { it[KEY_THANOS_CODE] = code }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = dark }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLastScanTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_SCAN_TIME] = time }
    }
}
