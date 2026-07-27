package com.example.bounds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.bounds.model.Zone
import com.example.bounds.ui.screens.AddZoneScreen
import com.example.bounds.ui.screens.CurrentScreen
import com.example.bounds.ui.screens.HomeScreen
import com.example.bounds.ui.theme.BoundsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BoundsTheme {
                BoundsApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun BoundsApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.ZONES) }
    var showAddZoneScreen by rememberSaveable { mutableStateOf(false) }
    var editingZone by rememberSaveable { mutableStateOf<Zone?>(null) }
    var zones by rememberSaveable { mutableStateOf<List<Zone>>(emptyList()) }

    if (showAddZoneScreen) {
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
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                painterResource(it.icon),
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    CURRENT("Current", R.drawable.ic_favorite),
    ZONES("Zones", R.drawable.ic_home),
}
