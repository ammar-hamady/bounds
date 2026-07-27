package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.R
import com.example.bounds.model.App
import com.example.bounds.model.Zone
import com.example.bounds.ui.components.AppChip
import com.example.bounds.ui.components.MapLocationPicker
import com.example.bounds.ui.components.RadiusSlider
import java.util.UUID

@Composable
fun AddZoneScreen(
    onSave: (Zone) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialZone: Zone? = null
) {
    var zoneName by remember { mutableStateOf(initialZone?.name ?: "") }
    var latitude by remember { mutableStateOf(initialZone?.latitude ?: 40.7128) }
    var longitude by remember { mutableStateOf(initialZone?.longitude ?: -74.0060) }
    var radius by remember { mutableStateOf(initialZone?.radiusMeters ?: 50) }
    var isTimeSensitive by remember { mutableStateOf(initialZone?.isTimeSensitive ?: false) }
    var startTime by remember { mutableStateOf(initialZone?.startTime ?: "22:00") }
    var endTime by remember { mutableStateOf(initialZone?.endTime ?: "07:00") }
    var selectedApps by remember {
        val blockedAppNames = initialZone?.blockedApps ?: emptyList()
        mutableStateOf(
            listOf(
                App("1", "Instagram", "ic_home", blockedAppNames.contains("Instagram")),
                App("2", "TikTok", "ic_home", blockedAppNames.contains("TikTok")),
                App("3", "Twitter", "ic_home", blockedAppNames.contains("Twitter")),
                App("4", "Discord", "ic_home", blockedAppNames.contains("Discord")),
                App("5", "YouTube", "ic_home", blockedAppNames.contains("YouTube")),
                App("6", "Facebook", "ic_home", blockedAppNames.contains("Facebook")),
                App("7", "Reddit", "ic_home", blockedAppNames.contains("Reddit")),
                App("8", "Telegram", "ic_home", blockedAppNames.contains("Telegram")),
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header with Cancel/Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Text(
                text = if (initialZone != null) "Edit Zone" else "New Zone",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = {
                    val newZone = Zone(
                        id = initialZone?.id ?: UUID.randomUUID().toString(),
                        name = zoneName,
                        isEnabled = initialZone?.isEnabled ?: true,
                        isTimeSensitive = isTimeSensitive,
                        startTime = startTime,
                        endTime = endTime,
                        blockedApps = selectedApps.filter { it.isSelected }.map { it.name },
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radius
                    )
                    onSave(newZone)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Zone Name Input
            OutlinedTextField(
                value = zoneName,
                onValueChange = { zoneName = it },
                label = { Text("Zone Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Map Location Picker
            MapLocationPicker(
                latitude = latitude,
                longitude = longitude,
                onLocationChange = { lat, lon ->
                    latitude = lat
                    longitude = lon
                }
            )

            // Radius Slider
            RadiusSlider(
                radius = radius,
                onRadiusChange = { radius = it }
            )

            // Time Sensitive Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time Sensitive",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isTimeSensitive,
                        onCheckedChange = { isTimeSensitive = it }
                    )
                }

                if (isTimeSensitive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                    Text(
                        text = "Format: HH:mm (e.g., 22:00)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // App Selection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Block These Apps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedApps) { app ->
                        AppChip(
                            name = app.name,
                            icon = painterResource(R.drawable.ic_home),
                            isSelected = app.isSelected,
                            onSelect = { isSelected ->
                                selectedApps = selectedApps.map {
                                    if (it.id == app.id) it.copy(isSelected = isSelected) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
