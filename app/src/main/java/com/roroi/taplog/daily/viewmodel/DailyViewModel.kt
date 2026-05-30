package com.roroi.taplog.daily.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roroi.taplog.daily.DailyTimeTheme
import com.roroi.taplog.daily.generateColorPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import kotlin.math.max
import kotlin.math.sin

class DailyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DailyRepository(application)
    private val _groupedEntries = MutableStateFlow<List<TimelineGroup>>(emptyList())
    val groupedEntries = _groupedEntries.asStateFlow()
    private val _editorState = MutableStateFlow(EditorState())
    private var currentUnlockedPassword: String? = null
    val editorState = _editorState.asStateFlow()

    fun startEditing(entryId: String?) {
        val entry = getEntryFromId(entryId)

        _editorState.value = if (entry != null) {
            EditorState(
                sessionId = System.currentTimeMillis(), // 新增：每次进入分配新 ID
                entryId = entry.id,
                originalTitle = entry.title ?: "", // 【新增】
                editingTitle = entry.title ?: "",  // 【新增】
                originalText = entry.content,
                editingText = entry.content,
                isDirty = false,
                isNew = false,
                timestamp = entry.timestamp
            )
        } else {
            EditorState(
                sessionId = System.currentTimeMillis(), // 新增：每次新建分配新 ID
                timestamp = System.currentTimeMillis() // 【新增】
            )
        }
    }
    // 【新增】处理标题变化
    fun onEditorTitleChange(title: String) {
        _editorState.update {
            it.copy(
                editingTitle = title,
                isDirty = title != it.originalTitle || it.editingText != it.originalText
            )
        }
    }
    fun onEditorTextChange(text: String) {
        _editorState.update {
            it.copy(
                editingText = text,
                isDirty = text != it.originalText || it.editingTitle != it.originalTitle
            )
        }
    }
    fun saveEditor(onDone: () -> Unit) {
        val state = _editorState.value

        if (state.editingText.isBlank() && state.editingTitle.isBlank()) return

        viewModelScope.launch {
            if (state.isNew) {
                addTextEntry(state.editingText, state.editingTitle)
            } else {
                val entry = getEntryFromId(state.entryId) ?: return@launch
                updateEntry(entry.copy(
                    content = state.editingText,
                    title = state.editingTitle.ifBlank { null } // 为空自动置为 null
                ))
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
    var bindingTargetId: String? by mutableStateOf(null)
        private set
    fun startBindingMode(targetId: String) {
        bindingTargetId = targetId
        batchEntries.clear()
        startBatchSelecting()
    }
    fun executeBinding() {
        val targetId = bindingTargetId ?: return
        if (batchEntries.isEmpty()) {
            stopBatchSelecting()
            return
        }

        viewModelScope.launch {
            showLoadingDialog = true
            val targetEntry = getEntryFromId(targetId)
            if (targetEntry != null) {
                // 1. 如果目标项还没有分组 ID，生成一个新的
                val groupId = targetEntry.manualGroupId ?: java.util.UUID.randomUUID().toString()
                val entriesToUpdate = mutableListOf<DailyEntry>()

                // 2. 将目标项原本组内的兄弟们打上一样的钢印
                val targetGroup = _groupedEntries.value.find { it.items.any { item -> item.id == targetId } }
                targetGroup?.items?.forEach { item ->
                    if (item.manualGroupId != groupId) {
                        entriesToUpdate.add(item.copy(manualGroupId = groupId))
                    }
                }

                // 3. 将用户勾选的所有 Entry 打上目标组的钢印
                batchEntries.forEach { sourceId ->
                    val sourceEntry = getEntryFromId(sourceId)
                    if (sourceEntry != null && sourceEntry.manualGroupId != groupId) {
                        entriesToUpdate.add(sourceEntry.copy(manualGroupId = groupId))
                    }
                }

                // 4. 批量保存并刷新UI
                entriesToUpdate.forEach { entry ->
                    repository.saveEntry(entry, selectedDSpaceId)
                }
                loadData()
            }

            // 5. 结束状态
            stopBatchSelecting()
            showLoadingDialog = false
        }
    }

    // 存储空间相关状态
    var spaces by mutableStateOf<List<DSpace>>(emptyList())
        private set
    var selectedDSpaceId: String? by mutableStateOf(null)
        private set
    var spaceDestination: String? by mutableStateOf(null)
        private set

    var showLoadingDialog: Boolean by mutableStateOf(false)

    fun getThemeBySpace(): DailyTimeTheme {
        val space = spaces.find { it.id == selectedDSpaceId }

        if (space != null) {
            val isDark = space.isDark

            return DailyTimeTheme(
                backgroundColor = Color(space.colorBgArgb),
                primaryColor = Color(space.colorBallArgb),
                isDark = isDark,            // 明确指定深色模式标志
                ballColors = generateColorPalette(Color(space.colorBallArgb))
            )
        }

        // 如果没找到空间，返回当前时间段的默认主题
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

    fun moveEntry(originalSpaceId: String?, targetSpaceId: String?, targetPassword: String? = null) {
        if (selectedEntryId.isNullOrBlank() && batchEntries.isEmpty()) return

        viewModelScope.launch {
            try {
                showLoadingDialog = true
                if (batchEntries.isEmpty() && selectedEntryId != null) batchEntries.add(selectedEntryId.toString())

                batchEntries.forEach { entryId ->
                    val entry = getEntryFromId(entryId)
                    entry?.let {
                        when (entry.type) {
                            // 文本和全天记录只有数据本身
                            EntryType.TEXT, EntryType.RECORD -> {
                                repository.moveTextEntry(originalSpaceId, targetSpaceId, entry)
                            }
                            // 图片和音频附带实体媒体文件
                            EntryType.IMAGE, EntryType.AUDIO -> {
                                repository.moveImageEntry(originalSpaceId, targetSpaceId, entry)
                            }
                        }
                    }
                }

                // [核心修复] 如果目标空间是加密的，移动完文件后必须立即加密刚刚放入的文件！
                val targetSpace = getSpaceFromId(targetSpaceId)
                if (targetSpace != null && targetSpace.isEncrypted && targetPassword != null) {
                    com.roroi.taplog.daily.viewmodel.encryption.lockAndExit(
                        getApplication(),
                        targetPassword,
                        targetSpace.id,
                        targetSpace.encryptImages,
                        targetSpace.encryptAudio
                    )
                }

                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                showLoadingDialog = false
                stopBatchSelecting()
                unFocusEntry()
            }
        }
    }

    // [新增] 可靠的 Record 跳转函数
    fun navigateToRecord(id: String) {
        viewModelScope.launch {
            navigationEvent.emit(Pair("portal", "record?id=$id"))
        }
    }
    // 修改更改密码逻辑
    fun changeSpacePassword(newPass: String) {
        val space = getSpaceFromId(selectedDSpaceId) ?: return

        if (newPass.isBlank()) {
            // 取消密码
            currentUnlockedPassword = null
            repository.saveSpace(space.copy(isEncrypted = false, password = ""))
            // 删除现存的加密文件(如果有的话)
            val encryptedFile = File(getApplication<Application>().getExternalFilesDir(null), "daily/${space.id}.${com.roroi.taplog.daily.viewmodel.encryption.CryptoManager.SUFFIX}")
            if (encryptedFile.exists()) encryptedFile.delete()
            toastOut("密码已移除")
        } else {
            // 修改或新增密码
            currentUnlockedPassword = newPass
            repository.saveSpace(space.copy(isEncrypted = true, password = ""))
            toastOut("密码设置成功，将在退出时生效")
        }
        loadSpaces()
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
    fun verifyAndEnterSpace(password: String) {
        val spaceId = spaceDestination ?: return
        val space = spaces.find { it.id == spaceId } ?: return

        viewModelScope.launch {
            showLoadingDialog = true
            if (space.isEncrypted) {
                // 调用解密逻辑
                val success = com.roroi.taplog.daily.viewmodel.encryption.unlockAndEnter(
                    getApplication(), password, space.id
                )
                if (success) {
                    currentUnlockedPassword = password // 验证成功，保存在内存中供退出时加密使用
                    selectedDSpaceId = spaceId
                    loadData()
                    showPasswordCheck = false
                } else {
                    toastOut("密码错误或文件损坏❌")
                }
            } else {
                selectedDSpaceId = spaceId
                loadData()
                showPasswordCheck = false
            }
            showLoadingDialog = false
        }

        viewingCapsuleId = null
    }

    fun changeSpace() {
        val spaceId = spaceDestination ?: return
        val spaceToChange = spaces.find { it.id == spaceId } ?: return

        // 如果空间没有加密，直接进；如果加密了，触发输入密码对话框
        if (spaceToChange.isEncrypted) {
            showPasswordCheck = true
        } else {
            selectedDSpaceId = spaceId
            viewModelScope.launch {
                showLoadingDialog = true
                loadData()
                delay(500)
                showLoadingDialog = false
            }
        }
    }
    // 修改退回主空间的逻辑
    fun exitToMainSpace() {
        viewingCapsuleId = null
        if (selectedDSpaceId != null) {
            viewModelScope.launch {
                showLoadingDialog = true
                lockCurrentSpaceIfNeeded() // 退出前加密擦除明文
                selectedDSpaceId = null
                loadData()
                showLoadingDialog = false
            }
        }
    }

    // 页面切换发射
    val navigationEvent = MutableSharedFlow<Pair<String, String>>()
    // [修改11] 打开一天的 Record 页面
    fun openRecordDay() {
        viewModelScope.launch {
            // 找到今天是否已经有未 stopped 的 RECORD entry
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }.timeInMillis

            var recordEntry = _groupedEntries.value.flatMap { it.items }.find {
                it.type == EntryType.RECORD && it.timestamp >= todayStart &&
                        !Json.decodeFromString<RecordDayData>(it.content).isStopped
            }

            if (recordEntry == null) {
                // 创建一个新的
                val newEntry = DailyEntry(
                    timestamp = System.currentTimeMillis(),
                    type = EntryType.RECORD,
                    content = Json.encodeToString(RecordDayData())
                )
                repository.saveEntry(newEntry, selectedDSpaceId)
                recordEntry = newEntry
            }
            loadData() // 刷新底层
            navigationEvent.emit(Pair("portal", "record?id=${recordEntry.id}"))
        }
    }

    // [修改1] 新增音频
    fun addAudioEntry(uri: Uri, name: String) {
        viewModelScope.launch {
            val path = repository.saveImage(uri, selectedDSpaceId) // 复用 saveImage 保存文件
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.AUDIO,
                title = name,
                content = path
            )
            repository.saveEntry(entry, selectedDSpaceId)
            loadData()
        }
    }

    // [修改6] 加密出口注入图片加密策略
    suspend fun lockCurrentSpaceIfNeeded() {
        val currentSpaceId = selectedDSpaceId ?: return
        val space = getSpaceFromId(currentSpaceId)

        if (space?.isEncrypted == true && currentUnlockedPassword != null) {
            com.roroi.taplog.daily.viewmodel.encryption.lockAndExit(
                getApplication(),
                currentUnlockedPassword!!,
                currentSpaceId,
                space.encryptImages,
                space.encryptAudio // [新增：传入音频加密策略]
            )
            // 上锁完成后，擦除内存中的密码
            currentUnlockedPassword = null
        }
    }
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

    // 修改后：
    fun navigateToEditor(id: String?) {
        startEditing(id) // <--- 新增：在导航前初始化状态，这样就不会受 Activity 重建影响
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

    fun addTextEntry(content: String, title: String = "") {
        viewModelScope.launch {
            val entry = DailyEntry(
                timestamp = System.currentTimeMillis(),
                type = EntryType.TEXT,
                content = content,
                title = title.ifBlank { null }
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

    // 【新增给 UI 调用的 API】：将条目从组中踢出，恢复自由身
    fun unbindEntryFromGroup(entryId: String) {
        viewModelScope.launch {
            val entry = getEntryFromId(entryId) ?: return@launch
            if (entry.manualGroupId != null) {
                repository.saveEntry(entry.copy(manualGroupId = null), selectedDSpaceId)
                loadData()
            }
        }
    }


    // ===============================================
    // 2. 【替换原有方法】：升级底层的数据分发排序逻辑
    // ===============================================
    private fun groupEntries(entries: List<DailyEntry>): List<TimelineGroup> {
        if (entries.isEmpty()) return emptyList()

        // 1. 拆分：带有 manualGroupId 的手动绑定项 vs 自由项
        val manualGroupsMap = mutableMapOf<String, MutableList<DailyEntry>>()
        val normalEntries = mutableListOf<DailyEntry>()

        entries.forEach { entry ->
            if (entry.manualGroupId != null) {
                manualGroupsMap.getOrPut(entry.manualGroupId) { mutableListOf() }.add(entry)
            } else {
                normalEntries.add(entry)
            }
        }

        val groups = mutableListOf<TimelineGroup>()

        // 2. 装载：强制合体手动绑定的组
        manualGroupsMap.values.forEach { groupList ->
            // 组内部按时间倒序排列
            groupList.sortByDescending { it.timestamp }
            // 整个组的时间轴位置，以组内最新的时间为准
            groups.add(TimelineGroup(groupList.first().timestamp, groupList))
        }

        // 3. 原有逻辑处理：散件（按照时间间距自动合并）
        normalEntries.sortByDescending { it.timestamp }
        var currentBatch = mutableListOf<DailyEntry>()

        normalEntries.forEach { entry ->
            if (currentBatch.isEmpty()) {
                currentBatch.add(entry)
            } else {
                val lastEntry = currentBatch.last()
                val timeDiff = kotlin.math.abs(lastEntry.timestamp - entry.timestamp)
                val isWithin10Min = timeDiff <= 10 * 60 * 1000

                val isCurrentBig = entry.type == EntryType.IMAGE && (entry.imageRatio > 1.5f || entry.isLarge)
                val isLastBig = lastEntry.type == EntryType.IMAGE && (lastEntry.imageRatio > 1.5f || lastEntry.isLarge)
                val isSamePinState = lastEntry.isPin == entry.isPin

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

        // 4. 将所有产生的组（手动绑定的 + 自动算出来的），最终在页面上按时间轴严密排序返回
        return groups.sortedWith(
            compareByDescending<TimelineGroup> { it.isPin() }
                .thenByDescending { it.timestamp }
        )
    }

    var radialMenuEntryId: String? by mutableStateOf(null)
        private set

    fun openRadialMenu(id: String) {
        radialMenuEntryId = id
    }

    fun closeRadialMenu() {
        radialMenuEntryId = null
    }

    // ================== 时间胶囊相关状态 ==================
    var timeCapsules by mutableStateOf<List<TimeCapsule>>(emptyList())
        private set

    // 记录准备创建的胶囊，挂起等待用户去主页选择 Entry
    var pendingCapsule: TimeCapsule? by mutableStateOf(null)
        private set

    // 在 init 或者 loadData 之后加载胶囊
    private suspend fun loadCapsules() {
        timeCapsules = repository.loadTimeCapsules(selectedDSpaceId)
    }

    // 第一步：在 AddTCScreen 填完信息后调用，跳转回主页并进入多选模式
    fun startCapsuleSelection(name: String, colorArgb: Int, openAt: Long) {
        pendingCapsule = TimeCapsule(
            name = name,
            colorArgb = colorArgb,
            createdAt = System.currentTimeMillis(),
            openAt = openAt,
            entryIds = emptyList()
        )
        batchEntries.clear()
        startBatchSelecting()
        navigatePop() // 退回主页
    }

    // 第二步：在主页选中日记后，点击确认封存
    fun confirmCapsuleCreation() {
        val capsule = pendingCapsule ?: return
        if (batchEntries.isEmpty()) {
            stopBatchSelecting()
            pendingCapsule = null
            return
        }

        viewModelScope.launch {
            showLoadingDialog = true
            // 1. 将选中的 entry 打上胶囊烙印
            val updatedEntries = batchEntries.mapNotNull { id ->
                getEntryFromId(id)?.copy(capsuleId = capsule.id)
            }
            updatedEntries.forEach { repository.saveEntry(it, selectedDSpaceId) }

            // 2. 保存新的胶囊元数据
            val finalCapsule = capsule.copy(entryIds = batchEntries.toList())
            val newCapsules = timeCapsules + finalCapsule
            repository.saveTimeCapsules(newCapsules, selectedDSpaceId)
            timeCapsules = newCapsules

            // 3. 清理状态并刷新主页
            pendingCapsule = null
            stopBatchSelecting()
            loadData()
            showLoadingDialog = false
            toastOut("时间胶囊已封存！🔒")
        }
    }

    // 路由跳转
    fun navigateToViewCapsules() {
        viewModelScope.launch { navigationEvent.emit(Pair("portal", "viewCapsules")) }
    }
    fun navigateToAddCapsule() {
        viewModelScope.launch { navigationEvent.emit(Pair("portal", "addCapsule")) }
    }

    // ============== 找到 loadData() 方法修改，隐藏被封存的日记 ==============
    suspend fun loadData() {
        loadCapsules()
        val entries = repository.getAllEntries(selectedDSpaceId)

        // [新增] 自动处理未停止的跨天 Record (延续昨日未结束的事件)
        var needsReload = false
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (entry in entries) {
            if (entry.type == EntryType.RECORD && entry.timestamp < todayStart) {
                try {
                    val data = kotlinx.serialization.json.Json.decodeFromString<RecordDayData>(entry.content)
                    if (!data.isStopped) {
                        // 1. 关闭昨天的记录(截至23:59:59)
                        val stoppedEvents = data.events.map { 
                            if (it.endTime == null) it.copy(endTime = todayStart - 1) else it 
                        }
                        repository.saveEntry(entry.copy(content = kotlinx.serialization.json.Json.encodeToString(data.copy(events = stoppedEvents, isStopped = true))), selectedDSpaceId)
                        needsReload = true
                        
                        // 2. 将未结束的事件跨越到今天创建新日记
                        val ongoingOld = data.events.filter { it.endTime == null }
                        if (ongoingOld.isNotEmpty()) {
                            val hasToday = entries.any { it.type == EntryType.RECORD && it.timestamp >= todayStart }
                            if (!hasToday) {
                                val newOngoing = ongoingOld.map { it.copy(id = java.util.UUID.randomUUID().toString(), startTime = todayStart, endTime = null) }
                                val newEntry = DailyEntry(
                                    timestamp = todayStart,
                                    type = EntryType.RECORD,
                                    content = kotlinx.serialization.json.Json.encodeToString(RecordDayData(events = newOngoing, isStopped = false))
                                )
                                repository.saveEntry(newEntry, selectedDSpaceId)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 若发生了跨天自动操作，重新读取数据流
        val updatedEntries = if (needsReload) repository.getAllEntries(selectedDSpaceId) else entries

        // 【核心修改】：通过拦截数据源实现“切换空间”的效果
        val visibleEntries = if (viewingCapsuleId != null) {
            updatedEntries.filter { it.capsuleId == viewingCapsuleId }
        } else {
            updatedEntries.filter { it.capsuleId == null }
        }

        val newEntries = visibleEntries.sortedWith(
            compareByDescending<DailyEntry> { it.isPin }
                .thenByDescending { it.timestamp }
        )
        _groupedEntries.value = groupEntries(newEntries)
    }

    // ============== 找到 stopBatchSelecting() 添加清理 ==============
    fun stopBatchSelecting() {
        isBatchManaging = false
        batchEntries.clear()
        bindingTargetId = null
        pendingCapsule = null // 【新增】
    }

    // 当前正在沉浸式查看的时间胶囊ID
    var viewingCapsuleId: String? by mutableStateOf(null)
        private set

    // 退出胶囊虚拟空间，回到普通主页
    fun exitCapsuleSpace() {
        viewingCapsuleId = null
        viewModelScope.launch {
            showLoadingDialog = true
            loadData()
            showLoadingDialog = false
        }
    }

    // 1. 标记胶囊为已读
    fun markCapsuleAsViewed(capsuleId: String) {
        viewModelScope.launch {
            val updatedCapsules = timeCapsules.map {
                if (it.id == capsuleId) it.copy(isViewed = true) else it
            }
            repository.saveTimeCapsules(updatedCapsules, selectedDSpaceId)
            timeCapsules = updatedCapsules
        }
    }

    // 2. 彻底删除胶囊及其内部的所有日记
    fun deleteCapsule(capsuleId: String) {
        viewModelScope.launch {
            val capsule = timeCapsules.find { it.id == capsuleId } ?: return@launch

            // 删除胶囊内的所有条目
            capsule.entryIds.forEach { entryId ->
                val entry = getEntryFromId(entryId)
                if (entry != null) repository.deleteEntry(entry, selectedDSpaceId)
            }

            // 删除胶囊元数据
            val updatedCapsules = timeCapsules.filterNot { it.id == capsuleId }
            repository.saveTimeCapsules(updatedCapsules, selectedDSpaceId)
            timeCapsules = updatedCapsules
            toastOut("胶囊已彻底销毁 💥")
            loadData() // 刷新底层数据
        }
    }

    // 3. 修改 openCapsuleSpace，进入时自动标记为已读并关闭侧边栏
    fun openCapsuleSpace(capsuleId: String) {
        markCapsuleAsViewed(capsuleId) // 消除红点
        viewingCapsuleId = capsuleId
        viewModelScope.launch {
            showLoadingDialog = true
            loadData()
            navigatePop()
            showLoadingDialog = false
        }
    }

    // ================== 搜索与高亮 ==================
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<DailyEntry>>(emptyList())
        private set
    var currentSearchIndex by mutableStateOf(-1)
        private set
    var highlightedEntryId by mutableStateOf<String?>(null)
        private set

    fun updateSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            searchResults = emptyList()
            currentSearchIndex = -1
        } else {
            searchResults = _groupedEntries.value.flatMap { it.items }
                .filter { it.type == EntryType.TEXT && ((it.content.contains(query, ignoreCase = true)) || (it.title?.contains(query, ignoreCase = true) == true)) }
            currentSearchIndex = if (searchResults.isNotEmpty()) 0 else -1
            if (currentSearchIndex >= 0) {
                highlightEntry(searchResults[currentSearchIndex].id)
            }
        }
    }

    fun stopSearch() {
        searchQuery = ""
    }

    fun jumpToNextSearch(listState: androidx.compose.foundation.lazy.LazyListState, scope: kotlinx.coroutines.CoroutineScope) {
        if (searchResults.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
        scrollToCurrentSearch(listState, scope)
    }

    fun jumpToPrevSearch(listState: androidx.compose.foundation.lazy.LazyListState, scope: kotlinx.coroutines.CoroutineScope) {
        if (searchResults.isEmpty()) return
        currentSearchIndex = if (currentSearchIndex - 1 < 0) searchResults.size - 1 else currentSearchIndex - 1
        scrollToCurrentSearch(listState, scope)
    }

    private fun scrollToCurrentSearch(listState: androidx.compose.foundation.lazy.LazyListState, scope: kotlinx.coroutines.CoroutineScope) {
        val targetEntry = searchResults[currentSearchIndex]
        val groupIndex = _groupedEntries.value.indexOfFirst { it.items.any { item -> item.id == targetEntry.id } }
        if (groupIndex >= 0) {
            scope.launch { listState.scrollToItem(groupIndex) }
            highlightEntry(targetEntry.id)
        }
    }

    private fun highlightEntry(id: String) {
        highlightedEntryId = id
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            if (highlightedEntryId == id) highlightedEntryId = null // 1.5秒后渐变消失
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
    val originalTitle: String = "", // 【新增】
    val editingTitle: String = "",  // 【新增】
    val originalText: String = "",
    val editingText: String = "",
    val isDirty: Boolean = false,
    val isNew: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

