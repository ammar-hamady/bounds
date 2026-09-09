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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.model.BlockIntensity
import com.example.bounds.model.ThemePreference
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.DividerLine
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.ui.theme.TextSubtle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    graceTimerSeconds: Int,
    onGraceTimerChange: (Int) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackChange: (Boolean) -> Unit,
    entryNotificationsEnabled: Boolean,
    onEntryNotificationsChange: (Boolean) -> Unit,
    blockIntensity: BlockIntensity,
    onBlockIntensityChange: (BlockIntensity) -> Unit,
    onManageAppBlocklist: () -> Unit,
    onDeleteAnalyticsData: () -> Unit,
    onBack: () -> Unit,
    hasUsageStatsPermission: Boolean = true,
    onRequestUsageAccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── USAGE ACCESS BANNER ───────────────────────────────────────────
            if (!hasUsageStatsPermission) {
                UsageAccessBanner(
                    onRequestUsageAccess = onRequestUsageAccess,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── PROTECTION ────────────────────────────────────────────────────
            SettingsGroup(label = "PROTECTION") {
                SettingsSwitchRow(
                    icon = Icons.Filled.Security,
                    label = "Strict blocking",
                    description = if (blockIntensity == BlockIntensity.STRICT) {
                        "Temporary bypass is disabled"
                    } else {
                        "Allows a five-minute bypass"
                    },
                    checked = blockIntensity == BlockIntensity.STRICT,
                    onCheckedChange = {
                        onBlockIntensityChange(
                            if (it) BlockIntensity.STRICT else BlockIntensity.STANDARD
                        )
                    }
                )
                SettingsDivider()
                // Grace period — functional slider embedded
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Grace period",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (graceTimerSeconds == 0) "Off" else "${graceTimerSeconds}s",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Amber
                        )
                    }
                    Slider(
                        value = graceTimerSeconds.toFloat(),
                        onValueChange = { onGraceTimerChange(it.roundToInt()) },
                        valueRange = 0f..60f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor         = Amber,
                            activeTrackColor   = Amber,
                            inactiveTrackColor = BgElevated
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Off", fontSize = 10.sp, color = TextSubtle)
                        Text("60s", fontSize = 10.sp, color = TextSubtle)
                    }
                }
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Filled.Vibration,
                    label = "Haptic feedback",
                    description = "Vibrate when protection activates or clears",
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = onHapticFeedbackChange
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Filled.Notifications,
                    label = "Entry notifications",
                    description = "Alert when you enter an active zone",
                    checked = entryNotificationsEnabled,
                    onCheckedChange = onEntryNotificationsChange
                )
            }

            // ── APPEARANCE ────────────────────────────────────────────────────
            SettingsGroup(label = "APPEARANCE") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = themePreference == pref,
                                onClick = { onThemeChange(pref) },
                                label = {
                                    Text(
                                        text = pref.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (themePreference == pref) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Amber,
                                    selectedLabelColor     = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // ── BLOCKED APPS ──────────────────────────────────────────────────
            SettingsGroup(label = "BLOCKED APPS") {
                SettingsRow(
                    icon = Icons.Filled.Apps,
                    label = "App blocklist",
                    value = "Managed per zone",
                    showArrow = true,
                    onClick = onManageAppBlocklist
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Filled.Language,
                    label = "Website blocklist",
                    value = "Requires VPN support"
                )
            }

            // ── DATA ──────────────────────────────────────────────────────────
            SettingsGroup(label = "DATA") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Analytics Data", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Delete all recorded blocking sessions. This cannot be undone.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Analytics Data")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Analytics Data?") },
            text  = { Text("All your blocking session history will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAnalyticsData()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Usage Access banner ───────────────────────────────────────────────────────

@Composable
fun UsageAccessBanner(onRequestUsageAccess: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AmberDim, RoundedCornerShape(14.dp))
            .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Usage Access required",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
            Text(
                text = "Enables reliable app blocking. Without it, blocked apps may not be detected.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        TextButton(onClick = onRequestUsageAccess) {
            Text("Enable", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun SettingsGroup(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = TextSubtle
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgSurface)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 60.dp)
            .height(1.dp)
            .background(DividerLine)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    value: String = "",
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BgElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(text = value, fontSize = 13.sp, color = TextMuted)
        }
        if (showArrow) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
