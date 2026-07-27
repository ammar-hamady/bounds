package com.example.bounds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

@Composable
fun ZoneCard(
    name: String,
    startTime: String,
    endTime: String,
    blockedAppsCount: Int,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$startTime - $endTime",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.size(width = 60.dp, height = 32.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )

                if (onEdit != null || onDelete != null) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEdit()
                                    showMenu = false
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (blockedAppsCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(minOf(blockedAppsCount, 3)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (blockedAppsCount > 3) {
                    Text(
                        text = "+${blockedAppsCount - 3}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
