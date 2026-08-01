package com.example.bounds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bounds.model.AnalyticsEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.analyticsDataStore by preferencesDataStore(name = "analytics")

class AnalyticsRepository(private val context: Context) {

    private val gson = Gson()
    private val eventsKey = stringPreferencesKey("analytics_events_json")

    val eventsFlow: Flow<List<AnalyticsEvent>> = context.analyticsDataStore.data.map { prefs ->
        val json = prefs[eventsKey] ?: "[]"
        runCatching {
            val type = object : TypeToken<List<AnalyticsEvent>>() {}.type
            gson.fromJson<List<AnalyticsEvent>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun saveEvents(events: List<AnalyticsEvent>) {
        context.analyticsDataStore.edit { prefs ->
            prefs[eventsKey] = gson.toJson(events)
        }
    }
}
