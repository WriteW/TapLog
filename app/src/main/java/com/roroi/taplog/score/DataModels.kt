package com.roroi.taplog.score

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import java.util.UUID

// ===========================
// 枚举类 (添加 Serializable)
// ===========================

@Serializable
enum class TaskType {
    ONE_TIME,
    HABIT
}

@Serializable
enum class Difficulty(val multiplier: Float) {
    EASY(1.0f),
    NORMAL(1.5f),
    HARD(2.0f),
    EPIC(5.0f);
}

// 扩展方法：获取难度对应颜色 (UI 逻辑不序列化)
fun Difficulty.getColor(): Color {
    return when (this) {
        Difficulty.EASY -> Color(0xFF4CAF50)
        Difficulty.NORMAL -> Color(0xFF2196F3)
        Difficulty.HARD -> Color(0xFFFF9800)
        Difficulty.EPIC -> Color(0xFF9C27B0)
    }
}

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var description: String = "",

    var income: Int = 10,
    var difficulty: Difficulty = Difficulty.NORMAL,
)

@Serializable
data class Goods(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val price: Int,
    var colorArgb: Int? = null,
) {
    // 颜色逻辑保持不变，不序列化
    fun getColor(): Color {
        return Color(colorArgb ?: PresetColors.drop(1).random()!!.toArgb())
    }
}

// 4. 定义一组预设颜色供选择器使用
val PresetColors = listOf(
    null, // 代表 "随机/自动"
    Color(0xFFF44336), // 红
    Color(0xFFFF9800), // 橙
    Color(0xFFFFEB3B), // 黄
    Color(0xFF4CAF50), // 绿
    Color(0xFF2196F3), // 蓝
    Color(0xFF9C27B0), // 紫
    Color(0xFFE91E63), // 粉
    Color(0xFF795548), // 棕
    Color(0xFF607D8B)  // 灰
)
@Serializable
data class TaskScore(
    val score: Int = 0,
    val dScore: Int = 0
)