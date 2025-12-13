package com.roroi.taplog.daily_ai

import kotlinx.serialization.Serializable
import java.util.UUID

enum class EntryType {
    TEXT, IMAGE
}

// 这里的 Enum 主要用于 UI Label，实际逻辑中我们增加了 isLarge 字段来控制
enum class ImageRatio(val ratio: Float, val label: String) {
    SQUARE(1f, "1:1"),
    WIDE(2f, "2:1"),
    LARGE(1f, "2:2") // 新增：比例虽是 1:1，但是大图
}

@Serializable
data class DailyEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val type: EntryType,
    val content: String,
    val imageRatio: Float = 1f,
    val isLarge: Boolean = false // 【新增】标记是否为 2*2 大图
)

data class TimelineGroup(
    val timestamp: Long,
    val items: List<DailyEntry>
)