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
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.abs

// 根据事件名称生成固定的优美色彩
fun getEventColor(name: String, fallback: Color): Color {
    if (name == "🛏️") return Color(0xFF5C6BC0)
    if (name == "💻") return Color(0xFFEF5350)
    if (name == "🎮") return Color(0xFF66BB6A)
    val hash = name.hashCode()
    val hue = abs(hash % 360).toFloat()
    return Color.hsv(hue, 0.65f, 0.9f)
}

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
    var showDeleteDialog by remember { mutableStateOf(false) } // [新增] 删除弹窗状态
    var localCustomEvents by remember { mutableStateOf(listOf<String>()) } // [新增] 临时保存列表，实现立即展示

    // [新增] 删除整条记录弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要彻底删除这一整天的所有记录吗？此操作无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry(recordId) // 删除数据
                    onBack() // 退出页面
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
                        if (space != null && !space.customRecordEvents.contains(trimmedInput)) {
                            val newSpace = space.copy(customRecordEvents = space.customRecordEvents + trimmedInput)
                            viewModel.changeSpaceP(newSpace)
                        }
                        // [修改] 仅添加到UI缓存展示，不再调用 saveData 自动开始
                        if (!localCustomEvents.contains(trimmedInput)) {
                            localCustomEvents = localCustomEvents + trimmedInput
                        }
                    }
                    showAddEventDialog = false
                }) { Text("保存") }
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
                            val historyIcons = data.events.map { it.iconOrText }.filter { it.isNotBlank() }
                            val spaceIcons = space?.customRecordEvents ?: emptyList()
                            
                            val customEvents = (spaceIcons + historyIcons + localCustomEvents)
                                .distinct()
                                .filter { icon -> defaultEvents.none { it.first == icon } }
                                .map { it to it }

                            val allEvents = defaultEvents + customEvents

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(allEvents) { (icon, label) ->
                                    val isOngoing = data.events.any { it.iconOrText == icon && it.endTime == null }
                                    // [修改] 获取专属随机色
                                    val eventColor = remember(icon) { getEventColor(icon, theme.primaryColor) }
                                    
                                    EventButton(
                                        icon = icon, label = label,
                                        activeColor = eventColor,
                                        isOngoing = isOngoing,
                                        onClick = {
                                            val ongoingEvent = data.events.find { it.iconOrText == icon && it.endTime == null }
                                            if (ongoingEvent != null) {
                                                val updated = data.events.map { if (it.id == ongoingEvent.id) it.copy(endTime = System.currentTimeMillis()) else it }
                                                saveData(data.copy(events = updated))
                                            } else {
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
            // [修改] 激活时背景变为专属颜色的 alpha = 0.5f
            modifier = Modifier.size(48.dp).background(if (isOngoing) activeColor.copy(alpha = 0.5f) else Color.White, CircleShape),
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

        // [修改] 加大左侧文字与线条的距离，防止挤压
        val startX = 76.dp.toPx() 
        val endX = canvasW - 40.dp.toPx()
        val distinctEvents = data.events.map { it.iconOrText }.distinct()
        
        // [修改] 将线宽增加到 24dp
        val strokeW = 24.dp.toPx()

        // 绘制宽轨道背景线
        for (i in 0..3) {
            val y = lineSpacing * (i + 1)
            drawContext.canvas.nativeCanvas.drawText(
                "${String.format("%02d", i * 6)}:00",
                12.dp.toPx(), y + 4.dp.toPx(), // 文字靠左
                android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12.sp.toPx(); typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = android.graphics.Paint.Align.LEFT }
            )
            drawLine(
                color = bgColor.copy(alpha = 0.4f + (i * 0.1f)),
                start = Offset(startX, y), end = Offset(endX, y),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        fun getProgress(millis: Long): Float = ((millis - dayStart).coerceIn(0L, 86400000L)).toFloat() / 86400000f
        fun getSection(progress: Float): Int = (progress * 4).toInt().coerceIn(0, 3)
        fun getX(progress: Float): Float = startX + (endX - startX) * ((progress * 4) - getSection(progress))
        fun getY(section: Int): Float = lineSpacing * (section + 1)

        fun drawActiveBlock(startMillis: Long, endMillis: Long, eventColor: Color, eventIndex: Int, isOngoing: Boolean) {
            val startP = getProgress(startMillis)
            val endP = getProgress(endMillis)
            if (startP >= endP && !isOngoing) return
            
            val s = getSection(startP)
            val e = getSection(endP)
            
            // [核心修改] 并发渲染算法：使用 DashPathEffect 使同段线条呈现颜色交替的斑马纹条带，完美融合！
            // 第一个事件实线，后面的事件用虚线叠加，产生交织的一条粗线。
            val pathEffect = if (eventIndex == 0) null else PathEffect.dashPathEffect(xfloatArrayOf(40f, 40f), (eventIndex * 20f))
            
            if (s == e) {
                drawLine(eventColor, Offset(getX(startP), getY(s)), Offset(getX(endP), getY(e)), strokeWidth = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect)
            } else {
                drawLine(eventColor, Offset(getX(startP), getY(s)), Offset(endX, getY(s)), strokeWidth = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect)
                for (i in (s + 1) until e) drawLine(eventColor, Offset(startX, getY(i)), Offset(endX, getY(i)), strokeWidth = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect)
                drawLine(eventColor, Offset(startX, getY(e)), Offset(getX(endP), getY(e)), strokeWidth = strokeW, cap = StrokeCap.Round, pathEffect = pathEffect)
            }
            
            // [修改] 绘制起点端点：加粗白边距，使紧挨着的点界限分明
            drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(getX(startP), getY(s)))
            drawCircle(eventColor, radius = 5.dp.toPx(), center = Offset(getX(startP), getY(s)))
            
            val endPoint = Offset(getX(endP), getY(e))
            if (!isOngoing) {
                drawCircle(Color.White, radius = 9.dp.toPx(), center = endPoint)
                drawCircle(eventColor, radius = 5.dp.toPx(), center = endPoint)
            } else {
                drawCircle(Color.White.copy(alpha = pulseAlpha), radius = 12.dp.toPx(), center = endPoint)
                drawCircle(Color.Red, radius = 6.dp.toPx(), center = endPoint)
            }
        }

        for (event in data.events) {
            val eventIndex = distinctEvents.indexOf(event.iconOrText)
            val isOngoing = event.endTime == null && !data.isStopped
            val calculateEnd = event.endTime ?: currentMillis
            
            // 获取该事件专属颜色
            val eventColor = getEventColor(event.iconOrText, activeColor)

            drawActiveBlock(event.startTime, calculateEnd, eventColor, eventIndex, isOngoing)
            
            // [修改] Emoji 上下左右交错排列算法
            // 偶数在上方，奇数在下方
            val isUp = eventIndex % 2 == 0
            val iconYOffset = if (isUp) -24.dp.toPx() else 36.dp.toPx() // 下方的稍微多留点给粗线

            drawContext.canvas.nativeCanvas.drawText(
                event.iconOrText, getX(getProgress(event.startTime)), getY(getSection(getProgress(event.startTime))) + iconYOffset,
                android.graphics.Paint().apply { textSize = 18.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}
