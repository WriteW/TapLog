package com.roroi.taplog.daily_ai

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sin

class DailyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DailyRepository(application)

    private val _groupedEntries = MutableStateFlow<List<TimelineGroup>>(emptyList())
    val groupedEntries = _groupedEntries.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val entries = repository.getAllEntries()
            _groupedEntries.value = groupEntries(entries)
        }
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    // --- 颜色算法 ---
    fun getTimelineColor(timestamp: Long): Color {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. 大渐变 (Macro - 日期):
        // 周期 30 天，色相在 0(红) ~ 260(紫) 之间循环
        val cycleLength = 30.0
        val phase = (dayOfYear % cycleLength) / cycleLength * 2 * Math.PI
        val baseHue = ((kotlin.math.sin(phase) + 1) / 2 * 260).toFloat()

        // 2. 小渐变 (Micro - 小时):
        // 【关键修改】：大幅降低亮度 (Lightness)，确保在白色背景上文字清晰可见
        // HSL 中 Lightness 0.5 是纯色，>0.5 变白，<0.5 变黑。
        // 为了阅读，我们需要深色，所以 Lightness 必须小于 0.45

        val saturation: Float
        val lightness: Float

        when (hour) {
            in 5..11 -> {
                // 上午: 稍微明亮一点，但不能太亮 (如深橙、深青)
                saturation = 0.7f
                lightness = 0.4f  // 之前是 0.65 (太亮)，改为 0.4
            }
            in 12..17 -> {
                // 下午: 浓郁饱和 (如正红、深蓝)
                saturation = 0.9f
                lightness = 0.35f // 之前是 0.5，改为 0.35
            }
            else -> {
                // 晚上: 深沉幽暗 (如暗紫、墨绿)
                saturation = 0.6f
                lightness = 0.25f // 之前是 0.35，改为 0.25
            }
        }

        // 微调 Hue
        val hourHueOffset = (hour - 12) * 2f
        val finalHue = (baseHue + hourHueOffset + 360) % 360

        return Color.hsl(finalHue, saturation, lightness)
    }

    // 核心分组逻辑
    private fun groupEntries(entries: List<DailyEntry>): List<TimelineGroup> {
        if (entries.isEmpty()) return emptyList()

        val groups = mutableListOf<TimelineGroup>()
        var currentBatch = mutableListOf<DailyEntry>()

        // entries 本身已经是按时间倒序 (New -> Old)

        entries.forEach { entry ->
            if (currentBatch.isEmpty()) {
                currentBatch.add(entry)
            } else {
                val lastEntry = currentBatch.last()
                val timeDiff = abs(lastEntry.timestamp - entry.timestamp)
                val isWithin10Min = timeDiff <= 10 * 60 * 1000

                // 判断大图逻辑 (2x2 或 宽图)
                val isCurrentBig = entry.type == EntryType.IMAGE && (entry.imageRatio > 1.5f || entry.isLarge)
                val isLastBig = lastEntry.type == EntryType.IMAGE && (lastEntry.imageRatio > 1.5f || lastEntry.isLarge)

                if (isWithin10Min && !isCurrentBig && !isLastBig) {
                    currentBatch.add(entry)
                } else {
                    // 结算上一组
                    // 【关键点】：FlowRow 从左到右显示 List[0], List[1]...
                    // 我们要保证靠近左边(时间轴)的是最新的，所以 List[0] 必须是时间最大的。
                    // 正常逻辑下 currentBatch 已经是倒序的，但为了绝对稳健，这里 sort 一下
                    currentBatch.sortByDescending { it.timestamp }

                    groups.add(TimelineGroup(currentBatch.first().timestamp, currentBatch))
                    currentBatch = mutableListOf(entry)
                }
            }
        }
        if (currentBatch.isNotEmpty()) {
            currentBatch.sortByDescending { it.timestamp }
            groups.add(TimelineGroup(currentBatch.first().timestamp, currentBatch))
        }

        return groups
    }

    // CRUD
    fun addTextEntry(content: String) {
        viewModelScope.launch {
            val entry = DailyEntry(timestamp = System.currentTimeMillis(), type = EntryType.TEXT, content = content)
            repository.saveEntry(entry)
            loadData()
        }
    }

    fun updateEntry(entry: DailyEntry) {
        viewModelScope.launch {
            repository.saveEntry(entry)
            loadData()
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            val entry = _groupedEntries.value.flatMap { it.items }.find { it.id == entryId }
            if (entry != null) {
                repository.deleteEntry(entry)
                loadData()
            }
        }
    }

    fun addImageEntry(uri: Uri, ratio: Float, isLarge: Boolean, cropParams: CropParams) {
        viewModelScope.launch {
            val path = repository.saveImage(uri)
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.IMAGE,
                content = path,
                imageRatio = ratio,
                isLarge = isLarge,
                cropParams = cropParams // 保存参数
            )
            repository.saveEntry(entry)
            loadData()
        }
    }


    fun getFullImagePath(path: String): File = repository.getImagePath(path)

    // --- Data Management ---

    // 修改：接收 Uri 进行导出
    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportDataToUri(uri)
                _uiMessage.value = "Export successful"
            } catch (e: Exception) {
                e.printStackTrace()
                _uiMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.importDataPackage(uri)
                loadData()
                _uiMessage.value = "Import successful"
            } catch (e: Exception) {
                _uiMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            loadData()
            _uiMessage.value = "All data cleared"
        }
    }
}