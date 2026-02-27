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
        private val WORKER_URL = stringPreferencesKey("worker_url")
        private val API_TOKEN = stringPreferencesKey("api_token")
        private val SPEECH_TIMEOUT_SECONDS = intPreferencesKey("speech_timeout_seconds")
        const val DEFAULT_SPEECH_TIMEOUT = 5
    }

    val workerUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[WORKER_URL] ?: ""
    }

    val apiToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_TOKEN] ?: ""
    }

    val speechTimeoutSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SPEECH_TIMEOUT_SECONDS] ?: DEFAULT_SPEECH_TIMEOUT
    }

    suspend fun setWorkerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[WORKER_URL] = url
        }
    }

    suspend fun setApiToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[API_TOKEN] = token
        }
    }

    suspend fun setSpeechTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[SPEECH_TIMEOUT_SECONDS] = seconds
        }
    }

    fun isConfigured(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[WORKER_URL].isNullOrBlank() && !prefs[API_TOKEN].isNullOrBlank()
    }
}
