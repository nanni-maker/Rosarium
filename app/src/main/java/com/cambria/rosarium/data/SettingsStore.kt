package com.cambria.rosarium.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rosarium_settings")

class SettingsStore(private val context: Context) {

    private val activePackKey = stringPreferencesKey("active_pack_id")

    fun activePackIdFlow(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[activePackKey]
        }
    }

    suspend fun setActivePackId(packId: String) {
        context.dataStore.edit { preferences ->
            preferences[activePackKey] = packId
        }
    }
}