package com.roroi.taplog.daily

import androidx.compose.ui.graphics.Color
import java.util.Calendar

enum class DailyTimeTheme(
    val backgroundColor: Color,
    val primaryColor: Color,
    val onSurfaceColor: Color,
    val ballColors: List<Color> // 小球颜色池
) {
    MORNING(
        backgroundColor = Color(0xFFE3F2FD), // 浅蓝
        primaryColor = Color(0xFF2196F3),
        onSurfaceColor = Color(0xFF455A64),
        ballColors = listOf(
            Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFF42A5F5), Color(0xFFBBDEFB)
        )
    ),
    NOON(
        backgroundColor = Color(0xFFFFFDE7), // 浅黄
        primaryColor = Color(0xFFFBC02D),
        onSurfaceColor = Color(0xFF5D4037),
        ballColors = listOf(
            Color(0xFFFFF59D), Color(0xFFFFE082), Color(0xFFFFD54F), Color(0xFFFFF9C4)
        )
    ),
    AFTERNOON(
        backgroundColor = Color(0xFFE8F5E9), // 浅绿
        primaryColor = Color(0xFF4CAF50),
        onSurfaceColor = Color(0xFF33691E),
        ballColors = listOf(
            Color(0xFFA5D6A7), Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFFC8E6C9)
        )
    ),
    EVENING(
        backgroundColor = Color(0xFFFCE4EC), // 浅粉
        primaryColor = Color(0xFFE91E63),
        onSurfaceColor = Color(0xFF880E4F),
        ballColors = listOf(
            Color(0xFFF48FB1), Color(0xFFF06292), Color(0xFFEC407A), Color(0xFFF8BBD0)
        )
    );

    companion object {
        fun getCurrent(): DailyTimeTheme {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10 -> MORNING
                in 11..13 -> NOON
                in 14..18 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}