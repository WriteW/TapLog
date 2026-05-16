package com.roroi.taplog.daily

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlin.math.abs

/**
 * 基于主色自动生成搭配的深色辅助色
 * @param primaryColor 主色（鲜艳色）
 * @return 颜色对 [primaryColor, darkAccentColor]
 */
fun generateColorPair(primaryColor: Color): Pair<Color, Color> {
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsl)

    val hue = hsl[0]
    val saturation = hsl[1]

    // 生成深色辅助色的规则：
    // 1. 保持相似色相（同色系深色）或转到对比色相

    // 方案A：同色系深色（降低亮度，略微降低饱和度）
    val darkAccent = Color.hsv(
        hue = hue,
        saturation = (saturation * 0.7f).coerceIn(0.3f, 0.9f),
        value = 0.35f  // 固定深色亮度
    )
    return Pair(primaryColor, darkAccent)
}

fun generateColorPalette(primaryColor: Color, count: Int = 4): List<Color> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsv)

    val baseHue = hsv[0]      // 色相 (0-360)
    val baseSat = hsv[1]      // 饱和度 (0-1)
    val baseVal = hsv[2]      // 亮度 (0-1)

    return List(count) { index ->
        // 1. 色相偏移：生成邻近色（每步偏移 15~20 度，创造多色融合感）
        // 例如：如果是黄色，会生成橘黄、柠檬黄、黄绿等相邻颜色，极其自然
        val hueShift = (index - count / 2f) * 20f
        val newHue = (baseHue + hueShift + 360) % 360

        // 2. 饱和度保护：避免太灰变脏，保持色彩鲜艳
        val newSat = (baseSat - abs(index - count / 2f) * 0.08f).coerceIn(0.5f, 1f)

        // 3. 亮度微调：不要降得太低，防止出现“死黑色”或“泥巴色”
        val newVal = (baseVal + (index % 2 * 0.1f)).coerceIn(0.7f, 1f)

        Color.hsv(newHue, newSat, newVal)
    }
}
// 将 enum class 改为 data class 以支持动态构造
data class DailyTimeTheme(
    val backgroundColor: Color,
    val primaryColor: Color,
    val isDark: Boolean = false,
    val onSurfaceColor: Color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF455A64),
    val ballColors: List<Color> // 小球颜色池
) {
    companion object {
        // 预定义的静态实例，模拟之前的枚举行为
        val MORNING = DailyTimeTheme(
            backgroundColor = Color(0xFFE3F2FD),
            primaryColor = Color(0xFF2196F3),
            onSurfaceColor = Color(0xFF455A64),
            ballColors = listOf(Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFF42A5F5), Color(0xFFBBDEFB))
        )
        val NOON = DailyTimeTheme(
            backgroundColor = Color(0xFFFFFDE7),
            primaryColor = Color(0xFFFBC02D),
            onSurfaceColor = Color(0xFF5D4037),
            ballColors = listOf(Color(0xFFFFF59D), Color(0xFFFFE082), Color(0xFFFFD54F), Color(0xFFFFF9C4))
        )
        val AFTERNOON = DailyTimeTheme(
            backgroundColor = Color(0xFFE8F5E9),
            primaryColor = Color(0xFF4CAF50),
            onSurfaceColor = Color(0xFF33691E),
            ballColors = listOf(Color(0xFFA5D6A7), Color(0xFF81C784), Color(0xFF66BB6A), Color(0xFFC8E6C9))
        )
        val EVENING = DailyTimeTheme(
            backgroundColor = Color(0xFFFCE4EC),
            primaryColor = Color(0xFFE91E63),
            onSurfaceColor = Color(0xFF880E4F),
            ballColors = listOf(Color(0xFFF48FB1), Color(0xFFF06292), Color(0xFFEC407A), Color(0xFFF8BBD0))
        )

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