package com.roroi.taplog.daily.viewmodel

import androidx.compose.ui.graphics.Color
import com.roroi.taplog.daily.GoldenYellow
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
    val cropParams: CropParams? = null, // 【新增】保存裁剪参数
    val isPin: Boolean = false
)

data class TimelineGroup(
    val timestamp: Long,
    val items: List<DailyEntry>,
)

fun TimelineGroup.isPin() = items.any { it.isPin }
fun TimelineGroup.getDotColor() = if (isPin()) GoldenYellow else Color.White

// 【新增】：将判断能否并列显示的逻辑内聚，之后加小录音卡片可直接在这修改
fun DailyEntry.canDisplayInline(): Boolean =
    this.type == EntryType.IMAGE && !this.isLarge && this.imageRatio < 1.5f

// 【新增】：是否支持传送门按钮
fun DailyEntry.supportPortal(): Boolean =
    this.type == EntryType.IMAGE