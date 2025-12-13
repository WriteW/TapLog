package com.roroi.taplog

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
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

    // 【新加入的状态】：游戏是否已经开始（点击了数字1）
    var gameStarted by remember { mutableStateOf(false) }

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
        val numbers = (1..16).shuffled()
        grid.clear()
        repeat(4) { row ->
            grid.add(
                mutableStateListOf(
                    *List(4) { col -> numbers[row * 4 + col] }.toTypedArray()
                )
            )
        }

        okGrid.clear()
        repeat(4) {
            okGrid.add(mutableStateListOf(false, false, false, false))
        }

        // 重置游戏核心状态
        completeness = 0
        isReady = 1
        gameStarted = false // 重置开始标志

        // 重置计时相关
        elapsedMs = 0L
        timeForOne = 0f
        canAddTime = false // 开局不计时，等点1
        lastUpdateMs = SystemClock.elapsedRealtime()
    }

    fun saveGame() {
        if (!isPre && isReady == 1) saveTodayData(score, minTime, context)
    }

    fun finishGame() {
        if (isReady == 1) {
            score += 1
            if (timeForOne < minTime || minTime == 0f) {
                minTime = timeForOne
            }
            saveGame()
        }
    }

    LaunchedEffect(Unit) {
        val data = loadDayData(context, now().toString())
        if (data != null) {
            score = data.first
            minTime = data.second
        }
        if (!isPre) {
            delay(300)
        }
        resetGame()
    }

    LaunchedEffect(canAddTime) {
        while (true) {
            delay(50L)
            val now = SystemClock.elapsedRealtime()
            if (canAddTime) {
                elapsedMs += (now - lastUpdateMs)
                lastUpdateMs = now
                timeForOne = elapsedMs / 1000f
                if(timeForOne >= 999.99f) resetGame()
            } else {
                lastUpdateMs = now
            }
        }
    }

    if (isReady == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "加载中...", fontSize = 40.sp, color = MaterialTheme.colorScheme.primaryContainer)
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
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                for (rowIndex in 0 until 4) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (colIndex in 0 until 4) {
                            val isDone = okGrid[rowIndex][colIndex]
                            val number = grid[rowIndex][colIndex]

                            // 逻辑判断：如果是数字1，或者是已经开始游戏了，或者是已经点过的，才显示数字内容
                            // 否则显示为黑色背景，且不透出文字
                            val shouldShowNumber = (number == 1 || gameStarted || isDone)

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .border(1.dp, Color.Black)
                                    .background(
                                        when {
                                            isDone -> Color.Green
                                            !shouldShowNumber -> Color.Black // 未点击1之前，非1格子全黑
                                            else -> MaterialTheme.colorScheme.background
                                        }
                                    )
                                    .clickable(enabled = !isDone) {
                                        if (number == completeness + 1) {
                                            // 如果点的是1，触发开始逻辑
                                            if (number == 1 && !gameStarted) {
                                                gameStarted = true
                                                canAddTime = true
                                                lastUpdateMs = SystemClock.elapsedRealtime()
                                            }

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
                                // 只有在应该显示且未完成时显示数字内容
                                if (shouldShowNumber && !isDone) {
                                    Text(
                                        text = number.toString(),
                                        color = if (number == 1 && !gameStarted) Color.Red else Color.Unspecified, // 把1标红突出，也可以去掉
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 下半部分 UI 保持不变
        Column(
            modifier = Modifier
                .weight(1F)
                .fillMaxSize()
                .border(4.dp, MaterialTheme.colorScheme.primary),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("今日得分：$score", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
            StableCenteredNumber(timeForOne.toDouble())
            val minTimeText = String.format(Locale.CHINA, "%.2f", minTime)
            Text(text = "今日最少用时：$minTimeText s", fontSize = 18.sp, color = godColor)
            Row {
                Button(onClick = { resetGame() }) { Text(text = "重置") }
                Spacer(modifier = Modifier.width(6.dp))
                Button(onClick = {
                    list = getAllJsonList(context)
                    showOverlay = true
                }) { Text(text = "历史记录") }
            }
        }
    }
    HistoryOverlay(showOverlay = showOverlay, onDismiss = { showOverlay = false }, list = list)
}

// ---------------------------------------------------------
// 后面的辅助函数（StableCenteredNumber, HistoryOverlay 等）保持原样即可
// ---------------------------------------------------------

@Composable
fun StableCenteredNumber(timeForOne: Double) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(fontSize = 22.sp, fontFamily = FontFamily.Monospace)
    val measureResult = textMeasurer.measure(text = "999.99", style = textStyle)
    val maxWidthDp = with(LocalDensity.current) { measureResult.size.width.toDp() }
    val targetNumber = String.format(Locale.CHINA, "%.2f", timeForOne)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("本次用时：", fontSize = 22.sp, color = Color.Gray)
        Box(modifier = Modifier.width(maxWidthDp), contentAlignment = Alignment.Center) {
            Text(text = targetNumber, style = textStyle, color = Color.Gray, textAlign = TextAlign.Center)
        }
        Text(" s", fontSize = 22.sp, color = Color.Gray)
    }
}

@Composable
fun HistoryOverlay(showOverlay: Boolean, onDismiss: () -> Unit, list: JSONArray) {
    val context = LocalContext.current
    val data = getBestScoreAndMinTime(context)
    val rList = List(list.length()) { index -> list.getJSONObject(index) }
    val scale by animateFloatAsState(targetValue = if (showOverlay) 1f else 0.6f)

    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
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
                    modifier = Modifier.weight(1f).fillMaxSize().padding(4.dp).border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LazyColumn(modifier = Modifier.weight(5f).padding(4.dp)) {
                    item {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "历史最佳得分：${data.first}", color = godColor)
                        }
                    }
                    item {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "历史最少用时：${String.format(Locale.CHINA, "%.2f", data.second)} s", color = Color.Gray)
                        }
                    }
                    item {
                        LazyColumn(
                            modifier = Modifier.height(220.dp).fillMaxWidth().padding(16.dp).background(Color.Gray.copy(alpha = 0.5f))
                        ) {
                            items(rList) { obj ->
                                Text(
                                    text = "日期：${obj.getString("date")}，最高得分：${obj.getInt("score")}，最少用时：${
                                        String.format(Locale.CHINA, "%.2f", obj.getDouble("minTime").toFloat())
                                    } s", fontSize = 10.sp
                                )
                            }
                        }
                    }
                    item {
                        Text("-点击浮窗外可关闭浮窗-", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

fun invertColor(color: Color): Color {
    return Color(red = 1f - color.red, green = 1f - color.green, blue = 1f - color.blue, alpha = color.alpha)
}

@RequiresApi(Build.VERSION_CODES.O)
fun saveTodayData(score: Int, minTime: Float, context: Context) {
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    file.parentFile?.mkdirs()
    val today = now().toString()
    val list = getAllJsonList(context)
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
    if (!foundToday) {
        val newObj = JSONObject().apply {
            put("score", score)
            put("minTime", minTime)
            put("date", today)
        }
        list.put(newObj)
    }
    file.writeText(list.toString())
}

fun getAllJsonList(context: Context): JSONArray {
    val file = File(context.getExternalFilesDir(null), "Tap/data.json")
    if (!file.exists()) return JSONArray()
    return JSONArray(file.readText())
}

@RequiresApi(Build.VERSION_CODES.O)
fun loadDayData(context: Context, day: String): Pair<Int, Float>? {
    val list = getAllJsonList(context)
    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        if (obj.getString("date") == day) {
            return Pair(obj.getInt("score"), obj.getDouble("minTime").toFloat())
        }
    }
    return null
}

fun getBestScoreAndMinTime(context: Context): Pair<Int, Float> {
    val list = getAllJsonList(context)
    var maxScore = 0
    var minTime = Float.MAX_VALUE
    if (list.length() == 0) return Pair(0, 0f)
    for (i in 0 until list.length()) {
        val obj = list.getJSONObject(i)
        val score = obj.getInt("score")
        val time = obj.getDouble("minTime").toFloat()
        if (score > maxScore) maxScore = score
        if (time < minTime && time > 0f) minTime = time
    }
    return Pair(maxScore, if(minTime == Float.MAX_VALUE) 0f else minTime)
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