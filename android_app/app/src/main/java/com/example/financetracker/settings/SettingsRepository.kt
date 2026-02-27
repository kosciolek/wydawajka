package com.example.financetracker.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        private val SERVICE_ACCOUNT_JSON = stringPreferencesKey("service_account_json")
        private val SPEECH_TIMEOUT_SECONDS = intPreferencesKey("speech_timeout_seconds")
        const val DEFAULT_SPEECH_TIMEOUT = 5
    }

    val spreadsheetId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SPREADSHEET_ID] ?: ""
    }

    val serviceAccountJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVICE_ACCOUNT_JSON] ?: ""
    }

    val speechTimeoutSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SPEECH_TIMEOUT_SECONDS] ?: DEFAULT_SPEECH_TIMEOUT
    }

    suspend fun setSpreadsheetId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[SPREADSHEET_ID] = id
        }
    }

    suspend fun setServiceAccountJson(json: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVICE_ACCOUNT_JSON] = json
        }
    }

    suspend fun setSpeechTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[SPEECH_TIMEOUT_SECONDS] = seconds
        }
    }

    fun isConfigured(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[SPREADSHEET_ID].isNullOrBlank() && !prefs[SERVICE_ACCOUNT_JSON].isNullOrBlank()
    }
}
