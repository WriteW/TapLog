package com.roroi.taplog.daily.subScreen.record

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.daily.DailyDynamicBackground
import com.roroi.taplog.daily.GlassmorphismBackground
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.daily.viewmodel.RecordDayData
import com.roroi.taplog.daily.viewmodel.RecordEvent
import kotlinx.serialization.json.Json
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(viewModel: DailyViewModel, recordId: String, onBack: () -> Unit) {
    val theme = viewModel.getThemeBySpace()
    val entry = viewModel.getEntryFromId(recordId) ?: return
    val space = viewModel.getSpaceFromId(viewModel.selectedDSpaceId)
    
    var data by remember { 
        mutableStateOf(try { Json.decodeFromString<RecordDayData>(entry.content) } catch (e: Exception) { RecordDayData() }) 
    } 

    val saveData = { newData: RecordDayData ->
        data = newData
        viewModel.updateEntry(entry.copy(content = Json.encodeToString(newData)))
    }

    var showStopDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("停止记录", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要停止这一天的记录吗？停止后无法再添加新事件。") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    // 强制停止所有尚未结束的事件
                    val stoppedEvents = data.events.map { if (it.endTime == null) it.copy(endTime = System.currentTimeMillis()) else it }
                    saveData(data.copy(events = stoppedEvents, isStopped = true))
                    onBack()
                }) { Text("确定", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    if (showAddEventDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("添加自定义事件", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input, onValueChange = { if (it.length <= 4) input = it }, 
                    label = { Text("输入单字/Emoji") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmedInput = input.trim()
                    if (trimmedInput.isNotBlank()) {
                        // 1. 如果在空间内，永久保存到该 Space 的 LazyRow 快捷选项中
                        if (space != null && !space.customRecordEvents.contains(trimmedInput)) {
                            val newSpace = space.copy(customRecordEvents = space.customRecordEvents + trimmedInput)
                            viewModel.changeSpaceP(newSpace)
                        }
                        
                        // 2. 修复核心问题：立即触发并在时间轴上开始记录这个新添加的事件！
                        saveData(data.copy(
                            events = data.events + RecordEvent(
                                startTime = System.currentTimeMillis(), 
                                iconOrText = trimmedInput
                            )
                        ))
                    }
                    showAddEventDialog = false
                }) { Text("保存并开始") }
            },
            dismissButton = { TextButton(onClick = { showAddEventDialog = false }) { Text("取消") } }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daily Timeline", color = theme.primaryColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.onSurfaceColor) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = theme.backgroundColor.copy(0.7f))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DailyDynamicBackground(theme)
            GlassmorphismBackground(modifier = Modifier.fillMaxSize(), alpha = 0.5f)

            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                
                // 核心时间轴区域 (Canvas绘制宽线轨)
                Surface(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(vertical = 16.dp)) {
                        TimelineCanvas(data, entry.timestamp, theme.primaryColor, theme.backgroundColor)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 底部操作面板 (LazyRow)
                if (!data.isStopped) {
                    Surface(color = Color.White.copy(0.75f), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(vertical = 16.dp)) {
                            Text("Activities", fontWeight = FontWeight.Bold, color = theme.onSurfaceColor, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val defaultEvents = listOf("🛏️" to "Rest", "💻" to "Work", "🎮" to "Play")
                            val customEvents = space?.customRecordEvents?.map { it to it } ?: emptyList()
                            val allEvents = defaultEvents + customEvents

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(allEvents) { (icon, label) ->
                                    val isOngoing = data.events.any { it.iconOrText == icon && it.endTime == null }
                                    EventButton(
                                        icon = icon, label = label,
                                        activeColor = if (isOngoing) Color(0xFFFF5252) else theme.primaryColor, // 正在运行显示红色的警告底色
                                        isOngoing = isOngoing,
                                        onClick = {
                                            val ongoingEvent = data.events.find { it.iconOrText == icon && it.endTime == null }
                                            if (ongoingEvent != null) {
                                                // 点击已激活事件：停止它
                                                val updated = data.events.map { if (it.id == ongoingEvent.id) it.copy(endTime = System.currentTimeMillis()) else it }
                                                saveData(data.copy(events = updated))
                                            } else {
                                                // 点击未激活事件：开始它
                                                saveData(data.copy(events = data.events + RecordEvent(startTime = System.currentTimeMillis(), iconOrText = icon)))
                                            }
                                        }
                                    )
                                }
                                item {
                                    EventButton("➕", "Add", Color.Gray, false) { showAddEventDialog = true }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.85f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Stop Record Today", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EventButton(icon: String, label: String, activeColor: Color, isOngoing: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(if (isOngoing) activeColor.copy(alpha = 0.2f) else Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = activeColor.copy(alpha = 0.9f))
    }
}

@Composable
fun TimelineCanvas(data: RecordDayData, entryTimestamp: Long, activeColor: Color, bgColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height
        val lineSpacing = canvasH / 5f
        val currentMillis = System.currentTimeMillis()

        val dayStart = Calendar.getInstance().apply { 
            timeInMillis = entryTimestamp; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startX = 60.dp.toPx()
        val endX = canvasW - 40.dp.toPx()
        // 获取所有唯一类型的Event，便于计算在宽带内堆叠时的Y轴偏移以避免完全重合
        val distinctEvents = data.events.map { it.iconOrText }.distinct()

        // 绘制宽轨道背景线
        for (i in 0..3) {
            val y = lineSpacing * (i + 1)
            drawContext.canvas.nativeCanvas.drawText(
                "${String.format("%02d", i * 6)}:00",
                16.dp.toPx(), y + 4.dp.toPx(),
                android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12.sp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
            )
            drawLine(
                color = bgColor.copy(alpha = 0.4f + (i * 0.1f)),
                start = Offset(startX, y), end = Offset(endX, y),
                strokeWidth = 28.dp.toPx(), // 加宽的背景跑道，容纳多条并行线
                cap = StrokeCap.Round
            )
        }

        fun getProgress(millis: Long): Float = ((millis - dayStart).coerceIn(0L, 86400000L)).toFloat() / 86400000f
        fun getSection(progress: Float): Int = (progress * 4).toInt().coerceIn(0, 3)
        fun getX(progress: Float): Float = startX + (endX - startX) * ((progress * 4) - getSection(progress))
        fun getY(section: Int): Float = lineSpacing * (section + 1)

        fun drawActiveBlock(startMillis: Long, endMillis: Long, yOffset: Float, isOngoing: Boolean) {
            val startP = getProgress(startMillis)
            val endP = getProgress(endMillis)
            if (startP >= endP && !isOngoing) return
            
            val s = getSection(startP)
            val e = getSection(endP)
            val strokeW = 6.dp.toPx() // 细一点的线条放入宽跑道内
            
            if (s == e) {
                drawLine(activeColor, Offset(getX(startP), getY(s) + yOffset), Offset(getX(endP), getY(e) + yOffset), strokeWidth = strokeW, cap = StrokeCap.Round)
            } else {
                drawLine(activeColor, Offset(getX(startP), getY(s) + yOffset), Offset(endX, getY(s) + yOffset), strokeWidth = strokeW, cap = StrokeCap.Round)
                for (i in (s + 1) until e) drawLine(activeColor, Offset(startX, getY(i) + yOffset), Offset(endX, getY(i) + yOffset), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(activeColor, Offset(startX, getY(e) + yOffset), Offset(getX(endP), getY(e) + yOffset), strokeWidth = strokeW, cap = StrokeCap.Round)
            }
            
            // 绘制起点白心点
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(getX(startP), getY(s) + yOffset))
            drawCircle(activeColor, radius = 3.dp.toPx(), center = Offset(getX(startP), getY(s) + yOffset))
            
            // 绘制终点 (或者是脉冲动画)
            val endPoint = Offset(getX(endP), getY(e) + yOffset)
            if (!isOngoing) {
                drawCircle(Color.White, radius = 5.dp.toPx(), center = endPoint)
                drawCircle(activeColor, radius = 3.dp.toPx(), center = endPoint)
            } else {
                drawCircle(Color.White.copy(alpha = pulseAlpha), radius = 8.dp.toPx(), center = endPoint)
                drawCircle(Color.Red, radius = 4.dp.toPx(), center = endPoint)
            }
        }

        for (event in data.events) {
            // 根据事件类型的索引，计算错开的 Y 轴距离（-8dp, 0dp, 8dp循环），实现平行轨道效果
            val eventIndex = distinctEvents.indexOf(event.iconOrText)
            val yOffset = ((eventIndex % 3) - 1) * 8.dp.toPx()
            
            val isOngoing = event.endTime == null && !data.isStopped
            val calculateEnd = event.endTime ?: currentMillis

            drawActiveBlock(event.startTime, calculateEnd, yOffset, isOngoing)
            
            // 起点文字 Emoji 漂浮在轨道上方
            drawContext.canvas.nativeCanvas.drawText(
                event.iconOrText, getX(getProgress(event.startTime)), getY(getSection(getProgress(event.startTime))) + yOffset - 8.dp.toPx(),
                android.graphics.Paint().apply { textSize = 16.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}
