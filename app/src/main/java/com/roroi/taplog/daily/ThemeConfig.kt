package com.roroi.taplog.daily

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar

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

    /**
 * 基于主色生成一组 Material 风格的渐变色
 * @param primaryColor 主色（最深色）
 * @param count 生成颜色数量，默认4个
 * @return 从浅到深排列的颜色列表
 */
fun generateColorPalette(primaryColor: Color, count: Int = 4): List<Color> {
    // 提取 HSL 分量
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsl)

    val hue = hsl[0]      // 色相 (0-360)
    val saturation = hsl[1]  // 饱和度 (0-1)

    // 根据主色的亮度确定深浅变体
    // 亮度越高(接近白色) -> 主色偏浅；亮度越低(接近黑色) -> 主色偏深
    val value = hsl[2].coerceIn(0.2f, 0.8f)  // 主色亮度范围限制

    return List(count) { index ->
        // index: 0最浅, count-1最深(主色)
        val lightness = when (index) {
            0 -> (value + 0.3f).coerceAtMost(0.95f)  // 最浅：+30%亮度
            1 -> (value + 0.15f).coerceAtMost(0.85f) // 次浅：+15%亮度
            2 -> (value - 0.15f).coerceAtLeast(0.35f) // 次深：-15%亮度
            else -> value  // 主色
        }

        Color.hsv(hue, saturation, lightness)
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