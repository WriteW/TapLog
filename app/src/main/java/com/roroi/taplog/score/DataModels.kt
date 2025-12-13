package com.roroi.taplog.score

import androidx.compose.ui.graphics.Color
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
    val title: String,
    val description: String,
    val price: Int
) {
    // 颜色逻辑保持不变，不序列化
    fun getColor(): Color {
        return when {
            price <= 30 -> Difficulty.EASY.getColor()
            price <= 60 -> Difficulty.NORMAL.getColor()
            price <= 120 -> Difficulty.HARD.getColor()
            else -> Difficulty.EPIC.getColor()
        }
    }
}

@Serializable
data class TaskScore(
    val score: Int = 0,
    val dScore: Int = 0
)