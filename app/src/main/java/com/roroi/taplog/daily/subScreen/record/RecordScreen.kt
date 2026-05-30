package com.roroi.taplog.daily.subScreen.record

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.daily.DailyDynamicBackground
import com.roroi.taplog.daily.GlassmorphismBackground
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.daily.viewmodel.RecordDayData
import com.roroi.taplog.daily.viewmodel.RecordEvent
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// 根据事件名称生成固定的优美色彩
fun getEventColor(name: String): Color {
    if (name == "🛏️") return Color(0xFF5C6BC0)
    if (name == "💻") return Color(0xFFEF5350)
    if (name == "🎮") return Color(0xFF66BB6A)
    val hash = name.hashCode()
    val hue = abs(hash % 360).toFloat()
    return Color.hsv(hue, 0.65f, 0.9f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecordScreen(viewModel: DailyViewModel, recordId: String, onBack: () -> Unit) {
    val theme = viewModel.getThemeBySpace()
    
    val initialEntry = viewModel.getEntryFromId(recordId) ?: return
    var currentEntry by remember { mutableStateOf(initialEntry) }
    var currentSpace by remember { mutableStateOf(viewModel.getSpaceFromId(viewModel.selectedDSpaceId)) }

    var data by remember {
        mutableStateOf(
            try {
                Json.decodeFromString<RecordDayData>(currentEntry.content)
            } catch (_: Exception) {
                RecordDayData()
            }
        )
    }

    val saveData = { newData: RecordDayData ->
        data = newData
        val updatedEntry = currentEntry.copy(content = Json.encodeToString(newData))
        viewModel.updateEntry(updatedEntry)
        currentEntry = updatedEntry
    }

    var showStopDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var editEventData by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要彻底删除这一整天的所有记录吗？此操作无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry(recordId)
                    onBack()
                }) { Text("永久删除", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("停止记录", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要停止这一天的记录吗？停止后无法再添加新事件。") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    val stoppedEvents = data.events.map { if (it.endTime == null) it.copy(endTime = System.currentTimeMillis()) else it }
                    saveData(data.copy(events = stoppedEvents, isStopped = true))
                    onBack()
                }) { Text("确定", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    // 【修复核心1】：添加事件时，不仅存入 Space，也存入当天数据
    if (showAddEventDialog) {
        var iconInput by remember { mutableStateOf("") }
        var descInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("添加自定义事件", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = iconInput, onValueChange = { if (it.length <= 4 && !it.contains("|")) iconInput = it }, label = { Text("图标 (单字/Emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descInput, onValueChange = { if (!it.contains("|")) descInput = it }, label = { Text("说明文字 (可选)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val trimmedIcon = iconInput.trim()
                    val trimmedDesc = descInput.trim()
                    if (trimmedIcon.isNotBlank()) {
                        val combinedString = if (trimmedDesc.isNotBlank()) "$trimmedIcon|$trimmedDesc" else trimmedIcon
                        
                        // 1. 如果在某个空间里，存进空间全局配置
                        currentSpace?.let { spaceVal ->
                            val filteredList = spaceVal.customRecordEvents.filterNot { it == trimmedIcon || it.startsWith("$trimmedIcon|") }
                            val updatedSpace = spaceVal.copy(customRecordEvents = filteredList + combinedString)
                            viewModel.changeSpaceP(updatedSpace)
                            currentSpace = updatedSpace
                        }
                        
                        // 2. 无论在不在空间里，都存进当天的 RecordDayData 里（完美解决主空间为null的问题）
                        val filteredDataEvents = data.customEvents.filterNot { it == trimmedIcon || it.startsWith("$trimmedIcon|") }
                        saveData(data.copy(customEvents = filteredDataEvents + combinedString))
                    }
                    showAddEventDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddEventDialog = false }) { Text("取消") } }
        )
    }

    // 【修复核心2】：编辑事件时，同步更新当天数据并持久化
    if (editEventData != null) {
        val (oldIcon, oldLabel) = editEventData!!
        var iconInput by remember { mutableStateOf(oldIcon) }
        var descInput by remember { mutableStateOf(oldLabel) }

        AlertDialog(
            onDismissRequest = { editEventData = null },
            title = { Text("编辑事件", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = iconInput, onValueChange = { if (it.length <= 4 && !it.contains("|")) iconInput = it }, label = { Text("图标 (单字/Emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descInput, onValueChange = { if (!it.contains("|")) descInput = it }, label = { Text("说明文字 (可选)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val trimmedIcon = iconInput.trim()
                    val trimmedDesc = descInput.trim()
                    if (trimmedIcon.isNotBlank()) {
                        val combinedString = if (trimmedDesc.isNotBlank()) "$trimmedIcon|$trimmedDesc" else trimmedIcon

                        currentSpace?.let { spaceVal ->
                            val filteredList = spaceVal.customRecordEvents.filterNot { it == oldIcon || it.startsWith("$oldIcon|") }
                            val updatedSpace = spaceVal.copy(customRecordEvents = filteredList + combinedString)
                            viewModel.changeSpaceP(updatedSpace)
                            currentSpace = updatedSpace 
                        }
                        
                        // 将修改后的配置存进当天记录里
                        val filteredDataEvents = data.customEvents.filterNot { it == oldIcon || it.startsWith("$oldIcon|") }
                        val newCustomEvents = filteredDataEvents + combinedString

                        val updatedEvents = if (oldIcon != trimmedIcon) {
                            data.events.map { if (it.iconOrText == oldIcon) it.copy(iconOrText = trimmedIcon) else it }
                        } else {
                            data.events
                        }

                        // 一次性保存到数据库
                        saveData(data.copy(customEvents = newCustomEvents, events = updatedEvents))
                    }
                    editEventData = null
                }) { Text("保存修改") }
            },
            dismissButton = { TextButton(onClick = { editEventData = null }) { Text("取消") } }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daily Timeline", color = theme.primaryColor, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.onSurfaceColor) } },
                actions = {
                    Box {
                        IconButton({ showMoreMenu = !showMoreMenu }) { Icon(Icons.Default.Menu, "Menu") }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }, containerColor = theme.backgroundColor) {
                            DropdownMenuItem(text = { Text("Stop", color = theme.primaryColor) }, leadingIcon = { Icon(Icons.Default.Save, "Stop", tint = theme.primaryColor) }, onClick = { showStopDialog = true })
                            DropdownMenuItem(text = { Text("Delete", color = theme.primaryColor) }, leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = theme.primaryColor) }, onClick = { showDeleteDialog = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.backgroundColor.copy(0.7f))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DailyDynamicBackground(theme)
            GlassmorphismBackground(modifier = Modifier.fillMaxSize(), alpha = 0.5f)

            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {

                Surface(color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.padding(vertical = 16.dp)) {
                        TimelineCanvas(data, currentEntry.timestamp, theme.primaryColor)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!data.isStopped) {
                    Surface(color = Color.White.copy(0.75f), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(vertical = 32.dp)) {
                            Text("Activities (Long press to edit)", fontWeight = FontWeight.Bold, color = theme.onSurfaceColor, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(12.dp))

                            val defaultEvents = listOf("🛏️" to "Rest", "💻" to "Work", "🎮" to "Play")
                            val historyIcons = data.events.map { it.iconOrText }.filter { it.isNotBlank() }
                            
                            val spaceIcons = currentSpace?.customRecordEvents ?: emptyList()
                            // 【修复核心3】：提取出今天保存在数据库里的专属自定义按钮
                            val dayIcons = data.customEvents

                            // 将空间按钮和当天按钮合并解析
                            val customEventsMap = (spaceIcons + dayIcons).associate { rawString ->
                                val parts = rawString.split("|", limit = 2)
                                parts[0] to parts.getOrElse(1) { parts[0] }
                            }

                            val combinedMap = mutableMapOf<String, String>()
                            defaultEvents.forEach { combinedMap[it.first] = it.second }
                            customEventsMap.forEach { combinedMap[it.key] = it.value } 
                            historyIcons.forEach { if (!combinedMap.containsKey(it)) combinedMap[it] = it }

                            val allEvents = combinedMap.toList()

                            LazyColumn {
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        maxItemsInEachRow = 5
                                    ) {
                                        allEvents.forEach { (icon, label) ->
                                            val isOngoing = data.events.any { it.iconOrText == icon && it.endTime == null }
                                            val eventColor = remember(icon) { getEventColor(icon) }

                                            EventButton(
                                                icon = icon,
                                                label = label,
                                                activeColor = eventColor,
                                                isOngoing = isOngoing,
                                                onClick = {
                                                    val ongoingEvent = data.events.find { it.iconOrText == icon && it.endTime == null }
                                                    if (ongoingEvent != null) {
                                                        val updated = data.events.map {
                                                            if (it.id == ongoingEvent.id) it.copy(endTime = System.currentTimeMillis()) else it
                                                        }
                                                        saveData(data.copy(events = updated))
                                                    } else {
                                                        saveData(data.copy(events = data.events + RecordEvent(startTime = System.currentTimeMillis(), iconOrText = icon)))
                                                    }
                                                },
                                                onLongClick = {
                                                    editEventData = icon to label
                                                }
                                            )
                                        }

                                        EventButton("➕", "Add", Color.Gray, false, onClick = { showAddEventDialog = true }, onLongClick = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventButton(
    icon: String,
    label: String,
    activeColor: Color,
    isOngoing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit // 新增长按事件
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            // [新增 4] 替换为 combinedClickable 支持长按
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
            .widthIn(min = 48.dp, max = 64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (isOngoing) activeColor.copy(alpha = 0.5f) else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = activeColor.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// 帮助存储点击区域的类
data class EventHitBox(val eventId: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

@Composable
fun TimelineCanvas(data: RecordDayData, entryTimestamp: Long, primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (data.events.any { event -> event.endTime == null && !data.isStopped }) 0.35f else 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // [新增 5] 浮窗选中事件的ID及点击区域收集器
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    val hitBoxes = remember { mutableListOf<EventHitBox>() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // 绑定点击事件，通过坐标找到点中的图标
            .pointerInput(data.events) {
                detectTapGestures { offset ->
                    val hit = hitBoxes.find { offset.x in it.left..it.right && offset.y in it.top..it.bottom }
                    selectedEventId = hit?.eventId // 如果没点中任何东西，则设为null隐藏浮窗
                }
            }
    ) {
        // 每次重新画的时候清空之前的碰撞体积
        hitBoxes.clear()

        val canvasW = size.width
        val canvasH = size.height
        val lineSpacing = canvasH / 5f
        val currentMillis = System.currentTimeMillis()

        val dayStart = Calendar.getInstance().apply {
            timeInMillis = entryTimestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startX = 76.dp.toPx()
        val endX = canvasW - 40.dp.toPx()
        val trackWidth = endX - startX
        val trackHeight = 28.dp.toPx()
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        val distinctEvents = data.events.map { it.iconOrText }.distinct()

        for (i in 0..3) {
            val yCenter = lineSpacing * (i + 1)
            val yTop = yCenter - trackHeight / 2f
            drawContext.canvas.nativeCanvas.drawText(
                String.format(Locale.ROOT, "%02d:00", i * 6),
                12.dp.toPx(), yCenter + 5.dp.toPx(),
                android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12.sp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
            )
            drawRoundRect(color = primaryColor.copy(alpha = 0.05f), topLeft = Offset(startX, yTop), size = androidx.compose.ui.geometry.Size(trackWidth, trackHeight), cornerRadius = cornerRadius)
        }

        fun getProgress(millis: Long): Float = ((millis - dayStart).coerceIn(0L, 86400000L)).toFloat() / 86400000f
        fun getSection(progress: Float): Int = (progress * 4).toInt().coerceIn(0, 3)
        fun getX(progress: Float): Float = startX + trackWidth * ((progress * 4) - getSection(progress))
        fun getYTop(section: Int): Float = lineSpacing * (section + 1) - trackHeight / 2f

        fun drawActiveRect(startMillis: Long, endMillis: Long, eventColor: Color, isOngoing: Boolean) {
            val startP = getProgress(startMillis)
            val endP = getProgress(endMillis)
            if (startP >= endP && !isOngoing) return
            val s = getSection(startP)
            val e = getSection(endP)

            fun drawSegment(section: Int, x1: Float, x2: Float) {
                val actualX2 = maxOf(x2, x1 + 2.dp.toPx())
                val top = getYTop(section)
                if (isOngoing) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(
                            left = x1, top = top, right = actualX2, bottom = top + trackHeight,
                            topLeftCornerRadius = cornerRadius, bottomLeftCornerRadius = cornerRadius,
                            topRightCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
                        ))
                    }
                    drawPath(path = path, color = eventColor.copy(alpha = 0.85f))
                } else {
                    drawRoundRect(color = eventColor.copy(alpha = 0.85f), topLeft = Offset(x1, top), size = androidx.compose.ui.geometry.Size(actualX2 - x1, trackHeight), cornerRadius = cornerRadius)
                }
            }

            if (s == e) { drawSegment(s, getX(startP), getX(endP)) }
            else { drawSegment(s, getX(startP), endX); for (i in (s + 1) until e) drawSegment(i, startX, endX); drawSegment(e, startX, getX(endP)) }

            val currentX = getX(endP)
            val currentYTop = getYTop(e)
            val actualCurrentX = if (getX(startP) <= getX(endP)) currentX else currentX + 2.dp.toPx()

            if (!data.isStopped && isOngoing) {
                drawRoundRect(color = primaryColor.copy(alpha = pulseAlpha), topLeft = Offset(actualCurrentX - 1.5.dp.toPx(), currentYTop - 4.dp.toPx()), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), trackHeight + 8.dp.toPx()), cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()))
            }
        }

        // 准备文字画笔
        val textPaint = android.graphics.Paint().apply {
            textSize = 18.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (event in data.events) {
            val eventIndex = distinctEvents.indexOf(event.iconOrText)
            val isOngoing = event.endTime == null && !data.isStopped
            val calculateEnd = event.endTime ?: currentMillis
            val eventColor = getEventColor(event.iconOrText)

            drawActiveRect(event.startTime, calculateEnd, eventColor, isOngoing)

            val isUp = eventIndex % 2 == 0
            val iconYOffset = if (isUp) -20.dp.toPx() else (trackHeight + 20.dp.toPx())
            val startP = getProgress(event.startTime)
            val startSection = getSection(startP)

            // [修改重点 6] 计算文字大小，并向右偏移宽度的一半
            val textWidth = textPaint.measureText(event.iconOrText)
            val textX = getX(startP) + textWidth / 2f
            val textY = getYTop(startSection) + iconYOffset

            drawContext.canvas.nativeCanvas.drawText(
                event.iconOrText,
                textX,
                textY,
                textPaint
            )

            // 收集该图标的碰撞检测框 (向四周延伸一点像素方便点击)
            hitBoxes.add(
                EventHitBox(
                    eventId = event.id,
                    left = textX - textWidth / 2 - 20f,
                    top = textY - textPaint.textSize - 20f, // 文字的Y是底座，所以要往上找
                    right = textX + textWidth / 2 + 20f,
                    bottom = textY + 20f
                )
            )
        }

        // [新增 7] 绘制点击出来的浮窗提示
        if (selectedEventId != null) {
            val selectedEvent = data.events.find { it.id == selectedEventId }
            if (selectedEvent != null) {
                val eventIndex = distinctEvents.indexOf(selectedEvent.iconOrText)
                val isUp = eventIndex % 2 == 0
                val startP = getProgress(selectedEvent.startTime)
                val startSection = getSection(startP)

                val textWidth = textPaint.measureText(selectedEvent.iconOrText)
                val textX = getX(startP) + textWidth / 2f

                // 根据图标方向反转浮窗方向（图标在上方，浮窗就在轨道下方）
                val tooltipYCenter = if (isUp) {
                    getYTop(startSection) + trackHeight + 24.dp.toPx()
                } else {
                    getYTop(startSection) - 24.dp.toPx()
                }

                // 格式化时间字符串
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTimeStr = sdf.format(Date(selectedEvent.startTime))
                val endTimeStr = if (selectedEvent.endTime != null) sdf.format(Date(selectedEvent.endTime)) else "Now"
                val timeString = "$startTimeStr - $endTimeStr"

                val tooltipPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val timeWidth = tooltipPaint.measureText(timeString)

                // 绘制浮窗黑色半透明背景
                val bgWidth = timeWidth + 24.dp.toPx()
                val bgHeight = 24.dp.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    topLeft = Offset(textX - bgWidth / 2f, tooltipYCenter - bgHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(bgWidth, bgHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )

                // 绘制时间文字
                drawContext.canvas.nativeCanvas.drawText(
                    timeString,
                    textX,
                    tooltipYCenter + 4.dp.toPx(), // 文字居中微调
                    tooltipPaint
                )
            }
        }
    }
}
