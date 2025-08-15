package com.roroi.taplog

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.ui.theme.TapLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate.now
import java.util.Locale

class Tap : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapLogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TapP(innerPadding, false)
                }
            }
        }
    }
}

val godColor = Color(0xFFFF9800)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TapP(innerPadding: PaddingValues, isPre: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 状态：grid
    val grid = remember { mutableStateListOf<MutableList<Int>>() }
    // 状态：okGrid
    val okGrid = remember { mutableStateListOf<MutableList<Boolean>>() }
    // 状态：计数器
    var completeness by remember { mutableIntStateOf(0) }
    // 总得分
    var score by remember { mutableIntStateOf(0) }
    // 是否加载完成的标志
    var isReady by remember { mutableIntStateOf(0) }
    // 计算用时
    var timeForOne by remember { mutableFloatStateOf(0f) }
    var canAddTime by remember { mutableStateOf(false) }
    var minTime by remember { mutableFloatStateOf(0f) }
    var elapsedMs by remember { mutableLongStateOf(0L) }    // 已累计毫秒
    var lastUpdateMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    //浮空弹窗加载
    var showOverlay by remember { mutableStateOf(false) }
    //  其中的列表
    var list by remember { mutableStateOf(JSONArray()) }

    // 初始化或重置函数
    @RequiresApi(Build.VERSION_CODES.O)
    fun resetGame() {
        // 生成 1~16 洗牌列表
        val numbers = (1..16).shuffled()

        // 填入 grid
        grid.clear()
        repeat(4) { row ->
            grid.add(
                mutableStateListOf(
                    *List(4) { col -> numbers[row * 4 + col] }.toTypedArray()
                )
            )
        }

        // 填入 okGrid
        okGrid.clear()
        repeat(4) {
            okGrid.add(mutableStateListOf(false, false, false, false))
        }

        // 重置计数
        completeness = 0
        isReady = 1
        // —— 关键：一并重置计时相关状态 —— //
        elapsedMs = 0L
        timeForOne = 0f
        // 把 lastUpdateMs 设为当前，防止下一次循环把一个大间隔算进去
        lastUpdateMs = SystemClock.elapsedRealtime()
        canAddTime = true
    }

    fun saveGame() {
        if (!isPre && isReady == 1) saveTodayData(score, minTime, context)
    }

    // 完成一场游戏
    fun finishGame() {
        if (isReady == 1) {
            score += 1
            // 计算最短用时
            if (timeForOne < minTime || minTime == 0f) {
                minTime = timeForOne
            }
            // 保存部分数据
            saveGame()
        }
    }

    // 首次启动自动初始化
    LaunchedEffect(Unit) {
        // 加载今日数据
        val data = loadDayData(context, now().toString())
        if (data != null) {
            score = data.first
            minTime = data.second
            Log.d("日志", "今天的记录：score=$score, minTime=$minTime")
        } else {
            Log.d("日志", "今天没有记录")
        }
        if (!isPre) {
            delay(800)
        }
        resetGame()
    }
    // 精准计数哈

    LaunchedEffect(canAddTime) {
        // 以大循环来持续更新 UI；只有 canAddTime=true 时才会把时间加到 elapsedMs
        while (true) {
            delay(50L) // 50ms 刷新一次足够平滑且省电
            val now = SystemClock.elapsedRealtime()
            if (canAddTime) {
                // 把从 lastUpdateMs 到 now 的时间累加到 elapsedMs（ms）
                // 注意：lastUpdateMs 在暂停时会被重置为 now，避免误加
                elapsedMs += (now - lastUpdateMs)
                lastUpdateMs = now
                // 更新展示时间（秒）
                timeForOne = elapsedMs / 1000f
                if(timeForOne >= 999.99f) resetGame()
            } else {
                // 暂停时只更新 lastUpdateMs，避免下次恢复时把暂停期间也算进来
                lastUpdateMs = now
            }
        }
    }

    if (isReady == 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "加载中...",
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        return
    }

    Column {
        Box(
            modifier = Modifier
                .weight(3f)
                .padding(innerPadding)
                .border(4.dp, invertColor(MaterialTheme.colorScheme.primary)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp)
            ) {
                for (rowIndex in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (colIndex in 0 until 4) {
                            val isDone = okGrid[rowIndex][colIndex]
                            val number = grid[rowIndex][colIndex]
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, Color.Black)
                                    .background(if (isDone) Color.Green else MaterialTheme.colorScheme.background)
                                    .clickable(enabled = !isDone) {
                                        if (number == completeness + 1) {
                                            okGrid[rowIndex][colIndex] = true
                                            completeness++
                                            if (completeness == 16) {
                                                canAddTime = false
                                                coroutineScope.launch {
                                                    delay(500)
                                                    finishGame()
                                                    resetGame()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isDone) {
                                    Text(text = number.toString())
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1F)
                .fillMaxSize()
                .border(4.dp, MaterialTheme.colorScheme.primary),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "今日得分：$score",
                textAlign = TextAlign.Center,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.primary
            )
            StableCenteredNumber(timeForOne.toDouble())
            val minTimeText = String.format(Locale.CHINA, "%.2f", minTime)
            Text(
                text = "今日最少用时：$minTimeText s",
                fontSize = 18.sp,
                color = godColor
            )
            Row {
                Button(onClick = { resetGame() }) {
                    Text(text = "重置")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(onClick = {
                    list = getAllJsonList(context)
                    showOverlay = true
                }) {
                    Text(text = "历史记录")
                }
            }
        }
    }
    HistoryOverlay(
        showOverlay = showOverlay,
        onDismiss = { showOverlay = false },
        list = list
    )
}

@Composable
fun StableCenteredNumber(timeForOne: Double) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle =
        TextStyle(fontSize = 22.sp, fontFamily = FontFamily.Monospace)

    val measureResult = textMeasurer.measure(text = "999.99", style = textStyle)
    val measuredPx = measureResult.size.width
    val maxWidthDp = with(LocalDensity.current) { measuredPx.toDp() }

    val targetNumber = String.format(Locale.CHINA, "%.2f", timeForOne)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("本次用时：", fontSize = 22.sp, color = Color.Gray)
        Box(
            modifier = Modifier.width(maxWidthDp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = targetNumber,
                style = textStyle,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        Text(" s", fontSize = 22.sp, color = Color.Gray)
    }
}


// 浮空显示历史记录
@Composable
fun HistoryOverlay(showOverlay: Boolean, onDismiss: () -> Unit, list: JSONArray) {
    val context = LocalContext.current
    val data = getBestScoreAndMinTime(context)

    val rList = List(list.length()) { index ->
        list.getJSONObject(index) // 或者 getString(index)，看你的数据结构
    }
    val scale by animateFloatAsState(targetValue = if (showOverlay) 1f else 0.6f)
    val backgroundColor by animateColorAsState(
        targetValue = if (showOverlay) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(
            alpha = 0.5f
        )
    )
    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .scale(scale)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .size(300.dp, 380.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(4.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    .clickable(enabled = false) {}
            ) {
                Text(
                    text = "在此查询之前的分数\n（注：现在游戏还在运行）",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(4.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(5f)
                        .padding(4.dp)
                ) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(text = "历史最佳得分：${data.first}", color = godColor)
                        }
                    }
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "历史最少用时：${
                                    String.format(
                                        Locale.CHINA,
                                        "%.2f",
                                        data.second
                                    )
                                } s", color = Color.Gray
                            )
                        }
                    }
                    item {
                        LazyColumn(
                            modifier = Modifier
                                .height(220.dp)
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color.Gray.copy(alpha = 0.5f))
                        ) {
                            items(rList) { obj ->
                                Text(
                                    text = "日期：${obj.getString("date")}，最高得分：${obj.getInt("score")}，最少用时：${
                                        String.format(
                                            Locale.CHINA,
                                            "%.2f",
                                            obj.getDouble("minTime").toFloat()
                                        )
                                    } s", fontSize = 10.sp
                                )
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            Text(
                                "-点击浮窗外可关闭浮窗-",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

        }
    }

}

fun invertColor(color: Color): Color {
    return Color(
        red = 1f - color.red,
        green = 1f - color.green,
        blue = 1f - color.blue,
        alpha = color.alpha
    )
}

@RequiresApi(Build.VERSION_CODES.O)
fun saveTodayData(score: Int, minTime: Float, context: Context) {
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    file.parentFile?.mkdirs()
    val today = now().toString()

    val list = getAllJsonList(context)

    // 检查是否已有今天的数据
    var foundToday = false
    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        if (obj.getString("date") == today) {
            obj.put("score", score)
            obj.put("minTime", minTime)
            foundToday = true
            break
        }
    }

    // 如果没有找到今天的记录，新增
    if (!foundToday) {
        val newObj = JSONObject()
        newObj.put("score", score)
        newObj.put("minTime", minTime)
        newObj.put("date", today)
        list.put(newObj)
    }

    // 写回文件（覆盖）
    file.writeText(list.toString())
}

fun getAllJsonList(context: Context): JSONArray {
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    if (!file.exists()) return JSONArray()

    val jsonString = file.readText()
    return JSONArray(jsonString)
}

@RequiresApi(Build.VERSION_CODES.O)
fun loadDayData(context: Context, day: String): Pair<Int, Float>? { // first -> score, second -> minTime
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    if (!file.exists()) return null

    val list = getAllJsonList(context)

    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        if (obj.getString("date") == day) {
            val score = obj.getInt("score")
            val minTime = obj.getDouble("minTime").toFloat()
            return Pair(score, minTime)
        }
    }

    return null  // 没找到今天的数据
}

fun getBestScoreAndMinTime(context: Context): Pair<Int, Float> {
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    if (!file.exists()) return Pair(0, 0f)

    val jsonString = file.readText()
    val list = JSONArray(jsonString)

    var maxScore = Int.MIN_VALUE
    var minTime = Float.MAX_VALUE

    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        val score = obj.getInt("score")
        val time = obj.getDouble("minTime").toFloat()

        if (score > maxScore) maxScore = score
        if (time < minTime) minTime = time
    }

    // 如果文件为空，返回 null
    return if (list.length() == 0) Pair(0, 0f) else Pair(maxScore, minTime)
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun TapPr() {
    TapLogTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            TapP(innerPadding, true)
        }
    }
}
