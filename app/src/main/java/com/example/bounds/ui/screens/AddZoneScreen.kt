package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.BorderDim
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.ui.theme.TextSubtle
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
    var radius by remember { mutableStateOf((initialZone?.radiusMeters ?: 200).toFloat()) }
    var isTimeSensitive by remember { mutableStateOf(initialZone?.isTimeSensitive ?: false) }
    var startTime by remember { mutableStateOf(initialZone?.startTime ?: "22:00") }
    var endTime by remember { mutableStateOf(initialZone?.endTime ?: "07:00") }
    var selectedApps by remember {
        val blocked = initialZone?.blockedApps ?: emptyList()
        mutableStateOf(
            listOf(
                App("1", "Instagram", "ic_home", blocked.contains("Instagram")),
                App("2", "TikTok",    "ic_home", blocked.contains("TikTok")),
                App("3", "Twitter",   "ic_home", blocked.contains("Twitter")),
                App("4", "Discord",   "ic_home", blocked.contains("Discord")),
                App("5", "YouTube",   "ic_home", blocked.contains("YouTube")),
                App("6", "Facebook",  "ic_home", blocked.contains("Facebook")),
                App("7", "Reddit",    "ic_home", blocked.contains("Reddit")),
                App("8", "Telegram",  "ic_home", blocked.contains("Telegram")),
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Text(
                text = if (initialZone != null) "Edit Zone" else "New Zone",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Scrollable body ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Map
            MapLocationPicker(
                latitude = latitude,
                longitude = longitude,
                onLocationChange = { lat, lon ->
                    latitude = lat
                    longitude = lon
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Zone Name input
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "ZONE NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    placeholder = {
                        Text(
                            text = """e.g. "Gym", "Library"""",
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = BgSurface,
                        unfocusedContainerColor = BgSurface,
                        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor             = Amber
                    ),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(24.dp))

            // Radius slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RADIUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        color = TextSubtle
                    )
                    Text(
                        text = "${radius.roundToInt()}m",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..500f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor              = Amber,
                        activeTrackColor        = Amber,
                        inactiveTrackColor      = BgElevated
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "50m",  fontSize = 11.sp, color = TextSubtle)
                    Text(text = "500m", fontSize = 11.sp, color = TextSubtle)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Time Sensitive toggle
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Time Window",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Only block during set hours",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isTimeSensitive,
                        onCheckedChange = { isTimeSensitive = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor   = Amber,
                            checkedThumbColor   = Color.White,
                            uncheckedTrackColor = BgElevated,
                            uncheckedBorderColor = BorderDim
                        )
                    )
                }

                if (isTimeSensitive) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "START",
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            TextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor   = BgElevated,
                                    unfocusedContainerColor = BgElevated,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                                    cursorColor             = Amber
                                ),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "END",
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            TextField(
                                value = endTime,
                                onValueChange = { endTime = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor   = BgElevated,
                                    unfocusedContainerColor = BgElevated,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                                    cursorColor             = Amber
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // App selection
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "BLOCK THESE APPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedApps) { app ->
                        AppChip(
                            name = app.name,
                            icon = painterResource(R.drawable.ic_home),
                            isSelected = app.isSelected,
                            onSelect = { sel ->
                                selectedApps = selectedApps.map {
                                    if (it.id == app.id) it.copy(isSelected = sel) else it
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Save button ───────────────────────────────────────────────────────
        Button(
            onClick = {
                onSave(
                    Zone(
                        id           = initialZone?.id ?: UUID.randomUUID().toString(),
                        name         = zoneName.ifBlank { "Unnamed Zone" },
                        isEnabled    = initialZone?.isEnabled ?: true,
                        isTimeSensitive = isTimeSensitive,
                        startTime    = startTime,
                        endTime      = endTime,
                        blockedApps  = selectedApps.filter { it.isSelected }.map { it.name },
                        latitude     = latitude,
                        longitude    = longitude,
                        radiusMeters = radius.roundToInt()
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor   = Color.Black
            )
        ) {
            Text(
                text = if (initialZone != null) "Save Changes" else "Save Zone",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
