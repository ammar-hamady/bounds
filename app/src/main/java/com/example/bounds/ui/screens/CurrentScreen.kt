package com.example.bounds.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgBanner
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.BorderDim
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.util.AppBlockingManager
import kotlin.math.roundToInt

@Composable
fun CurrentScreen(
    onBlockingStarted: (appName: String, durationMinutes: Int) -> Unit = { _, _ -> },
    graceTimerSeconds: Int = 0,
    currentZoneName: String? = null,
    modifier: Modifier = Modifier
) {
    var isLocked by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Grace period fraction (simulate full ring for preview; would count down in real impl)
    val graceFraction = if (graceTimerSeconds > 0) 0.85f else 1f
    val graceDisplay = if (graceTimerSeconds > 0) {
        val m = graceTimerSeconds / 60
        val s = graceTimerSeconds % 60
        if (m > 0) "$m:${s.toString().padStart(2, '0')}" else "0:${s.toString().padStart(2, '0')}"
    } else "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Currently in:" banner
        val bannerText = currentZoneName ?: "No active zone"
        val inZone = currentZoneName != null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(BgBanner, RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (inZone) Amber else TextMuted,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Text(
                text = if (inZone) "Currently in: " else "Currently in: ",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = bannerText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (inZone) Amber else TextMuted
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Circular grace period ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            val ringAmber = Amber
            val ringTrack = BgElevated
            Canvas(modifier = Modifier.size(240.dp)) {
                val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                val inset = 14.dp.toPx() / 2f
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)
                // Track
                drawArc(
                    color = ringTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                // Progress
                drawArc(
                    color = ringAmber,
                    startAngle = -90f,
                    sweepAngle = graceFraction * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (graceTimerSeconds > 0) graceDisplay else if (isLocked) "LOCKED" else "—",
                    fontSize = if (graceTimerSeconds > 0) 48.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (graceTimerSeconds > 0) "GRACE PERIOD" else "BLOCKING",
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status text
        Text(
            text = if (isLocked) "Instagram is blocked" else "Waiting to enter a zone",
            fontSize = 14.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Locked feed illustration
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
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isLocked) Amber else TextMuted,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (isLocked) "FEED\nLOCKED" else "FEED\nFREE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = if (isLocked) Amber.copy(alpha = 0.7f) else TextMuted.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status error/success message
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                fontSize = 12.sp,
                color = if (statusMessage.contains("not installed"))
                    MaterialTheme.colorScheme.error
                else Amber,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Unlock Phone button
        Button(
            onClick = {
                if (isLocked) {
                    AppBlockingManager.stopBlockingApp(context)
                    statusMessage = ""
                    isLocked = false
                } else {
                    val durationMinutes = (graceTimerSeconds / 60).coerceAtLeast(5)
                    val success = AppBlockingManager.startBlockingApp(
                        context,
                        AppBlockingManager.getInstagramPackageName(),
                        durationMinutes
                    )
                    if (success) {
                        statusMessage = "✅ Instagram locked for $durationMinutes minutes"
                        isLocked = true
                        onBlockingStarted("Instagram", durationMinutes)
                    } else {
                        statusMessage = "❌ Instagram not installed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked) MaterialTheme.colorScheme.error else Amber,
                contentColor = if (isLocked) Color.White else Color.Black
            )
        ) {
            Text(
                text = if (isLocked) "Unlock Phone" else "Unlock Phone",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulate Entry ghost button
        TextButton(
            onClick = { /* simulate zone entry */ },
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
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Simulate Entry",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
