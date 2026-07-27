package com.example.bounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.ThemePreference
import com.example.bounds.model.Zone
import com.example.bounds.ui.screens.AddZoneScreen
import com.example.bounds.ui.screens.AnalyticsScreen
import com.example.bounds.ui.screens.CurrentScreen
import com.example.bounds.ui.screens.HomeScreen
import com.example.bounds.ui.screens.SettingsScreen
import com.example.bounds.ui.theme.BoundsTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoundsApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoundsApp() {
    // ── App-wide settings ─────────────────────────────────────────────────────
    var themePreference by rememberSaveable { mutableStateOf(ThemePreference.SYSTEM) }
    var graceTimerSeconds by rememberSaveable { mutableStateOf(0) }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> systemDark
    }

    BoundsTheme(darkTheme = isDark, dynamicColor = false) {
        // ── Navigation state ──────────────────────────────────────────────────
        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ZONES) }
        var showAddZoneScreen by rememberSaveable { mutableStateOf(false) }
        var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
        var editingZone by rememberSaveable { mutableStateOf<Zone?>(null) }

        // ── Data state ────────────────────────────────────────────────────────
        var zones by rememberSaveable { mutableStateOf<List<Zone>>(emptyList()) }
        var analyticsEvents by rememberSaveable { mutableStateOf<List<AnalyticsEvent>>(emptyList()) }

        when {
            showSettingsScreen -> {
                SettingsScreen(
                    themePreference = themePreference,
                    onThemeChange = { themePreference = it },
                    graceTimerSeconds = graceTimerSeconds,
                    onGraceTimerChange = { graceTimerSeconds = it },
                    onDeleteAnalyticsData = { analyticsEvents = emptyList() },
                    onBack = { showSettingsScreen = false }
                )
            }

            showAddZoneScreen -> {
                AddZoneScreen(
                    onSave = { newZone ->
                        if (editingZone != null) {
                            zones = zones.map { if (it.id == editingZone!!.id) newZone else it }
                        } else {
                            zones = zones + newZone
                        }
                        showAddZoneScreen = false
                        editingZone = null
                    },
                    onCancel = {
                        showAddZoneScreen = false
                        editingZone = null
                    },
                    initialZone = editingZone
                )
            }

            else -> {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        AppDestinations.entries.forEach { dest ->
                            item(
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                selected = dest == currentDestination,
                                onClick = { currentDestination = dest }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = currentDestination.label,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                actions = {
                                    IconButton(onClick = { showSettingsScreen = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        when (currentDestination) {
                            AppDestinations.ZONES -> {
                                HomeScreen(
                                    zones = zones,
                                    onAddZoneClick = {
                                        editingZone = null
                                        showAddZoneScreen = true
                                    },
                                    onToggleZone = { zoneId, isEnabled ->
                                        zones = zones.map { zone ->
                                            if (zone.id == zoneId) zone.copy(isEnabled = isEnabled) else zone
                                        }
                                    },
                                    onEditZone = { zone ->
                                        editingZone = zone
                                        showAddZoneScreen = true
                                    },
                                    onDeleteZone = { zoneId ->
                                        zones = zones.filter { it.id != zoneId }
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }

                            AppDestinations.CURRENT -> {
                                CurrentScreen(
                                    onBlockingStarted = { appName, durationMinutes ->
                                        val event = AnalyticsEvent(
                                            id = UUID.randomUUID().toString(),
                                            appName = appName,
                                            zoneName = "Manual Block",
                                            durationMinutes = durationMinutes,
                                            timestampMs = System.currentTimeMillis()
                                        )
                                        analyticsEvents = analyticsEvents + event
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }

                            AppDestinations.ANALYTICS -> {
                                AnalyticsScreen(
                                    events = analyticsEvents,
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CURRENT("Current", Icons.Default.MyLocation),
    ZONES("Zones", Icons.Default.Home),
    ANALYTICS("Analytics", Icons.Default.BarChart),
}
