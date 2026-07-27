package com.example.bounds.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.model.AnalyticsEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    events: List<AnalyticsEvent>,
    modifier: Modifier = Modifier
) {
    val totalSessions = events.size
    val totalMinutes = events.sumOf { it.durationMinutes }
    val topApp = events.groupingBy { it.appName }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: "—"

    // Build last-7-days bar chart data
    val dayBars = remember(events) { buildDayBars(events) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your blocking activity at a glance",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (events.isEmpty()) {
            item { EmptyAnalyticsState() }
        } else {
            item {
                // Summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        label = "Sessions",
                        value = "$totalSessions",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Time Blocked",
                        value = formatMinutes(totalMinutes),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        label = "Top App",
                        value = topApp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // 7-day bar chart
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Last 7 Days",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeekBarChart(dayBars = dayBars)
                }
            }

            item {
                Text(
                    text = "Recent Sessions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(events.sortedByDescending { it.timestampMs }) { event ->
                SessionRow(event = event)
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No sessions yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Block an app from the Current tab\nto see your stats here.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeekBarChart(dayBars: List<DayBar>) {
    val maxCount = dayBars.maxOfOrNull { it.count } ?: 1
    val barMaxHeight = 80.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        dayBars.forEach { bar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (bar.count > 0) {
                    Text(
                        text = "${bar.count}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val fraction = if (maxCount > 0) bar.count.toFloat() / maxCount else 0f
                val barHeight = if (bar.count > 0) (barMaxHeight.value * fraction.coerceAtLeast(0.1f)).dp else 4.dp
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .background(
                            color = if (bar.count > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bar.label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SessionRow(event: AnalyticsEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = event.zoneName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMinutes(event.durationMinutes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatTimestamp(event.timestampMs),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

data class DayBar(val label: String, val count: Int)

private fun buildDayBars(events: List<AnalyticsEvent>): List<DayBar> {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val today = Calendar.getInstance()

    val countsByDay = events.groupBy { dayKeyFormat.format(Date(it.timestampMs)) }
        .mapValues { it.value.size }

    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val key = dayKeyFormat.format(cal.time)
        val label = if (daysAgo == 0) "Today" else dayFormat.format(cal.time)
        DayBar(label = label, count = countsByDay[key] ?: 0)
    }
}

private fun formatMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun formatTimestamp(ts: Long): String {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return fmt.format(Date(ts))
}
