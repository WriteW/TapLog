package com.roroi.taplog.daily.viewmodel

import androidx.compose.ui.graphics.Color
import com.roroi.taplog.daily.GoldenYellow
import kotlinx.serialization.Serializable
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class EntryType {
    TEXT, IMAGE, AUDIO, RECORD // [修改1] 新增 AUDIO 和 RECORD
}
// [修改11] 新增用于一整天记录的事件结构
@Serializable
data class RecordEvent(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val iconOrText: String = ""
)

@Serializable
data class RecordDayData(
    val events: List<RecordEvent> = emptyList(),
    val isStopped: Boolean = false,
    // 👇 [新增这一行] 用来把按钮配置和当天的记录绑在一起保存
    val customEvents: List<String> = emptyList()
) {
    fun toPlainText(events: List<Pair<String, String>>): String {
        val sdf = DateFormat.getDateInstance(DateFormat.LONG)
        var result = sdf.format(Date(this.events.first().startTime))
        val timeFm = SimpleDateFormat("HH:mm", Locale.US)
        this.events.forEach { event ->
            val start = timeFm.format(Date(event.startTime))
            val end = if (event.endTime == null) {
                "Now"
            } else {
                timeFm.format(event.endTime)
            }
            result += "\n${event.iconOrText} ${events.find {event.iconOrText == it.first}?.second ?: ""}: $start - $end"
        }
        return result
    }
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
    val title: String? = null,
    val content: String,
    val imageRatio: Float = 1f,
    val isLarge: Boolean = false,
    val cropParams: CropParams? = null,
    val isPin: Boolean = false,
    // 【新增】：手动分组ID，拥有相同此ID的条目会被强制绑在一个 TimelineGroup
    val manualGroupId: String? = null,
    val capsuleId: String? = null
)

@Serializable
data class EntryComment(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val content: String,
    val likes: List<DLike>,
    val who: DUser
)

@Serializable
data class DLike(
    val dUserId: String
)

@Serializable
data class DUser(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val profile: String? = null
)

@Serializable
data class TimeCapsule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Int,
    val createdAt: Long,
    val openAt: Long,
    val entryIds: List<String>,
    val isViewed: Boolean = false
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
