package com.pablogold.callblocker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "call_blocker_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        val KEY_ACTION_MODE = stringPreferencesKey("action_mode")
        val KEY_SKIP_CALL_LOG = booleanPreferencesKey("skip_call_log")

        // Chaves do Modo de Emergência
        val KEY_EMERGENCY_BYPASS_ENABLED = booleanPreferencesKey("emergency_bypass_enabled")
        val KEY_EMERGENCY_ATTEMPTS = intPreferencesKey("emergency_attempts")
        val KEY_EMERGENCY_WINDOW_MINUTES = intPreferencesKey("emergency_window_minutes")
    }

    val isBlockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCKING_ENABLED] ?: true
    }

    val actionMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTION_MODE] ?: "REJECT"
    }

    val skipCallLog: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SKIP_CALL_LOG] ?: false
    }

    // Leitura das configurações de emergência
    val isEmergencyBypassEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_EMERGENCY_BYPASS_ENABLED] ?: true // Ativo por padrão
    }

    val emergencyAttempts: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_EMERGENCY_ATTEMPTS] ?: 2 // Padrão: 2 chamadas anteriores
    }

    val emergencyWindowMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_EMERGENCY_WINDOW_MINUTES] ?: 3 // Padrão: 3 minutos
    }

    suspend fun setBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLOCKING_ENABLED] = enabled }
    }

    suspend fun setActionMode(mode: String) {
        context.dataStore.edit { it[KEY_ACTION_MODE] = mode }
    }

    suspend fun setSkipCallLog(skip: Boolean) {
        context.dataStore.edit { it[KEY_SKIP_CALL_LOG] = skip }
    }

    suspend fun setEmergencyBypassEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EMERGENCY_BYPASS_ENABLED] = enabled }
    }

    suspend fun setEmergencyAttempts(attempts: Int) {
        context.dataStore.edit { it[KEY_EMERGENCY_ATTEMPTS] = attempts.coerceAtLeast(1) }
    }

    suspend fun setEmergencyWindowMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_EMERGENCY_WINDOW_MINUTES] = minutes.coerceAtLeast(1) }
    }
}