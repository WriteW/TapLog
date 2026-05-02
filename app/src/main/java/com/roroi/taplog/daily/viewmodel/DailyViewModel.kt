package com.roroi.taplog.daily.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roroi.taplog.daily.DailyTimeTheme
import com.roroi.taplog.daily.generateColorPair
import com.roroi.taplog.daily.generateColorPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

class DailyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DailyRepository(application)
    private val _groupedEntries = MutableStateFlow<List<TimelineGroup>>(emptyList())
    val groupedEntries = _groupedEntries.asStateFlow()
    private val _editorState = MutableStateFlow(EditorState())
    val editorState = _editorState.asStateFlow()
    fun startEditing(entryId: String?) {
        val entry = getEntryFromId(entryId)

        _editorState.value = if (entry != null) {
            EditorState(
                sessionId = System.currentTimeMillis(), // 新增：每次进入分配新 ID
                entryId = entry.id,
                originalText = entry.content,
                editingText = entry.content,
                isDirty = false,
                isNew = false
            )
        } else {
            EditorState(
                sessionId = System.currentTimeMillis() // 新增：每次新建分配新 ID
            )
        }
    }
    fun onEditorTextChange(text: String) {
        _editorState.update {
            it.copy(
                editingText = text,
                isDirty = text != it.originalText
            )
        }
    }
    fun saveEditor(onDone: () -> Unit) {
        val state = _editorState.value

        if (state.editingText.isBlank()) return

        viewModelScope.launch {
            if (state.isNew) {
                addTextEntry(state.editingText)
            } else {
                val entry = getEntryFromId(state.entryId) ?: return@launch
                updateEntry(entry.copy(content = state.editingText))
            }

            onDone()
        }
    }
    fun deleteEditor(onDone: () -> Unit) {
        val id = _editorState.value.entryId ?: return

        deleteEntry(id)
        onDone()
    }
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    fun toastOut(text: String) {
        _uiMessage.value = text
    }

    var selectedEntryId: String? by mutableStateOf(null)
        private set // 长按所选中

    fun getEntryFromId(entryId: String?): DailyEntry? {
        return groupedEntries.value.flatMap { it.items }.find { it.id == entryId }
    }

    var isBatchManaging by mutableStateOf(false)
        private set
    val batchEntries = mutableStateListOf<String>()
    fun startBatchSelecting() {
        isBatchManaging = true
    }
    fun stopBatchSelecting() {
        isBatchManaging = false
        batchEntries.clear()
    }

    // 存储空间相关状态
    var spaces by mutableStateOf<List<DSpace>>(emptyList())
        private set
    var selectedDSpaceId: String? = null
        private set
    var spaceDestination: String? = null
        private set

    var showLoadingDialog: Boolean by mutableStateOf(false)

    fun getThemeBySpace(): DailyTimeTheme {
        spaces.find { it.id == selectedDSpaceId }?.let {
            return DailyTimeTheme(
                Color(it.colorBgArgb),
                Color(it.colorBallArgb),
                generateColorPair(Color(it.colorBallArgb)).second,
                generateColorPalette(Color(it.colorBallArgb)),
                it.isDark
            )
        }
        return DailyTimeTheme.getCurrent()
    }

    var showChangePassword: Boolean by mutableStateOf(false)
    var showSelectSpaceM: Boolean by mutableStateOf(false)

    fun getSpaceFromId(spaceId: String?): DSpace? {
        return spaces.find { it.id == spaceId }
    }

    fun setSDestination(spaceId: String?) {
        spaceDestination = spaceId
    }

    fun moveEntry(originalSpaceId: String?, targetSpaceId: String?) {
        if (selectedEntryId.isNullOrBlank() && batchEntries.isEmpty()) return
        // 1. 启动单一协程处理所有类型
        viewModelScope.launch {
            try {
                // 设置 UI 为加载中状态
                showLoadingDialog = true

                if (batchEntries.isEmpty() && selectedEntryId != null) batchEntries.add(selectedEntryId.toString())

                batchEntries.forEach { entryId ->
                    val entry = getEntryFromId(entryId)
                    entry?.let {
                        when (entry.type) {
                            EntryType.TEXT -> {
                                repository.moveTextEntry(originalSpaceId, targetSpaceId, entry)
                            }
                            EntryType.IMAGE -> {
                                // 注意：根据你之前的定义，Repository 里的方法名是 moveImage
                                repository.moveImageEntry(originalSpaceId, targetSpaceId, entry)
                            }
                        }
                    }
                }

                // 2. 移动成功后统一刷新数据
                loadData()

            } catch (e: Exception) {
                // 3. 错误处理：记录日志或通过 Channel 发送弹窗通知
                e.printStackTrace()
                // _errorEvents.send("移动失败: ${e.message}")
            } finally {
                // 4. 关闭加载状态
                showLoadingDialog = false
            }
        }
    }

    fun exitToMainSpace() {
        if (selectedDSpaceId != null) {
            selectedDSpaceId = null
            viewModelScope.launch {
                showLoadingDialog = true
                loadData()
                showLoadingDialog = false
            }
        }
    }

    fun changeEntryFId(entryId: String, spaceId: String) {
        viewModelScope.launch {
            val space = getSpaceFromId(spaceId) ?: return@launch
            val updatedSpace = space.copy(entryId = entryId)
            // 直接调用 repository 保存，避免 addSpace 中的重复检查
            repository.saveSpace(updatedSpace)
            // 刷新内存中的 spaces 列表
            loadSpaces()
            // 可选：提示成功
            toastOut("入口已更换")
        }
    }

    fun changeSpaceP(newSpace: DSpace) {
        val updatedSpace = newSpace

        // 建议：如果 saveSpace 是耗时操作，也可以考虑放进协程
        repository.saveSpace(updatedSpace)
        loadSpaces()

        viewModelScope.launch {
            showLoadingDialog = true
            loadData()
            showLoadingDialog = false
        }
    }

    fun hasSpace(entryId: String): Boolean {
        return spaces.any { it.entryId == entryId }
    }

    fun addSpace(space: DSpace) {
        if (spaces.any { it.id == space.id }) return
        repository.saveSpace(space)
        loadSpaces()
    }

    fun delSpace(spaceId: String) {
        viewModelScope.launch {
            repository.delSpaceData(spaceId)
            loadSpaces().join()
        }
    }

    fun loadSpaces() =
        viewModelScope.launch {
            spaces = repository.loadSpace()
        }

    fun changeSpace() {
        val spaceId = spaceDestination
        if (spaceId == null) return
        Log.d("DailyViewModel", "Switching to space: $spaceId")
        val spaceToChange = spaces.find { it.id == spaceId }
        if (spaceToChange == null) return
        selectedDSpaceId = spaceId
        viewModelScope.launch {
            showLoadingDialog = true
            loadData()
            delay(500)
            showLoadingDialog = false
        }
    }

    // 页面切换发射
    val navigationEvent = MutableSharedFlow<Pair<String, String>>()

    // 图片查看器状态
    var viewingImageEntry by mutableStateOf<DailyEntry?>(null)
        private set
    var showPasswordCheck by mutableStateOf(false)

    init {
        loadSpaces()
        viewModelScope.launch {
            loadData()
        }
    }

    fun dismissShowImage() {
        viewingImageEntry = null
    }

    fun navigateToEditor(id: String?) {
        viewModelScope.launch {
            navigationEvent.emit(Pair("editor", if (id == null) "editor" else "editor?id=$id"))
        }
    }

    fun navigateToImagePicker() {
        viewModelScope.launch {
            navigationEvent.emit(Pair("imagePicker", ""))
        }
    }

    fun navigateToPortal(entryId: String) {
        viewModelScope.launch {
            navigationEvent.emit(Pair("portal", "portal?id=$entryId"))
        }
    }

    fun navigatePop() {
        viewModelScope.launch {
            navigationEvent.emit(Pair("pop", ""))
        }
    }

    fun showImage(entry: DailyEntry) {
        viewingImageEntry = entry
    }

    fun selectEntry(entryId: String?) {
        selectedEntryId = entryId
    }

    fun unFocusEntry() {
        selectedEntryId = null
    }

    suspend fun loadData() {
        val entries = repository.getAllEntries(selectedDSpaceId)
        val newEntries = entries.sortedWith(
            compareByDescending<DailyEntry> { it.isPin }
                .thenByDescending { it.timestamp }
        )
        _groupedEntries.value = groupEntries(newEntries)

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

        val cycleLength = 30.0
        val phase = (dayOfYear % cycleLength) / cycleLength * 2 * Math.PI
        val baseHue = ((sin(phase) + 1) / 2 * 260).toFloat()

        val saturation: Float
        val lightness: Float

        when (hour) {
            in 5..11 -> {
                saturation = 0.7f
                lightness = 0.4f
            }

            in 12..17 -> {
                saturation = 0.9f
                lightness = 0.35f
            }

            else -> {
                saturation = 0.6f
                lightness = 0.25f
            }
        }

        val hourHueOffset = (hour - 12) * 2f
        val finalHue = (baseHue + hourHueOffset + 360) % 360

        return Color.hsl(finalHue, saturation, lightness)
    }

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

                val isCurrentBig =
                    entry.type == EntryType.IMAGE && (entry.imageRatio > 1.5f || entry.isLarge)
                val isLastBig =
                    lastEntry.type == EntryType.IMAGE && (lastEntry.imageRatio > 1.5f || lastEntry.isLarge)

                // 修改点：检查两者的置顶状态是否相同，而不是直接拒绝所有的置顶项
                val isSamePinState = lastEntry.isPin == entry.isPin

                // 使用 isSamePinState 替换掉原来的 !isPin
                if (isWithin10Min && !isCurrentBig && !isLastBig && isSamePinState) {
                    currentBatch.add(entry)
                } else {
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

    fun addTextEntry(content: String) {
        viewModelScope.launch {
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.TEXT,
                content = content
            )
            repository.saveEntry(entry, selectedDSpaceId)
            loadData()
        }
    }

    fun updateEntry(entry: DailyEntry) {
        viewModelScope.launch {
            repository.saveEntry(entry, selectedDSpaceId)
            loadData()
        }
    }

    fun toggleEntryPin(entryId: String) {
        viewModelScope.launch {
            val entry = _groupedEntries.value.flatMap { it.items }.find { it.id == entryId }
            if (entry != null) {
                val newEntry = entry.copy(isPin = !entry.isPin)
                updateEntry(newEntry)
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            val entry = _groupedEntries.value.flatMap { it.items }.find { it.id == entryId }
            if (entry != null) {
                repository.deleteEntry(entry, selectedDSpaceId)
                loadData()
            }
        }
    }

    fun addImageEntry(uri: Uri, ratio: Float, isLarge: Boolean, cropParams: CropParams) {
        viewModelScope.launch {
            val path = repository.saveImage(uri, selectedDSpaceId)
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.IMAGE,
                content = path,
                imageRatio = ratio,
                isLarge = isLarge,
                cropParams = cropParams
            )
            repository.saveEntry(entry, selectedDSpaceId)
            loadData()
        }
    }

    fun getFullImagePath(path: String): File = repository.getImagePath(path, selectedDSpaceId)

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiMessage.value = "Exporting..."
                repository.exportDataToUri(uri, selectedDSpaceId)
                _uiMessage.value = "Export successful!"
            } catch (e: Exception) {
                e.printStackTrace()
                _uiMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiMessage.value = "Importing..."
                repository.importDataPackage(uri, selectedDSpaceId)
                loadData()
                _uiMessage.value = "Import successful!"
            } catch (e: Exception) {
                _uiMessage.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData(selectedDSpaceId)
            loadData()
            _uiMessage.value = "All data cleared"
        }
    }
}

data class TransformData(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

fun calculateTransform(
    imageSize: IntSize,
    containerSize: IntSize,
    cropParams: CropParams,
    scaleAdjustment: Float
): TransformData {
    if (imageSize.width == 0 || containerSize.width == 0) return TransformData(1f, 0f, 0f)

    val imageRatio = imageSize.width.toFloat() / imageSize.height
    val containerRatio = containerSize.width.toFloat() / containerSize.height.toFloat()

    val fittedWidth: Float
    val fittedHeight: Float

    if (imageRatio > containerRatio) {
        fittedWidth = containerSize.width.toFloat()
        fittedHeight = fittedWidth / imageRatio
    } else {
        fittedHeight = containerSize.height.toFloat()
        fittedWidth = fittedHeight * imageRatio
    }

    val baseScale = max(containerSize.width / fittedWidth, containerSize.height / fittedHeight)
    val totalScale = baseScale * cropParams.userScale

    return TransformData(
        scale = totalScale,
        offsetX = cropParams.userOffsetX * scaleAdjustment,
        offsetY = cropParams.userOffsetY * scaleAdjustment
    )
}

// 1. 在 EditorState 中增加 sessionId
data class EditorState(
    val sessionId: Long = 0L, // 新增：用于强制刷新 Compose UI 缓存的唯一标识
    val entryId: String? = null,
    val originalText: String = "",
    val editingText: String = "",
    val isDirty: Boolean = false,
    val isNew: Boolean = true
)