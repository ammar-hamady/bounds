package com.example.bounds.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// ── Lock state key for AnimatedContent ───────────────────────────────────────
private enum class LockState { IDLE, GRACE, LOCKED }

@Composable
fun CurrentScreen(
    activeEnforcement: ActiveEnforcementInfo? = null,
    manualIsLocked: Boolean = false,
    manualStatusMsg: String = "",
    hasFineLocation: Boolean = false,
    hasBackgroundLocation: Boolean = true,
    hasUsageStatsPermission: Boolean = true,
    hasOverlayPermission: Boolean = true,
    onRequestFineLocation: () -> Unit = {},
    onRequestBackgroundLocation: () -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    onManualLockChange: (Boolean) -> Unit = {},
    onManualStatusChange: (String) -> Unit = {},
    zones: List<Zone> = emptyList(),
    onSimulateEntry: (Zone) -> Unit = {},
    onManualBlockingStarted: (appName: String, durationMinutes: Int) -> Unit = { _, _ -> },
    graceTimerSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ── Derived state ─────────────────────────────────────────────────────────
    val isEnforcingZone = activeEnforcement != null
    val isGrace         = activeEnforcement?.isGracePeriod == true
    val isBlocking      = (isEnforcingZone && !isGrace) || manualIsLocked
    val zoneName        = activeEnforcement?.zoneName

    val lockState = when {
        isBlocking -> LockState.LOCKED
        isGrace    -> LockState.GRACE
        else       -> LockState.IDLE
    }

    val blockedAppsLabel = when {
        activeEnforcement != null && activeEnforcement.blockedApps.isNotEmpty() -> {
            val apps = activeEnforcement.blockedApps
            if (apps.size == 1) "${apps[0]} is blocked"
            else "${apps[0]} + ${apps.size - 1} more blocked"
        }
        manualIsLocked           -> "Instagram is blocked"
        isEnforcingZone && isGrace -> "Grace period — blocking soon"
        else                     -> "Waiting to enter a zone"
    }

    // ── Step 5: pulsing glow on the amber ring while LOCKED ──────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "ringGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue   = 0.70f,
        targetValue    = 1.00f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val ringFraction = if (isGrace) 0.75f else 1.0f
    val ringColor = when {
        isGrace    -> Amber.copy(alpha = 0.55f)
        isBlocking -> Amber.copy(alpha = glowAlpha)   // animated pulse
        else       -> Amber.copy(alpha = 0.25f)
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

        // ── Permission banners ────────────────────────────────────────────────
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
        if (!hasUsageStatsPermission) {
            UsageAccessBanner(
                onRequestUsageAccess = onRequestUsageAccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        if (!hasOverlayPermission) {
            OverlayPermissionBanner(
                onRequestPermission = onRequestOverlayPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // ── Zone banner ───────────────────────────────────────────────────────
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
                text  = "Currently in: ",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text       = zoneName ?: "No active zone",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (isEnforcingZone) Amber else TextMuted
            )
        }

        Spacer(Modifier.weight(1f))

        // ── Circular ring + animated centre ──────────────────────────────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val sw     = 14.dp.toPx()
                val stroke = Stroke(width = sw, cap = StrokeCap.Round)
                val inset  = sw / 2f
                val arcSz  = Size(size.width - inset * 2, size.height - inset * 2)
                val tl     = Offset(inset, inset)
                drawArc(
                    color = BgElevated, startAngle = -90f, sweepAngle = 360f,
                    useCenter = false, topLeft = tl, size = arcSz, style = stroke
                )
                drawArc(
                    color = ringColor, startAngle = -90f, sweepAngle = ringFraction * 360f,
                    useCenter = false, topLeft = tl, size = arcSz, style = stroke
                )
            }

            // Step 4: animate the ring centre content on lock-state change
            AnimatedContent(
                targetState  = lockState,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(tween(300), initialScale = 0.82f)) togetherWith
                    (fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.82f))
                },
                label = "ringCentre"
            ) { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state) {
                        LockState.GRACE -> {
                            Text(
                                text          = graceDisplay,
                                fontSize      = 44.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text          = "GRACE PERIOD",
                                fontSize      = 10.sp,
                                letterSpacing = 2.sp,
                                color         = TextMuted,
                                modifier      = Modifier.padding(top = 4.dp)
                            )
                        }
                        LockState.LOCKED -> {
                            Text(
                                text          = "LOCKED",
                                fontSize      = 26.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text          = "BLOCKING",
                                fontSize      = 10.sp,
                                letterSpacing = 2.sp,
                                color         = TextMuted,
                                modifier      = Modifier.padding(top = 4.dp)
                            )
                        }
                        LockState.IDLE -> {
                            Text(
                                text       = "—",
                                fontSize   = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextMuted
                            )
                            Text(
                                text          = "IDLE",
                                fontSize      = 10.sp,
                                letterSpacing = 2.sp,
                                color         = TextMuted.copy(alpha = 0.5f),
                                modifier      = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text     = blockedAppsLabel,
            fontSize = 14.sp,
            color    = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.weight(1f))

        // Status message (manual block feedback)
        if (manualStatusMsg.isNotEmpty() && !isEnforcingZone) {
            Text(
                text     = manualStatusMsg,
                fontSize = 12.sp,
                color    = if (manualStatusMsg.contains("not installed"))
                    MaterialTheme.colorScheme.error else Amber,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )
        }

        // ── Lock / Unlock button ───────────────────────────────────────────────
        Button(
            onClick = {
                if (manualIsLocked) {
                    AppBlockingManager.stopAllBlocking(context)
                    onManualStatusChange("")
                    onManualLockChange(false)
                } else if (!hasUsageStatsPermission) {
                    onManualStatusChange("Usage Access is required to block Instagram")
                    onRequestUsageAccess()
                } else if (!hasOverlayPermission) {
                    onManualStatusChange("Display over other apps is required to show the block screen")
                    onRequestOverlayPermission()
                } else {
                    val duration = graceTimerSeconds.coerceAtLeast(5)
                    val ok = AppBlockingManager.startBlockingApp(context, durationMinutes = duration)
                    if (ok) {
                        onManualStatusChange("✅ Instagram locked for $duration minutes")
                        onManualLockChange(true)
                        onManualBlockingStarted("Instagram", duration)
                    } else {
                        onManualStatusChange("❌ Instagram not installed")
                    }
                }
            },
            enabled = !isEnforcingZone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = if (manualIsLocked) MaterialTheme.colorScheme.error else Amber,
                contentColor           = if (manualIsLocked) Color.White else Color.Black,
                disabledContainerColor = BgElevated,
                disabledContentColor   = TextMuted.copy(alpha = 0.65f)
            )
        ) {
            Text(
                text = when {
                    isEnforcingZone -> "Locked by zone"
                    manualIsLocked  -> "Unlock Phone"
                    else            -> "Lock Phone"
                },
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isEnforcingZone) {
            Text(
                text = if (isGrace) {
                    "Zone protection will begin after the grace period and end when you leave."
                } else {
                    "This lock ends automatically when you leave ${zoneName ?: "the active zone"}."
                },
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 10.dp)
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Permission banners ────────────────────────────────────────────────────────

@Composable
private fun LocationPermissionBanner(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AmberDim, RoundedCornerShape(14.dp))
            .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Location required", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Amber)
            Text("Zones are inactive until location is granted.", fontSize = 11.sp, color = TextMuted)
        }
        TextButton(onClick = onRequestPermission) {
            Text("Enable", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackgroundLocationBanner(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(BgSurface, RoundedCornerShape(14.dp))
            .border(1.dp, BorderDim, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Background location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("Allow All the time for zones to trigger when the app is closed.", fontSize = 11.sp, color = TextMuted)
        }
        TextButton(onClick = onRequestPermission) {
            Text("Allow", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OverlayPermissionBanner(
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
            imageVector = Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = Amber,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Display over other apps",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
            Text(
                "Required to place the Bounds block screen over Instagram.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        TextButton(onClick = onRequestPermission) {
            Text("Enable", fontSize = 12.sp, color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}

