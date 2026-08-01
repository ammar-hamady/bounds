package com.example.bounds.ui.screens

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.model.ActiveEnforcementInfo
import com.example.bounds.model.Zone
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgBanner
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.BorderDim
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.util.AppBlockingManager

@Composable
fun CurrentScreen(
    // Geofence-driven enforcement state (null = no zone active)
    activeEnforcement: ActiveEnforcementInfo? = null,
    // Location permission state
    hasFineLocation: Boolean = false,
    hasBackgroundLocation: Boolean = true,
    onRequestFineLocation: () -> Unit = {},
    onRequestBackgroundLocation: () -> Unit = {},
    // Zone list (for Simulate Entry picker)
    zones: List<Zone> = emptyList(),
    onSimulateEntry: (Zone) -> Unit = {},
    // Manual block (legacy CurrentScreen button)
    onManualBlockingStarted: (appName: String, durationMinutes: Int) -> Unit = { _, _ -> },
    graceTimerSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    var manualIsLocked by remember { mutableStateOf(false) }
    var manualStatusMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Derive display state from enforcement or manual lock
    val isEnforcingZone = activeEnforcement != null
    val isGrace = activeEnforcement?.isGracePeriod == true
    val isBlocking = (isEnforcingZone && !isGrace) || manualIsLocked
    val zoneName = activeEnforcement?.zoneName
    val blockedAppsLabel = when {
        activeEnforcement != null && activeEnforcement.blockedApps.isNotEmpty() -> {
            val apps = activeEnforcement.blockedApps
            if (apps.size == 1) "${apps[0]} is blocked"
            else "${apps[0]} + ${apps.size - 1} more blocked"
        }
        manualIsLocked -> "Instagram is blocked"
        isEnforcingZone && isGrace -> "Grace period — blocking soon"
        else -> "Waiting to enter a zone"
    }

    val ringFraction = when {
        isGrace      -> 0.75f   // partial ring during grace
        isBlocking   -> 1.0f
        else         -> 1.0f
    }
    val ringColor = when {
        isGrace    -> Amber.copy(alpha = 0.5f)
        isBlocking -> Amber
        else       -> Amber.copy(alpha = 0.3f)
    }
    val graceDisplay = if (graceTimerSeconds > 0) {
        val m = graceTimerSeconds / 60; val s = graceTimerSeconds % 60
        if (m > 0) "$m:${s.toString().padStart(2, '0')}" else "0:${s.toString().padStart(2, '0')}"
    } else "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Location permission banner ─────────────────────────────────────────
        if (!hasFineLocation) {
            LocationPermissionBanner(
                onRequestPermission = onRequestFineLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        } else if (!hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BackgroundLocationBanner(
                onRequestPermission = onRequestBackgroundLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ── Zone banner ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(BgBanner, RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isEnforcingZone) Amber else TextMuted,
                        shape = CircleShape
                    )
            )
            Text(
                text = "Currently in: ",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = zoneName ?: "No active zone",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnforcingZone) Amber else TextMuted
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Circular ring ──────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val strokeWidth = 14.dp.toPx()
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = BgElevated, startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke
                )
                drawArc(
                    color = ringColor, startAngle = -90f, sweepAngle = ringFraction * 360f,
                    useCenter = false, topLeft = topLeft, size = arcSize, style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    isGrace -> {
                        Text(
                            text = graceDisplay,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "GRACE PERIOD",
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    isBlocking -> {
                        Text(
                            text = "LOCKED",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "BLOCKING",
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "—",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "IDLE",
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = blockedAppsLabel,
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Feed lock illustration ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 108.dp)
                .background(BgSurface, RoundedCornerShape(14.dp))
                .border(1.dp, BorderDim, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isBlocking) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isBlocking) Amber else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (isBlocking) "FEED\nLOCKED" else "FEED\nFREE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = if (isBlocking) Amber.copy(alpha = 0.7f) else TextMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Status message (manual block feedback)
        if (manualStatusMsg.isNotEmpty() && !isEnforcingZone) {
            Text(
                text = manualStatusMsg,
                fontSize = 12.sp,
                color = if (manualStatusMsg.contains("not installed"))
                    MaterialTheme.colorScheme.error else Amber,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        // ── Manual lock button (always available as override) ──────────────────
        Button(
            onClick = {
                if (manualIsLocked || isEnforcingZone) {
                    AppBlockingManager.stopAllBlocking(context)
                    manualStatusMsg = ""
                    manualIsLocked = false
                } else {
                    val duration = graceTimerSeconds.coerceAtLeast(5)
                    val ok = AppBlockingManager.startBlockingApp(context, durationMinutes = duration)
                    if (ok) {
                        manualStatusMsg = "✅ Instagram locked for $duration minutes"
                        manualIsLocked = true
                        onManualBlockingStarted("Instagram", duration)
                    } else {
                        manualStatusMsg = "❌ Instagram not installed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (manualIsLocked || isBlocking) MaterialTheme.colorScheme.error else Amber,
                contentColor   = if (manualIsLocked || isBlocking) Color.White else Color.Black
            )
        ) {
            Text(
                text = if (manualIsLocked || isBlocking) "Unlock Phone" else "Unlock Phone",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Simulate Entry button ──────────────────────────────────────────────
        val simulateZone = zones.firstOrNull { it.isEnabled && it.blockedApps.isNotEmpty() }
        if (simulateZone != null) {
            TextButton(
                onClick = { onSimulateEntry(simulateZone) },
                modifier = Modifier
                    .background(BgBanner, RoundedCornerShape(50.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Simulate Entry · ${simulateZone.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Amber
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Permission banners ────────────────────────────────────────────────────────

@Composable
private fun LocationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AmberDim, RoundedCornerShape(14.dp))
            .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Location required",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
            Text(
                text = "Zones are inactive until location is granted.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        TextButton(onClick = onRequestPermission) {
            Text("Enable", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackgroundLocationBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(BgSurface, RoundedCornerShape(14.dp))
            .border(1.dp, BorderDim, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Background location",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Allow "All the time" for zones to trigger when the app is closed.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        TextButton(onClick = onRequestPermission) {
            Text("Allow", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}
