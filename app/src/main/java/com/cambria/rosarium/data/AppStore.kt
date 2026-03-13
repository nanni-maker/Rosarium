package com.cambria.rosarium.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cambria.rosarium.core.RosaryPack
import com.cambria.rosarium.repository.RosaryRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rosarium_settings")

class AppStore(private val context: Context) {

    private val gson = Gson()

    private val activePackKey = stringPreferencesKey("active_pack_id")
    private val packsJsonKey = stringPreferencesKey("packs_json")

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

    suspend fun loadPacks(): List<RosaryPack> {
        val preferences = context.dataStore.data.first()
        val json = preferences[packsJsonKey]

        if (json.isNullOrBlank()) {
            val defaults = RosaryRepository.defaultPacks()
            savePacks(defaults)
            if (defaults.isNotEmpty()) {
                setActivePackId(defaults.first().id)
            }
            return defaults
        }

        val type = object : TypeToken<List<RosaryPack>>() {}.type
        val loaded: List<RosaryPack>? = gson.fromJson(json, type)

        if (loaded.isNullOrEmpty()) {
            val defaults = RosaryRepository.defaultPacks()
            savePacks(defaults)
            if (defaults.isNotEmpty()) {
                setActivePackId(defaults.first().id)
            }
            return defaults
        }

        return loaded
    }

    suspend fun savePacks(packs: List<RosaryPack>) {
        val json = gson.toJson(packs)
        context.dataStore.edit { preferences ->
            preferences[packsJsonKey] = json
        }
    }
}