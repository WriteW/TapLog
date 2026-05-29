package com.roroi.taplog.daily.subScreen.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
    var data by remember { mutableStateOf(try { Json.decodeFromString<RecordDayData>(entry.content) } catch (e: Exception) { RecordDayData() }) }

    var showStopDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    // 弹窗确认终止
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("停止记录", color = Color.Red) },
            text = { Text("确定要停止这一天的记录吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    val newData = data.copy(isStopped = true)
                    viewModel.updateEntry(entry.copy(content = Json.encodeToString(newData)))
                    onBack()
                }) { Text("确定停止", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    // 弹窗增加自定义事件
    if (showAddEventDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("记录新事件") },
            text = {
                OutlinedTextField(
                    value = input, onValueChange = { if (it.length <= 2) input = it }, // 限制一到两个字
                    label = { Text("输入单字/Emoji") }, singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newEvent = RecordEvent(System.currentTimeMillis(), input)
                    val newData = data.copy(events = data.events + newEvent)
                    data = newData
                    viewModel.updateEntry(entry.copy(content = Json.encodeToString(newData)))
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
                title = { Text("Entire Timeline", color = theme.primaryColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = theme.onSurfaceColor) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = theme.backgroundColor.copy(0.6f))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DailyDynamicBackground(theme)
            GlassmorphismBackground(modifier = Modifier.fillMaxSize(), alpha = 0.5f)

            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                // Part 1: 四条线绘制
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    TimelineCanvas(data, theme.primaryColor, theme.backgroundColor)
                }

                // 如果未停止，显示面板和终止按钮
                if (!data.isStopped) {
                    Spacer(Modifier.height(16.dp))
                    // Part 2: 常用操作面板
                    Surface(color = Color.White.copy(0.7f), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                            EventButton("🛏️", "Rest") {
                                data = data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "🛏️"))
                                viewModel.updateEntry(entry.copy(content = Json.encodeToString(data)))
                            }
                            EventButton("💻", "Work") {
                                data = data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "💻"))
                                viewModel.updateEntry(entry.copy(content = Json.encodeToString(data)))
                            }
                            EventButton("🚫", "None") {
                                data = data.copy(events = data.events + RecordEvent(System.currentTimeMillis(), "")) // 空代表停止活动恢复底色
                                viewModel.updateEntry(entry.copy(content = Json.encodeToString(data)))
                            }
                            EventButton("➕", "Other") { showAddEventDialog = true }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    // Part 3: 停止按钮
                    Button(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.8f)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Stop Record", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EventButton(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Text(icon, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun TimelineCanvas(data: RecordDayData, activeColor: Color, bgColor: Color) {
    val labels = listOf("00:00", "06:00", "12:00", "18:00")

    // 强制不断刷新以推进进度点
    var trigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60000); trigger++ } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        trigger // 读取刷新状态
        val canvasW = size.width
        val canvasH = size.height
        val lineSpacing = canvasH / 5f

        val now = Calendar.getInstance()
        val currentMillis = now.timeInMillis
        val dayStart = now.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis

        // 绘制4根底色横线
        for (i in 0..3) {
            val y = lineSpacing * (i + 1)
            // 越晚线越亮，通过 alpha 提亮
            val lineAlpha = 0.3f + (i * 0.2f)
            drawLine(
                color = bgColor.copy(alpha = lineAlpha),
                start = Offset(40.dp.toPx(), y),
                end = Offset(canvasW - 20.dp.toPx(), y),
                strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round
            )
        }

        // 把一天分为 4 段，每段占一条线，计算 x 坐标和 y 坐标
        fun getPosByTime(timeMillis: Long): Offset {
            val progress = ((timeMillis - dayStart).coerceIn(0L, 86400000L)).toFloat() / 86400000f
            val section = (progress * 4).toInt().coerceIn(0, 3)
            val sectionProgress = (progress * 4) - section
            val y = lineSpacing * (section + 1)
            val startX = 40.dp.toPx()
            val endX = canvasW - 20.dp.toPx()
            val x = startX + (endX - startX) * sectionProgress
            return Offset(x, y)
        }

        // 绘制覆盖色事件块
        var lastEvent: RecordEvent? = null
        for (event in data.events) {
            if (lastEvent != null && lastEvent.iconOrText.isNotBlank()) {
                val startPos = getPosByTime(lastEvent.timeMillis)
                val endPos = getPosByTime(event.timeMillis)
                // 简化处理：如果跨线了，这里连线会斜过去。真正的纯横线渲染算法会略微复杂，为了满足美观，我们使用渐变或直接连斜线
                drawLine(activeColor, startPos, endPos, strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
            }
            lastEvent = event
        }
        // 如果未停止，画到当前时间
        if (!data.isStopped && lastEvent != null && lastEvent.iconOrText.isNotBlank()) {
            val startPos = getPosByTime(lastEvent.timeMillis)
            val currentPos = getPosByTime(currentMillis)
            drawLine(activeColor, startPos, currentPos, strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
        }

        // 绘制当前时间指示点（小但明显）
        if (!data.isStopped) {
            val currentPos = getPosByTime(currentMillis)
            drawCircle(Color.Red, radius = 6.dp.toPx(), center = currentPos)
        }
    }
}