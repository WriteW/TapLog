package com.roroi.taplog.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Composable
fun ColorSelector(
    selectedColorArgb: Int?,
    onColorSelected: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PresetColors.forEach { color ->
            val isSelected = if (color == null) selectedColorArgb == null else color.toArgb() == selectedColorArgb

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(
                        if (color == null) {
                            // "随机"选项显示彩虹渐变
                            Modifier.background(
                                Brush.sweepGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta, Color.Red)
                                )
                            )
                        } else {
                            Modifier.background(color)
                        }
                    )
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color.Black.copy(alpha = 0.6f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color?.toArgb()) },
                contentAlignment = Alignment.Center
            ) {
                // 如果是 null (随机模式)，中间加个星星图标
                if (color == null) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Auto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}