package com.roroi.taplog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.roroi.taplog.ui.theme.TapLogTheme
import org.json.JSONArray
import java.io.File
import kotlinx.coroutines.delay
import com.google.accompanist.flowlayout.FlowRow
import org.json.JSONException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.LocalDate.now

class IsOkay : ComponentActivity() {
    // 使用 SnapshotStateList - Compose 会观察它的改动并触发重组
    private var okayList = mutableStateListOf<StateData>()

    @SuppressLint("RememberReturnType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        okayList.clear()
        okayList.addAll(loadStateList(this))

        // 读取并填充初始状态
        setContent {
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer
            val mainColor = remember { mutableStateOf(primaryContainer) }
            TapLogTheme {
                Scaffold { innerPadding ->
                    MainActivity(innerPadding, okayList, mainColor)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        writeState(okayList, this)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainActivity(
    innerPaddingValues: PaddingValues,
    okayList: MutableList<StateData>,
    mainColor: MutableState<Color>
) {
    mainColor.value = MaterialTheme.colorScheme.primaryContainer
    val activity = LocalContext.current.findActivity()
    DisposableEffect(mainColor) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                darkenColor(mainColor.value, 0.1f).toArgb() // 背景色
            ),
            navigationBarStyle = SystemBarStyle.dark(
                darkenColor(mainColor.value, 0.1f).toArgb()
            )
        )
        onDispose { }
    }
    val context = LocalContext.current

    // 可观察的时间状态，每秒刷新一次
    val timeState = remember { mutableStateOf(timeUntilNextBoundary()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            timeState.value = timeUntilNextBoundary()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "离下个时间点还有: ${timeState.value}",
            fontSize = 25.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(2f))
        Column(
            modifier = Modifier
                .padding(innerPaddingValues)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "节奏怎么样？", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Button(
                    onClick = {
                        okayList.add(StateData(getTimePeriod(), true, now().toString()))
                        writeState(okayList, context) // 把写入状态放到点击处理里
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text(text = "当然")
                }
                Spacer(modifier = Modifier.width(50.dp))
                Button(
                    onClick = {
                        okayList.add(StateData(getTimePeriod(), false, now().toString()))
                        writeState(okayList, context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(text = "不好")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        listOf("上午", "下午", "晚上", "凌晨").forEach {
            var showSquare = false
            Text(text = "$it：")
            FlowRow(
                mainAxisAlignment = FlowMainAxisAlignment.Center,
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                // okayList 是 SnapshotStateList，修改后会触发重新组合，方块会实时出现
                okayList.forEach { value ->
                    if (value.timePeriod == it && value.now == now().toString()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (value.isOkay) Color.Green else Color.Red)
                        )
                        showSquare = true
                    }
                }
            }
            if (!showSquare) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Gray)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .clickable {
                    okayList.clear()
                    writeState(okayList, context)
                }
                .height(45.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "重置", color = Color.White)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun writeState(okayList: MutableList<StateData>, context: Context) {
    val f = File(context.getExternalFilesDir(null), "isOkay/state.txt")
    f.parentFile?.mkdirs()
    val targetJson = JSONArray()
    okayList.forEach { isOkay ->
        targetJson.put(JSONArray().put(isOkay.timePeriod).put(isOkay.isOkay).put(isOkay.now))
    }
    f.writeText(targetJson.toString())
    Log.d("isOkay", f.readText())
}

@RequiresApi(Build.VERSION_CODES.O)
fun loadStateList(context: Context): List<StateData> {
    val f = File(context.getExternalFilesDir(null), "isOkay/state.txt")
    val targetList = mutableListOf<StateData>()
    if (f.exists() && isJson(f.readText())) {
        val jsonArray = JSONArray(f.readText())
        for (i in 0 until jsonArray.length()) {
            val jarry = jsonArray.getJSONArray(i)
            val timePeriod = jarry.getString(0)
            val state = jarry.getBoolean(1)
            val date = try {
                jarry.getString(2)
            } catch (_: JSONException) {
                now().toString()
            }
            targetList.add(StateData(timePeriod, state, date))
        }
        return targetList
    }
    return listOf()
}

@RequiresApi(Build.VERSION_CODES.O)
fun timeUntilNextBoundary(): String {
    val now = LocalDateTime.now()
    val boundaries = listOf(
        LocalTime.of(12, 0),
        LocalTime.of(18, 0),
        LocalTime.of(0, 0)
    )

    val nextBoundaryTime = boundaries
        .map { boundary ->
            if (now.toLocalTime().isBefore(boundary)) {
                now.with(boundary)
            } else {
                now.plusDays(1).with(boundary)
            }
        }
        .minByOrNull { it }
        ?: now

    val duration = Duration.between(now, nextBoundaryTime)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTimePeriod(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 0..5 -> "凌晨"
        in 6..11 -> "上午"
        in 12..17 -> "下午"
        else -> "晚上"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun IsOkayPre() {
    val mainColor = remember { mutableStateOf(Color.Green) }
    TapLogTheme {
        Scaffold { innerPadding ->
            MainActivity(
                innerPadding,
                mutableListOf(
                    StateData(getTimePeriod(), true, now = now().toString()),
                    StateData(getTimePeriod(), false, now = now().toString()),
                    StateData(getTimePeriod(), true, now = now().toString()),
                    StateData(getTimePeriod(), false, now = now().toString())
                ),
                mainColor
            )
        }
    }
}

data class StateData(val timePeriod: String, val isOkay: Boolean, val now: String)
