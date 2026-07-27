package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.ui.components.MapLocationPicker
import com.example.bounds.util.AppBlockingManager
import kotlin.math.roundToInt

@Composable
fun CurrentScreen(modifier: Modifier = Modifier) {
    var latitude by remember { mutableStateOf(40.7128) }
    var longitude by remember { mutableStateOf(-74.0060) }
    var sliderValue by remember { mutableStateOf(30f) }
    var isLocked by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Map with current location
        MapLocationPicker(
            latitude = latitude,
            longitude = longitude,
            onLocationChange = { newLat, newLon ->
                latitude = newLat
                longitude = newLon
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Time Slider
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Block Time",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "${sliderValue.roundToInt()} minutes",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    if (!isLocked) {
                        sliderValue = newValue
                    }
                },
                valueRange = 5f..480f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                enabled = !isLocked
            )

            Text(
                text = "5 min - 8 hours",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Status Message
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                fontSize = 12.sp,
                color = if (statusMessage.contains("not installed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // Lock Button
        Button(
            onClick = {
                val durationMinutes = sliderValue.roundToInt()
                if (isLocked) {
                    AppBlockingManager.stopBlockingApp(context)
                    statusMessage = ""
                    isLocked = false
                } else {
                    val success = AppBlockingManager.startBlockingApp(
                        context,
                        AppBlockingManager.getInstagramPackageName(),
                        durationMinutes
                    )
                    if (success) {
                        statusMessage = "✅ Instagram locked for $durationMinutes minutes"
                        isLocked = true
                    } else {
                        statusMessage = "❌ Instagram not installed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isLocked) "🔒 Unlock - Stop Blocking" else "🔓 Lock Instagram",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
