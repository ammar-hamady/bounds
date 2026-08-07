package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.model.Zone
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.BorderDim
import com.example.bounds.ui.theme.DividerLine
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.ui.theme.TextSubtle

@Composable
fun HomeScreen(
    zones: List<Zone>,
    onAddZoneClick: () -> Unit,
    onToggleZone: (String, Boolean) -> Unit,
    onEditZone: (Zone) -> Unit = {},
    onDeleteZone: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeCount = zones.count { it.isEnabled }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Subtitle
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (zones.isEmpty()) "No zones yet"
                       else "$activeCount of ${zones.size} zones active",
                fontSize = 14.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(20.dp))
        }

        // Section label
        if (zones.isNotEmpty()) {
            item {
                Text(
                    text = "YOUR ZONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        // Grouped zone list
        if (zones.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgSurface)
                ) {
                    zones.forEachIndexed { index, zone ->
                        ZoneRow(
                            zone = zone,
                            onToggle = { onToggleZone(zone.id, it) },
                            onEdit   = { onEditZone(zone) },
                            onDelete = { onDeleteZone(zone.id) }
                        )
                        if (index < zones.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 68.dp)
                                    .height(1.dp)
                                    .background(DividerLine)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Add New Zone — dashed border button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, BorderDim, RoundedCornerShape(16.dp))
                    .clickable(onClick = onAddZoneClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Add New Zone",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Amber
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ZoneRow(
    zone: Zone,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete \"${zone.name}\"?") },
            text  = { Text(text = "This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (zone.isEnabled) AmberDim else BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (zone.isEnabled) Amber else TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zone.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val subtitle = buildString {
                if (zone.isTimeSensitive) append("${zone.startTime} – ${zone.endTime} · ")
                append("${zone.radiusMeters}m radius")
            }
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1
            )
        }

        // Delete button — intercepts click so it does not bubble to the row's onEdit
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete zone",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        // Toggle — wrapped to prevent click from bubbling up to the row's onEdit
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
        ) {
            Switch(
                checked = zone.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor      = Color.White,
                    checkedTrackColor      = Amber,
                    uncheckedThumbColor    = Color.White,
                    uncheckedTrackColor    = BgElevated,
                    uncheckedBorderColor   = BorderDim
                )
            )
        }
    }
}
