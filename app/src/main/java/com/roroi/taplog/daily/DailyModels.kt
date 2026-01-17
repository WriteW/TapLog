package com.roroi.taplog.daily

import kotlinx.serialization.Serializable
import java.util.UUID

enum class EntryType {
    TEXT, IMAGE
}

@Serializable
data class CropParams(
    val userScale: Float = 1f,
    val userOffsetX: Float = 0f,
    val userOffsetY: Float = 0f
)

@Serializable
data class DailyEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val type: EntryType,
    val content: String,
    val imageRatio: Float = 1f,
    val isLarge: Boolean = false,
    val cropParams: CropParams? = null // 【新增】保存裁剪参数
)

data class TimelineGroup(
    val timestamp: Long,
    val items: List<DailyEntry>
)