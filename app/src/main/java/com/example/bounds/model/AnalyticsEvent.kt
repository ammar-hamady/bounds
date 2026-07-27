package com.example.bounds.model

import java.io.Serializable

data class AnalyticsEvent(
    val id: String,
    val appName: String,
    val zoneName: String,
    val durationMinutes: Int,
    val timestampMs: Long
) : Serializable
