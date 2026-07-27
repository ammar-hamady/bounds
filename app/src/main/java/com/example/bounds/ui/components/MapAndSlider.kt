package com.example.bounds.ui.components

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlin.math.roundToInt

@Composable
fun MapLocationPicker(
    latitude: Double,
    longitude: Double,
    onLocationChange: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentLat by remember { mutableStateOf(latitude) }
    var currentLng by remember { mutableStateOf(longitude) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectedLocation = LatLng(currentLat, currentLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 15f)
    }
    val markerState = rememberMarkerState(position = selectedLocation)

    LaunchedEffect(currentLat, currentLng) {
        markerState.position = LatLng(currentLat, currentLng)
        onLocationChange(currentLat, currentLng)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(currentLat, currentLng), 15f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Select Location",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Suggestions
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    showSuggestions = query.isNotEmpty()
                    if (query.isNotEmpty()) {
                        suggestions = searchLocationSuggestions(query, context)
                    } else {
                        suggestions = emptyList()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search address or location...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            suggestions = emptyList()
                            showSuggestions = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            // Suggestions Dropdown
            if (showSuggestions && suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(0.dp, 0.dp, 8.dp, 8.dp)
                        )
                        .padding(top = 48.dp)
                ) {
                    items(suggestions.take(5)) { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentLat = suggestion.latitude
                                    currentLng = suggestion.longitude
                                    searchQuery = suggestion.address
                                    showSuggestions = false
                                    suggestions = emptyList()
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = suggestion.address,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Current Location Button
        Button(
            onClick = {
                // Default to New York for now (in production, would use device location)
                currentLat = 40.7128
                currentLng = -74.0060
                searchQuery = "Current Location"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Return to Current Location")
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp)
                ),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                currentLat = latLng.latitude
                currentLng = latLng.longitude
                showSuggestions = false
            }
        ) {
            Marker(
                state = markerState,
                title = "Selected Location",
                snippet = "Lat: ${"%.4f".format(currentLat)}, Lon: ${"%.4f".format(currentLng)}"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Lat: ${"%.4f".format(currentLat)}, Lon: ${"%.4f".format(currentLng)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun RadiusSlider(
    radius: Int,
    onRadiusChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = "Radius",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${radius}m",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = radius.toFloat(),
            onValueChange = { onRadiusChange(it.toInt()) },
            valueRange = 0f..150f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

data class LocationSuggestion(
    val address: String,
    val latitude: Double,
    val longitude: Double
)

private fun searchLocationSuggestions(
    query: String,
    context: android.content.Context
): List<LocationSuggestion> {
    return try {
        val geocoder = Geocoder(context)
        val addresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 5)
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(query, 5)
        }

        addresses?.mapNotNull { address ->
            LocationSuggestion(
                address = address.getAddressLine(0) ?: return@mapNotNull null,
                latitude = address.latitude,
                longitude = address.longitude
            )
        } ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
