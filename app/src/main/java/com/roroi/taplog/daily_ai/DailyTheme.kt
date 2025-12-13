package com.roroi.taplog.daily_ai

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 暖黄色调，不要太黄
val WarmYellowBg = Color(0xFFFFFBE6) // 极浅的暖黄
val WarmYellowSurface = Color(0xFFFFE082) // 稍微深一点的装饰色
val TextColor = Color(0xFF333333)
val TimelineGray = Color(0xFFE0E0E0)
val TimelineDotColor = Color(0xFFBDBDBD)

private val DailyColorScheme = lightColorScheme(
    primary = Color(0xFFF9A825), // 暖金
    onPrimary = Color.White,
    secondary = Color(0xFFFFF176),
    background = Color.White,
    surface = Color.White,
    surfaceVariant = WarmYellowBg, // 用于Editor背景
    onSurface = TextColor
)

@Composable
fun DailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DailyColorScheme,
        content = content
    )
}