package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.R
import com.example.bounds.model.Zone
import com.example.bounds.ui.components.ZoneCard

@Composable
fun HomeScreen(
    zones: List<Zone>,
    onAddZoneClick: () -> Unit,
    onToggleZone: (String, Boolean) -> Unit,
    onEditZone: (Zone) -> Unit = {},
    onDeleteZone: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your Sacred Spaces",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your focus zones",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = "Active Zones",
                value = "${zones.count { it.isEnabled }}"
            )
            StatItem(
                label = "Total Apps",
                value = "${zones.sumOf { it.blockedApps.size }}"
            )
            StatItem(
                label = "Reclaimed Time",
                value = "12h"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Zones List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(zones) { zone ->
                ZoneCard(
                    name = zone.name,
                    startTime = zone.startTime,
                    endTime = zone.endTime,
                    blockedAppsCount = zone.blockedApps.size,
                    isEnabled = zone.isEnabled,
                    onToggle = { newState ->
                        onToggleZone(zone.id, newState)
                    },
                    onEdit = { onEditZone(zone) },
                    onDelete = { onDeleteZone(zone.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Zone Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onAddZoneClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "+ Add New Zone",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
