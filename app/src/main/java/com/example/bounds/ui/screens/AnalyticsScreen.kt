package com.example.bounds.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.TextMuted
import com.example.bounds.ui.theme.TextSubtle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Zone breakdown colours ────────────────────────────────────────────────────
private val ZoneColors = listOf(
    Amber,
    Color(0xFF6B8EFF),
    Color(0xFF4CAF82),
    Color(0xFFE57373),
    Color(0xFFBA68C8)
)

@Composable
fun AnalyticsScreen(
    events: List<AnalyticsEvent>,
    modifier: Modifier = Modifier
) {
    val totalSessions  = events.size
    val totalMinutes   = events.sumOf { it.durationMinutes }
    val topApp         = events.groupingBy { it.appName }.eachCount()
        .maxByOrNull { it.value }?.key ?: "—"
    val streak         = 3 // placeholder
    val dayBars        = remember(events) { buildDayBars(events) }
    val byZone         = remember(events) {
        events.groupBy { it.zoneName }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(5)
    }
    val byZoneTotal    = byZone.sumOf { it.value }.coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // "Coming soon" banner
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Amber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Full analytics coming soon",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Preview of what's being tracked",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }
        }

        if (events.isEmpty()) {
            item { EmptyAnalyticsState() }
        } else {
            // 2×2 stat card grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        emoji = "🛡️",
                        value = formatMinutes(totalMinutes),
                        label = "Time protected",
                        period = "this week",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        emoji = "📍",
                        value = "$totalSessions",
                        label = "Zones triggered",
                        period = "this week",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        emoji = "✅",
                        value = "$totalSessions",
                        label = "Unlocks avoided",
                        period = "this week",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        emoji = "🔥",
                        value = "$streak days",
                        label = "Streak",
                        period = "current",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Bar chart — always shown (shows zeros if no events)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screen time blocked",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PreviewBadge()
                }
                Spacer(Modifier.height(16.dp))
                WeekBarChart(dayBars = dayBars)
            }
        }

        // By zone breakdown
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BgSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By zone",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PreviewBadge()
                }

                if (byZone.isEmpty()) {
                    // placeholder rows matching the mockup
                    listOf("Bedroom" to 48, "Dinner Table" to 31, "Office" to 21)
                        .forEachIndexed { i, (name, pct) ->
                            ZoneBreakdownRow(
                                name = name,
                                pct = pct,
                                color = ZoneColors.getOrElse(i) { Amber }
                            )
                        }
                } else {
                    byZone.forEachIndexed { i, entry ->
                        val pct = (entry.value * 100 / byZoneTotal)
                        ZoneBreakdownRow(
                            name = entry.key,
                            pct = pct,
                            color = ZoneColors.getOrElse(i) { Amber }
                        )
                    }
                }
            }
        }

        // Recent sessions
        if (events.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Sessions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(events.sortedByDescending { it.timestampMs }) { event ->
                SessionRow(event = event)
            }
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun PreviewBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AmberDim)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "PREVIEW",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Amber
        )
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    period: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(
            text = period,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Amber
        )
    }
}

@Composable
private fun ZoneBreakdownRow(name: String, pct: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = name,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        // Progress bar
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(BgElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct / 100f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
        Text(
            text = "$pct%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
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
            tint = Amber.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No sessions yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Block an app from the Current tab\nto see your stats here.",
            fontSize = 14.sp,
            color = TextSubtle,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WeekBarChart(dayBars: List<DayBar>) {
    val maxCount   = dayBars.maxOfOrNull { it.count }.let { if (it == null || it == 0) 1 else it }
    val barMaxHeightDp = 80

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
                        color = Amber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                }
                val fraction  = bar.count.toFloat() / maxCount
                val barHeight = if (bar.count > 0)
                    (barMaxHeightDp * fraction.coerceAtLeast(0.08f)).dp
                else 4.dp

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (bar.count > 0) Amber else BgElevated)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = bar.label,
                    fontSize = 10.sp,
                    color = TextSubtle
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
            .clip(RoundedCornerShape(12.dp))
            .background(BgSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AmberDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
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
                color = TextMuted
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMinutes(event.durationMinutes),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Amber
            )
            Text(
                text = formatTimestamp(event.timestampMs),
                fontSize = 11.sp,
                color = TextSubtle
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

data class DayBar(val label: String, val count: Int)

private fun buildDayBars(events: List<AnalyticsEvent>): List<DayBar> {
    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val keyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val counts = events.groupBy { keyFmt.format(Date(it.timestampMs)) }.mapValues { it.value.size }
    return (6 downTo 0).map { ago ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -ago) }
        DayBar(
            label = if (ago == 0) "Today" else dayFmt.format(cal.time),
            count = counts[keyFmt.format(cal.time)] ?: 0
        )
    }
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ts))
