package com.example.bounds

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.ThemePreference
import com.example.bounds.model.Zone
import com.example.bounds.service.GeofenceEnforcementService
import com.example.bounds.ui.screens.AddZoneScreen
import com.example.bounds.ui.screens.AnalyticsScreen
import com.example.bounds.ui.screens.CurrentScreen
import com.example.bounds.ui.screens.HomeScreen
import com.example.bounds.ui.screens.SettingsScreen
import com.example.bounds.ui.theme.BoundsTheme
import com.example.bounds.util.BoundsGeofenceManager
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BoundsApp() }
    }
}

// Navigation keys for AnimatedContent so the slide direction is unambiguous
private enum class NavLayer { MAIN, ADD_ZONE, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoundsApp() {
    val context = LocalContext.current
    val app = context.applicationContext as BoundsApplication

    // ── App-wide settings ─────────────────────────────────────────────────────
    // Step 1: default to DARK so the app always opens in dark mode
    var themePreference by rememberSaveable { mutableStateOf(ThemePreference.DARK) }
    var graceTimerSeconds by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(graceTimerSeconds) { app.graceTimerSeconds = graceTimerSeconds }

    // ── Location permissions ──────────────────────────────────────────────────
    var hasFineLocation by remember {
        mutableStateOf(BoundsGeofenceManager.hasFineLocation(context))
    }
    var hasBackgroundLocation by remember {
        mutableStateOf(BoundsGeofenceManager.hasBackgroundLocation(context))
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasFineLocation = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasBackgroundLocation = BoundsGeofenceManager.hasBackgroundLocation(context)
    }

    val bgLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
        if (granted) BoundsGeofenceManager.syncGeofences(context, app.zones)
    }

    // ── Data state ────────────────────────────────────────────────────────────
    var zones by rememberSaveable { mutableStateOf<List<Zone>>(emptyList()) }
    var analyticsEvents by rememberSaveable { mutableStateOf<List<AnalyticsEvent>>(emptyList()) }

    LaunchedEffect(zones, hasFineLocation) {
        app.zones = zones
        if (hasFineLocation) BoundsGeofenceManager.syncGeofences(context, zones)
    }

    val activeEnforcement by app.activeEnforcement.collectAsState()

    val pendingAnalytics by app.pendingAnalytics.collectAsState()
    LaunchedEffect(pendingAnalytics) {
        pendingAnalytics?.let { event ->
            analyticsEvents = analyticsEvents + event
            app.consumeAnalyticsEvent()
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        ThemePreference.DARK   -> true
        ThemePreference.LIGHT  -> false
        ThemePreference.SYSTEM -> systemDark
    }

    BoundsTheme(darkTheme = isDark, dynamicColor = false) {
        var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ZONES) }
        var showAddZoneScreen  by rememberSaveable { mutableStateOf(false) }
        var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
        var editingZone        by rememberSaveable { mutableStateOf<Zone?>(null) }

        val navLayer = when {
            showSettingsScreen -> NavLayer.SETTINGS
            showAddZoneScreen  -> NavLayer.ADD_ZONE
            else               -> NavLayer.MAIN
        }

        // Step 3: slide full-screen flows in from the right; back-action slides out
        AnimatedContent(
            targetState = navLayer,
            transitionSpec = {
                when {
                    // Pushing a new layer forward → slide in from right
                    targetState != NavLayer.MAIN ->
                        (slideInHorizontally(tween(280)) { it / 2 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(180)))
                    // Popping back to MAIN → slide out to right
                    else ->
                        (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { it / 2 } + fadeOut(tween(180)))
                }
            },
            label = "navLayerTransition"
        ) { layer ->
            when (layer) {
                NavLayer.SETTINGS -> {
                    SettingsScreen(
                        themePreference       = themePreference,
                        onThemeChange         = { themePreference = it },
                        graceTimerSeconds     = graceTimerSeconds,
                        onGraceTimerChange    = { graceTimerSeconds = it },
                        onDeleteAnalyticsData = { analyticsEvents = emptyList() },
                        onBack                = { showSettingsScreen = false }
                    )
                }

                NavLayer.ADD_ZONE -> {
                    AddZoneScreen(
                        onSave = { newZone ->
                            zones = if (editingZone != null) {
                                zones.map { if (it.id == editingZone!!.id) newZone else it }
                            } else {
                                zones + newZone
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

                NavLayer.MAIN -> {
                    NavigationSuiteScaffold(
                        navigationSuiteItems = {
                            AppDestinations.entries.forEach { dest ->
                                item(
                                    icon     = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
                                    label    = { Text(dest.label) },
                                    selected = dest == currentDestination,
                                    onClick  = { currentDestination = dest }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            modifier       = Modifier.fillMaxSize(),
                            topBar         = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text       = currentDestination.label,
                                            fontSize   = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = { showSettingsScreen = true }) {
                                            Icon(
                                                imageVector        = Icons.Default.Settings,
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

                            // Step 2: crossfade between tabs (~200 ms)
                            Crossfade(
                                targetState   = currentDestination,
                                animationSpec = tween(200),
                                label         = "tabCrossfade",
                                modifier      = Modifier.padding(innerPadding)
                            ) { dest ->
                                when (dest) {
                                    AppDestinations.ZONES -> {
                                        HomeScreen(
                                            zones          = zones,
                                            onAddZoneClick = {
                                                editingZone = null
                                                showAddZoneScreen = true
                                            },
                                            onToggleZone   = { id, enabled ->
                                                zones = zones.map {
                                                    if (it.id == id) it.copy(isEnabled = enabled) else it
                                                }
                                            },
                                            onEditZone     = { zone ->
                                                editingZone = zone
                                                showAddZoneScreen = true
                                            },
                                            onDeleteZone   = { id -> zones = zones.filter { it.id != id } }
                                        )
                                    }

                                    AppDestinations.CURRENT -> {
                                        CurrentScreen(
                                            activeEnforcement           = activeEnforcement,
                                            hasFineLocation             = hasFineLocation,
                                            hasBackgroundLocation       = hasBackgroundLocation,
                                            graceTimerSeconds           = graceTimerSeconds,
                                            zones                       = zones,
                                            onRequestFineLocation       = {
                                                fineLocationLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            },
                                            onRequestBackgroundLocation = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    bgLocationLauncher.launch(
                                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                                    )
                                                }
                                            },
                                            onSimulateEntry             = { zone ->
                                                val intent = Intent(context, GeofenceEnforcementService::class.java).apply {
                                                    action = GeofenceEnforcementService.ACTION_ZONE_ENTER
                                                    putExtra(GeofenceEnforcementService.EXTRA_ZONE_ID,   zone.id)
                                                    putExtra(GeofenceEnforcementService.EXTRA_ZONE_NAME, zone.name)
                                                    putStringArrayListExtra(
                                                        GeofenceEnforcementService.EXTRA_BLOCKED_APPS,
                                                        ArrayList(zone.blockedApps)
                                                    )
                                                    putExtra(GeofenceEnforcementService.EXTRA_IS_TIME_SENSITIVE, zone.isTimeSensitive)
                                                    putExtra(GeofenceEnforcementService.EXTRA_START_TIME, zone.startTime)
                                                    putExtra(GeofenceEnforcementService.EXTRA_END_TIME,   zone.endTime)
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                            },
                                            onManualBlockingStarted     = { appName, durationMinutes ->
                                                analyticsEvents = analyticsEvents + AnalyticsEvent(
                                                    id              = UUID.randomUUID().toString(),
                                                    appName         = appName,
                                                    zoneName        = "Manual Block",
                                                    durationMinutes = durationMinutes,
                                                    timestampMs     = System.currentTimeMillis()
                                                )
                                            }
                                        )
                                    }

                                    AppDestinations.ANALYTICS -> {
                                        AnalyticsScreen(events = analyticsEvents)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    CURRENT("Current",   Icons.Default.MyLocation),
    ZONES("Zones",       Icons.Default.Home),
    ANALYTICS("Analytics", Icons.Default.BarChart),
}
