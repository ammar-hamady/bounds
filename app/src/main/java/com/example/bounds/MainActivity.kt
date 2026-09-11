package com.example.bounds

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
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
import com.example.bounds.util.PermissionUtils
import com.example.bounds.util.WebsiteBlockingManager
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

    // ── ViewModel (persisted state) ───────────────────────────────────────────
    val boundsViewModel: BoundsViewModel = viewModel(factory = BoundsViewModel.Factory)
    val zones by boundsViewModel.zones.collectAsState()
    val analyticsEvents by boundsViewModel.analyticsEvents.collectAsState()

    // ── App-wide settings (persisted via DataStore through ViewModel) ─────────
    val themePreference by boundsViewModel.themePreference.collectAsState()
    val graceTimerSeconds by boundsViewModel.graceTimerSeconds.collectAsState()
    val hapticFeedbackEnabled by boundsViewModel.hapticFeedbackEnabled.collectAsState()
    val entryNotificationsEnabled by boundsViewModel.entryNotificationsEnabled.collectAsState()
    val blockIntensity by boundsViewModel.blockIntensity.collectAsState()
    val defaultBlockedApps by boundsViewModel.defaultBlockedApps.collectAsState()
    val websiteEnforcement by app.websiteEnforcement.collectAsState()
    // CurrentScreen is disposed when the user changes tabs. Keep manual lock
    // state above the tab content so returning to Current does not reset it.
    var manualIsLocked by rememberSaveable { mutableStateOf(false) }
    var manualStatusMsg by rememberSaveable { mutableStateOf("") }

    // ── Usage Access permission ───────────────────────────────────────────────
    var hasUsageStatsPermission by remember {
        mutableStateOf(PermissionUtils.hasUsageStatsPermission(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var suppressNextVpnResumeRefresh by remember { mutableStateOf(false) }

    // Recheck whenever the app resumes (user may have just returned from Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStatsPermission = PermissionUtils.hasUsageStatsPermission(context)
                hasOverlayPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                hasNotificationPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                if (suppressNextVpnResumeRefresh) {
                    suppressNextVpnResumeRefresh = false
                } else {
                    WebsiteBlockingManager.refreshReadiness(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        boundsViewModel.saveEntryNotificationsEnabled(granted)
    }

    val vpnConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        WebsiteBlockingManager.handleConsentResult(
            context = context,
            approvalReported = result.resultCode == Activity.RESULT_OK
        )
    }

    // The application-level website state starts with a conservative default.
    // Resolve it when the UI first opens as well as when returning from Android
    // Settings, otherwise previously granted consent can still appear unavailable.
    LaunchedEffect(Unit) {
        WebsiteBlockingManager.refreshReadiness(context)
    }

    // ── Sync zone list to Application singleton + platform geofences ──────────
    LaunchedEffect(zones, hasFineLocation) {
        app.zones = zones
        if (hasFineLocation) BoundsGeofenceManager.syncGeofences(context, zones)
    }

    val activeEnforcement by app.activeEnforcement.collectAsState()

    // ── Consume analytics events produced by GeofenceEnforcementService ───────
    val pendingAnalytics by app.pendingAnalytics.collectAsState()
    LaunchedEffect(pendingAnalytics) {
        pendingAnalytics?.let { event ->
            boundsViewModel.addEvent(event)
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
        // Save only the ID. Zone itself is not Parcelable/Serializable and
        // cannot safely be stored in Android's saved-instance-state Bundle.
        var editingZoneId      by rememberSaveable { mutableStateOf<String?>(null) }
        var showProtectedMessage by remember { mutableStateOf(false) }
        val editingZone = editingZoneId?.let { id -> zones.firstOrNull { it.id == id } }
        val protectedZoneId = activeEnforcement?.zoneId

        if (showProtectedMessage) {
            AlertDialog(
                onDismissRequest = { showProtectedMessage = false },
                title = { Text("Protection settings locked") },
                text = { Text("These settings can be changed after you leave the active zone.") },
                confirmButton = {
                    TextButton(onClick = { showProtectedMessage = false }) { Text("OK") }
                }
            )
        }

        val navLayer = when {
            showSettingsScreen -> NavLayer.SETTINGS
            showAddZoneScreen  -> NavLayer.ADD_ZONE
            else               -> NavLayer.MAIN
        }

        // Slide full-screen flows in from the right; back-action slides out
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
                        themePreference          = themePreference,
                        onThemeChange            = { boundsViewModel.saveThemePreference(it) },
                        graceTimerSeconds        = graceTimerSeconds,
                        onGraceTimerChange       = { boundsViewModel.saveGraceTimerSeconds(it) },
                        hapticFeedbackEnabled    = hapticFeedbackEnabled,
                        onHapticFeedbackChange   = { boundsViewModel.saveHapticFeedbackEnabled(it) },
                        entryNotificationsEnabled =
                            entryNotificationsEnabled && hasNotificationPermission,
                        onEntryNotificationsChange = { enabled ->
                            if (
                                enabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                boundsViewModel.saveEntryNotificationsEnabled(enabled)
                            }
                        },
                        blockIntensity           = blockIntensity,
                        onBlockIntensityChange   = { boundsViewModel.saveBlockIntensity(it) },
                        defaultBlockedApps       = defaultBlockedApps,
                        onDefaultBlockedAppsChange = { boundsViewModel.saveDefaultBlockedApps(it) },
                        onDeleteAnalyticsData    = { boundsViewModel.clearEvents() },
                        onBack                   = { showSettingsScreen = false },
                        hasUsageStatsPermission  = hasUsageStatsPermission,
                        onRequestUsageAccess     = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        websiteEnforcement       = websiteEnforcement,
                        onRequestWebsiteVpnConsent = {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent == null) {
                                WebsiteBlockingManager.retryActivePolicy(context)
                            } else {
                                // Activity results can arrive before or after ON_RESUME.
                                // Suppress this consent flow's passive refresh so it cannot
                                // overwrite the more specific result or startup state.
                                suppressNextVpnResumeRefresh = true
                                WebsiteBlockingManager.markConsentPending(context)
                                vpnConsentLauncher.launch(prepareIntent)
                            }
                        },
                        enforcementSettingsLocked = protectedZoneId != null,
                        onProtectedAction = { showProtectedMessage = true }
                    )
                }

                NavLayer.ADD_ZONE -> {
                    AddZoneScreen(
                        onSave = { newZone ->
                            val updatedZones = if (editingZone != null) {
                                zones.map { if (it.id == editingZone!!.id) newZone else it }
                            } else {
                                zones + newZone
                            }
                            boundsViewModel.saveZones(updatedZones) { saved ->
                                if (saved) {
                                    showAddZoneScreen = false
                                    editingZoneId = null
                                } else {
                                    showProtectedMessage = true
                                }
                            }
                        },
                        onCancel = {
                            showAddZoneScreen = false
                            editingZoneId = null
                        },
                        initialZone = editingZone,
                        defaultBlockedApps = defaultBlockedApps,
                        isProtected = editingZoneId != null && editingZoneId == protectedZoneId,
                        onProtectedAction = { showProtectedMessage = true }
                    )
                }

                NavLayer.MAIN -> {
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
                        bottomBar = {
                            BoundsBottomNavigation(
                                selected = currentDestination,
                                onSelect = { currentDestination = it }
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->

                        // Crossfade between tabs (~200 ms)
                        Crossfade(
                            targetState = currentDestination,
                            animationSpec = tween(200),
                            label = "tabCrossfade",
                            modifier = Modifier.padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) { dest ->
                            when (dest) {
                                AppDestinations.ZONES -> {
                                    HomeScreen(
                                        zones = zones,
                                        onAddZoneClick = {
                                            editingZoneId = null
                                            showAddZoneScreen = true
                                        },
                                        onToggleZone = { id, enabled ->
                                            boundsViewModel.saveZones(
                                                zones.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
                                            )
                                        },
                                        onEditZone = { zone ->
                                            editingZoneId = zone.id
                                            showAddZoneScreen = true
                                        },
                                        onDeleteZone = { id ->
                                            boundsViewModel.saveZones(zones.filter { it.id != id })
                                        },
                                        protectedZoneId = protectedZoneId
                                    )
                                }

                                AppDestinations.CURRENT -> {
                                    CurrentScreen(
                                        activeEnforcement = activeEnforcement,
                                        manualIsLocked = manualIsLocked,
                                        manualStatusMsg = manualStatusMsg,
                                        hasFineLocation = hasFineLocation,
                                        hasBackgroundLocation = hasBackgroundLocation,
                                        hasUsageStatsPermission = hasUsageStatsPermission,
                                        hasOverlayPermission = hasOverlayPermission,
                                        graceTimerSeconds = graceTimerSeconds,
                                        zones = zones,
                                        onRequestFineLocation = {
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
                                        onRequestUsageAccess = {
                                            context.startActivity(
                                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        },
                                        onRequestOverlayPermission = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        Uri.parse("package:${context.packageName}")
                                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                        onManualLockChange = { manualIsLocked = it },
                                        onManualStatusChange = { manualStatusMsg = it },
                                        onSimulateEntry = { zone ->
                                            val intent = Intent(context, GeofenceEnforcementService::class.java).apply {
                                                action = GeofenceEnforcementService.ACTION_ZONE_ENTER
                                                putExtra(GeofenceEnforcementService.EXTRA_ZONE_ID, zone.id)
                                                putExtra(GeofenceEnforcementService.EXTRA_ZONE_NAME, zone.name)
                                                putStringArrayListExtra(
                                                    GeofenceEnforcementService.EXTRA_BLOCKED_APPS,
                                                    ArrayList(zone.blockedApps)
                                                )
                                                putStringArrayListExtra(
                                                    GeofenceEnforcementService.EXTRA_BLOCKED_DOMAINS,
                                                    ArrayList(zone.blockedDomains)
                                                )
                                                putExtra(GeofenceEnforcementService.EXTRA_IS_TIME_SENSITIVE, zone.isTimeSensitive)
                                                putExtra(GeofenceEnforcementService.EXTRA_START_TIME, zone.startTime)
                                                putExtra(GeofenceEnforcementService.EXTRA_END_TIME, zone.endTime)
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                        },
                                        onManualBlockingStarted = { appName, durationMinutes ->
                                            boundsViewModel.addEvent(
                                                AnalyticsEvent(
                                                    id = UUID.randomUUID().toString(),
                                                    appName = appName,
                                                    zoneName = "Manual Block",
                                                    durationMinutes = durationMinutes,
                                                    timestampMs = System.currentTimeMillis()
                                                )
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

@Composable
private fun BoundsBottomNavigation(
    selected: AppDestinations,
    onSelect: (AppDestinations) -> Unit
) {
    val dockShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(dockShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
                    shape = dockShape
                )
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keep Current in the middle so the app's primary status screen
            // remains the visual anchor of the navigation.
            listOf(
                AppDestinations.ZONES,
                AppDestinations.CURRENT,
                AppDestinations.ANALYTICS
            ).forEach { destination ->
                val isSelected = destination == selected
                val itemShape = RoundedCornerShape(17.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(itemShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { onSelect(destination) }
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        modifier = Modifier.size(19.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = destination.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
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
