package com.roroi.taplog.daily.subScreen.record

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    
    // UI状态真值，解析异常时返回空记录以防崩溃
    var data by remember { 
        mutableStateOf(
            try { Json.decodeFromString<RecordDayData>(entry.content) } 
            catch (e: Exception) { RecordDayData() }
        ) 
    }

    // 辅助保存函数：保证每次记录时完全覆写最新数据，解决存储混乱
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
                    saveData(data.copy(isStopped = true))
                    onBack()
                }) { Text("确定停止", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    if (showAddEventDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("记录新事件", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = input, 
                    onValueChange = { if (it.length <= 4) input = it }, // 允许短文字或Emoji
                    label = { Text("输入单字/Emoji") }, 
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        saveData(data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), input)))
                    }
                    showAddEventDialog = false
                }) { Text("添加") }
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

            Column(modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                
                // 顶部状态提示
                if (data.isStopped) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ) {
                        Text("Record has ended", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 核心时间轴区域
                Surface(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(vertical = 16.dp)) {
                        TimelineCanvas(data, entry.timestamp, theme.primaryColor, theme.backgroundColor)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 底部操作面板
                if (!data.isStopped) {
                    Surface(
                        color = Color.White.copy(0.75f), 
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Activity", fontWeight = FontWeight.Bold, color = theme.onSurfaceColor, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                EventButton("🛏️", "Rest", theme.primaryColor) {
                                    saveData(data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "🛏️")))
                                }
                                EventButton("💻", "Work", theme.primaryColor) {
                                    saveData(data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "💻")))
                                }
                                EventButton("🎮", "Play", theme.primaryColor) {
                                    saveData(data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "🎮")))
                                }
                                EventButton("🚫", "None", Color.Gray) {
                                    // 插入空白事件，恢复底色空白（无活动状态）
                                    saveData(data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "")))
                                }
                                EventButton("➕", "Other", theme.primaryColor) { 
                                    showAddEventDialog = true 
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.85f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Stop Record Today", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp)) // 留出底部边距，防止被导航条遮挡
                }
            }
        }
    }
}

@Composable
fun EventButton(icon: String, label: String, activeColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = activeColor.copy(alpha = 0.8f))
    }
}

@Composable
fun TimelineCanvas(data: RecordDayData, entryTimestamp: Long, activeColor: Color, bgColor: Color) {
    // 呼吸动画不仅用于红点脉冲，还能作为每帧重绘Canvas的触发器（进度条逐帧精确更新）
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height
        val lineSpacing = canvasH / 5f

        val currentMillis = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply { 
            timeInMillis = entryTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startX = 60.dp.toPx()
        val endX = canvasW - 40.dp.toPx()

        // 绘制时间标签和灰底线
        for (i in 0..3) {
            val y = lineSpacing * (i + 1)
            val lineAlpha = 0.3f + (i * 0.15f)
            
            drawContext.canvas.nativeCanvas.drawText(
                "${String.format("%02d", i * 6)}:00",
                16.dp.toPx(), 
                y + 4.dp.toPx(), // 微调 y 居中
                android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 12.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.LEFT
                }
            )

            drawLine(
                color = bgColor.copy(alpha = lineAlpha),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 10.dp.toPx(), 
                cap = StrokeCap.Round
            )
        }

        fun getProgress(millis: Long): Float = ((millis - dayStart).coerceIn(0L, 86400000L)).toFloat() / 86400000f
        fun getSection(progress: Float): Int = (progress * 4).toInt().coerceIn(0, 3)
        fun getX(progress: Float): Float {
            val section = getSection(progress)
            val sectionProgress = (progress * 4) - section
            return startX + (endX - startX) * sectionProgress
        }
        fun getY(section: Int): Float = lineSpacing * (section + 1)

        // 精确算法：支持在一行内画段，或者跨越多行连续画满。避免跨段时斜着连线
        fun drawActiveLine(startMillis: Long, endMillis: Long) {
            val startP = getProgress(startMillis)
            val endP = getProgress(endMillis)
            if (startP >= endP) return
            
            val s = getSection(startP)
            val e = getSection(endP)
            
            if (s == e) {
                drawLine(activeColor, Offset(getX(startP), getY(s)), Offset(getX(endP), getY(e)), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
            } else {
                drawLine(activeColor, Offset(getX(startP), getY(s)), Offset(endX, getY(s)), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                for (i in (s + 1) until e) {
                    drawLine(activeColor, Offset(startX, getY(i)), Offset(endX, getY(i)), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
                }
                drawLine(activeColor, Offset(startX, getY(e)), Offset(getX(endP), getY(e)), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
            }
        }

        // 绘制实际高亮活跃时间段
        var lastEvent: RecordEvent? = null
        for (event in data.events) {
            if (lastEvent != null && lastEvent.iconOrText.isNotBlank()) {
                drawActiveLine(lastEvent.timeMillis, event.timeMillis)
            }
            lastEvent = event
        }
        
        // 画到当前时间
        if (!data.isStopped && lastEvent != null && lastEvent.iconOrText.isNotBlank()) {
            drawActiveLine(lastEvent.timeMillis, currentMillis)
        }

        // 绘制事件图标和节点
        for (event in data.events) {
            if (event.iconOrText.isNotBlank()) {
                val p = getProgress(event.timeMillis)
                val s = getSection(p)
                val x = getX(p)
                val y = getY(s)
                
                // 外圈白，内圈主色，非常精致的节点
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(x, y))
                drawCircle(activeColor, radius = 4.dp.toPx(), center = Offset(x, y))

                // 在节点上方完美居中画 Emoji 标识
                drawContext.canvas.nativeCanvas.drawText(
                    event.iconOrText,
                    x,
                    y - 14.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 20.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        // 绘制当前正在进行的闪烁点指示
        if (!data.isStopped) {
            val p = getProgress(currentMillis)
            val s = getSection(p)
            val x = getX(p)
            val y = getY(s)
            
            drawCircle(Color.White.copy(alpha = pulseAlpha), radius = 8.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.Red, radius = 5.dp.toPx(), center = Offset(x, y))
        }
    }
}
