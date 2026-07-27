package com.example.bounds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .size(80.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(!isSelected) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = name,
            modifier = Modifier.size(32.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
