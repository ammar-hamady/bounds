package com.example.bounds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bounds.model.Zone
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zoneDataStore by preferencesDataStore(name = "zones")

class ZoneRepository(private val context: Context) {

    private val gson = Gson()
    private val zonesKey = stringPreferencesKey("zones_json")

    val zonesFlow: Flow<List<Zone>> = context.zoneDataStore.data.map { prefs ->
        val json = prefs[zonesKey] ?: "[]"
        runCatching {
            val type = object : TypeToken<List<Zone>>() {}.type
            gson.fromJson<List<Zone>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun saveZones(zones: List<Zone>) {
        context.zoneDataStore.edit { prefs ->
            prefs[zonesKey] = gson.toJson(zones)
        }
    }
}
