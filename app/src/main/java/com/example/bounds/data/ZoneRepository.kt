package com.example.bounds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bounds.model.Zone
import com.example.bounds.util.DomainBlocklist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zoneDataStore by preferencesDataStore(name = "zones")

class ZoneRepository(
    private val context: Context,
    private val mutationGuard: ActiveEnforcementMutationGuard =
        ActiveEnforcementMutationGuard()
) {

    private val gson = Gson()
    private val zonesKey = stringPreferencesKey("zones_json")

    val zonesFlow: Flow<List<Zone>> = context.zoneDataStore.data.map { prefs ->
        val json = prefs[zonesKey] ?: "[]"
        runCatching {
            val type = object : TypeToken<List<Zone>>() {}.type
            normalizeZones(gson.fromJson<List<Zone>>(json, type) ?: emptyList())
        }.getOrDefault(emptyList())
    }

    suspend fun saveZones(zones: List<Zone>): Boolean {
        val normalized = normalizeZones(zones)
        var persisted = false
        context.zoneDataStore.edit { prefs ->
            val current = decodeZones(prefs[zonesKey])
            persisted = mutationGuard.commitZoneMutation(current, normalized) {
                prefs[zonesKey] = gson.toJson(normalized)
            }
        }
        return persisted
    }

    private fun decodeZones(json: String?): List<Zone> = runCatching {
        val type = object : TypeToken<List<Zone>>() {}.type
        normalizeZones(gson.fromJson<List<Zone>>(json ?: "[]", type) ?: emptyList())
    }.getOrDefault(emptyList())

    companion object {
        /**
         * Gson can instantiate a data class without running its constructor,
         * so fields absent from old JSON may arrive as null despite Kotlin
         * defaults. Normalize both legacy lists and the new domain list here.
         */
        fun normalizeZones(zones: List<Zone>): List<Zone> = zones.map { zone ->
            zone.copy(
                blockedApps = zone.blockedApps ?: emptyList(),
                blockedDomains = DomainBlocklist.canonicalizeAll(zone.blockedDomains ?: emptyList())
            )
        }
    }
}
