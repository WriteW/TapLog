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

        // 1. 大渐变 (Macro): 周期为 60 天
        val cycleLength = 60.0
        val phase = (dayOfYear % cycleLength) / cycleLength * 2 * Math.PI
        val baseHue = ((sin(phase) + 1) / 2 * 360).toFloat()

        // 2. 小渐变 (Micro): 基于小时微调
        val hourOffset = (hour - 12) * 2f
        val finalHue = (baseHue + hourOffset + 360) % 360

        return Color.hsl(finalHue, 0.6f, 0.5f)
    }

    // 【修改】：分组逻辑，排除 2*2 大图
    private fun groupEntries(entries: List<DailyEntry>): List<TimelineGroup> {
        if (entries.isEmpty()) return emptyList()
        val groups = mutableListOf<TimelineGroup>()
        var currentBatch = mutableListOf<DailyEntry>()

        entries.forEach { entry ->
            if (currentBatch.isEmpty()) {
                currentBatch.add(entry)
            } else {
                val lastEntry = currentBatch.last()
                val timeDiff = abs(lastEntry.timestamp - entry.timestamp)
                val isWithin10Min = timeDiff <= 10 * 60 * 1000

                // 判断是否为大尺寸图片 (宽图 或 大正方形)
                val isCurrentBig = entry.type == EntryType.IMAGE && (entry.imageRatio > 1.5f || entry.isLarge)
                val isLastBig = lastEntry.type == EntryType.IMAGE && (lastEntry.imageRatio > 1.5f || lastEntry.isLarge)

                // 如果是大图，就强制断开分组，单独占一行(或者作为一个Group的开头)
                // 这里逻辑是：如果都不是大图，且时间相近，才合并
                if (isWithin10Min && !isCurrentBig && !isLastBig) {
                    currentBatch.add(entry)
                } else {
                    groups.add(TimelineGroup(currentBatch.first().timestamp, currentBatch))
                    currentBatch = mutableListOf(entry)
                }
            }
        }
        if (currentBatch.isNotEmpty()) {
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

    // 【修改】：增加 isLarge 参数
    fun addImageEntry(uri: Uri, ratio: Float, isLarge: Boolean) {
        viewModelScope.launch {
            val path = repository.saveImage(uri)
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.IMAGE,
                content = path,
                imageRatio = ratio,
                isLarge = isLarge // 保存尺寸标记
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