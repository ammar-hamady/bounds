package com.example.bounds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bounds.ui.theme.Amber
import com.example.bounds.ui.theme.AmberDim
import com.example.bounds.ui.theme.BgElevated
import com.example.bounds.ui.theme.BorderDim
import com.example.bounds.ui.theme.TextMuted

@Composable
fun AppChip(
    name: String,
    icon: Painter,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) AmberDim else BgElevated)
            .border(
                width = 1.5.dp,
                color = if (isSelected) Amber else BorderDim,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect(!isSelected) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            modifier = Modifier.size(26.dp),
            tint = if (isSelected) Amber else TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Amber else TextMuted,
            maxLines = 1
        )
    }
}
