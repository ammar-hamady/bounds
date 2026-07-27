package com.example.bounds.model

data class Zone(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val isTimeSensitive: Boolean = false,
    val startTime: String = "22:00",
    val endTime: String = "07:00",
    val blockedApps: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = 50
)

data class App(
    val id: String,
    val name: String,
    val iconName: String,
    val isSelected: Boolean = false,
    val category: String = "General"
)
