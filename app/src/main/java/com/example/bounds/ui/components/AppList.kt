package com.example.bounds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.R
import com.example.bounds.model.App
import com.example.bounds.ui.theme.BgSurface
import com.example.bounds.ui.theme.TextSubtle

fun defaultAppList(selectedAppNames: List<String>): List<App> {
    return listOf(
        App("1", "Instagram", "ic_home", selectedAppNames.contains("Instagram")),
        App("2", "TikTok", "ic_home", selectedAppNames.contains("TikTok")),
        App("3", "Twitter", "ic_home", selectedAppNames.contains("Twitter")),
        App("4", "Discord", "ic_home", selectedAppNames.contains("Discord")),
        App("5", "YouTube", "ic_home", selectedAppNames.contains("YouTube")),
        App("6", "Facebook", "ic_home", selectedAppNames.contains("Facebook")),
        App("7", "Reddit", "ic_home", selectedAppNames.contains("Reddit")),
        App("8", "Telegram", "ic_home", selectedAppNames.contains("Telegram"))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppList(
    apps: List<App>,
    onAppSelectionChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = "BLOCK THESE APPS"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgSurface)
            .padding(16.dp)
    ) {
        title?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = TextSubtle,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.forEach { app ->
                AppChip(
                    name = app.name,
                    icon = painterResource(R.drawable.ic_home),
                    isSelected = app.isSelected,
                    onSelect = { isSelected ->
                        onAppSelectionChange(app.id, isSelected)
                    }
                )
            }
        }
    }
}
