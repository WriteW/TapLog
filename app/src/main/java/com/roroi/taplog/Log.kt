package com.roroi.taplog

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.ui.theme.TapLogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.time.LocalDate.now
import java.util.Locale

class Log : ComponentActivity() {
    var logList = mutableStateListOf<LogData>()
    var typeList = mutableStateListOf<String>()

    @SuppressLint("ContextCastToActivity", "UnrememberedMutableState")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TapLogTheme {
                val mainColor = MaterialTheme.colorScheme.inversePrimary
                val activity = LocalContext.current.findActivity()

                DisposableEffect(mainColor) {
                    activity?.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(
                            darkenColor(mainColor, 0.3f).toArgb() // 背景色
                        ),
                        navigationBarStyle = SystemBarStyle.dark(
                            darkenColor(mainColor, 0.3f).toArgb()
                        )
                    )
                    onDispose { }
                }

                logList = remember { mutableStateListOf() }
                typeList = remember { mutableStateListOf("Daily", "Dream") }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        loadLog(activity ?: return@withContext, logList)
                        loadType(activity, typeList)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LogCard(innerPadding, logList, typeList, remember { mutableStateOf(mainColor) })
                }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        saveLog(this, logList)
        saveType(this, typeList)
    }
}

// 安全获取 Activity
fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LogCard(
    innerPaddingValues: PaddingValues,
    logList: SnapshotStateList<LogData>,
    typeList: SnapshotStateList<String>,
    mainColor: MutableState<Color>
) {
    mainColor.value = MaterialTheme.colorScheme.inversePrimary
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 动态变量
    var showOverlay by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val selectedType = remember { mutableStateOf(typeList[0]) }
    var showAddCategoryOverlay by remember { mutableStateOf(false) }
    // 底部弹窗
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var sheetToShow by remember {
        mutableStateOf(
            LogData(
                now().toString(),
                "\uD83D\uDE0A",
                "显示错误",
                "*"
            )
        )
    }
    val showEmojiChoice = remember { mutableStateOf(false) }
    val emojiSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val headEmoji = remember { mutableStateOf("\uD83D\uDE0A") }
    // 动画变量
    var showLogAnim by remember { mutableStateOf(false) }
    var showBoxAnim by remember { mutableStateOf(false) }
    val logScale by animateFloatAsState(
        targetValue = if (showLogAnim) 1f else 1.5f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
    )
    val boxScale by animateFloatAsState(
        targetValue = if (showBoxAnim) 1f else 0.5f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        finishedListener = { showLogAnim = true }
    )
    // 删除分类
    var showDeleteDialog by remember { mutableStateOf(false) }
    val typeToDelete = remember { mutableStateListOf<String>() }

    @Composable
    fun LogItemRow(
        item: LogData, // 你的数据类型
        isLast: Boolean
    ) {
        val isOverFlow = remember { mutableStateOf(false) }
        var showRemoveItemExpand by remember { mutableStateOf(false) }

        Column {
            Row {
                Spacer(modifier = Modifier.width(30.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Gray)
                        .combinedClickable(
                            onClick = {
                                if (isOverFlow.value) {
                                    sheetToShow = item
                                    showSheet = true
                                    scope.launch {
                                        sheetState.show()
                                    }
                                }
                            },
                            onLongClick = {
                                showRemoveItemExpand = true
                            }
                        )
                ) {
                    DropdownMenu(
                        expanded = showRemoveItemExpand,
                        onDismissRequest = { showRemoveItemExpand = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                logList.remove(item)
                                return@DropdownMenuItem
                            })
                    }
                    Text(
                        text = "${item.head}: ${item.content}",
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        onTextLayout = { layoutResult ->
                            isOverFlow.value = layoutResult.hasVisualOverflow
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (isLast) 0.dp else 4.dp))
        }
    }
    LaunchedEffect(Unit) { // 用户可见时
        showBoxAnim = true
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPaddingValues)
    ) {
        // 分类及其选择
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(mainColor.value)
                .height(50.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "选择Log种类",
                    tint = invertColor(MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = selectedType.value,
                    modifier = Modifier
                        .weight(1f)
                        .offset((-24).dp, 0.dp),
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center
                )

            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                typeList.forEach { type ->
                    if (type != selectedType.value) {
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { selectedType.value = type
                                expanded = false }
                        )
                    }
                }
                TextButton(onClick = {
                    showAddCategoryOverlay = true
                    expanded = false
                }) {
                    Text(
                        text = "添加分类",
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = {
                    showDeleteDialog = true
                    expanded = false
                }) {
                    Text(
                        text = "删除分类",
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        // 添加日志
        Box( // Button
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
                .background(lightenColor(mainColor.value, 0.3f))
                .clickable {
                    showOverlay = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加",
                tint = invertColor(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(48.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
            .scale(boxScale)
                .padding(25.dp)
                .clip(RoundedCornerShape(16.dp))     // 圆角
                .background(
                    darkenColor(
                        MaterialTheme.colorScheme.primaryContainer,
                        0.15f
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                                        .scale(logScale)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(darkenColor(MaterialTheme.colorScheme.primaryContainer, 0.1f))
                    .padding(10.dp)
            ) {
                val selected = selectedType.value

                // 按时间降序排列日志
                val sortedLogs = logList.sortedByDescending { it.time }

                // 原来的排序得到不可变列表 -> 转成可变列表
                val filteredLogs = sortedLogs.filter { it.type == selected }.toMutableList()

                val scoreList = getAllJsonList(context)
                val scoreDates = mutableSetOf<String>()

                for (i in 0 until scoreList.length()) {
                    val obj = scoreList.getJSONObject(i)
                    val date = obj.getString("date")
                    scoreDates.add(date)
                }

                val existingDates = filteredLogs.map { it.time }.toSet()
                val missingDates = scoreDates - existingDates

                // 插入“虚拟日志”
                missingDates.forEach { date ->
                    filteredLogs.add(LogData(time = date, type = selected, head = "", content = ""))
                }

                // 最后按时间排序
                val sortedLogsWithScores = filteredLogs.sortedByDescending { it.time }

                // 按时间分组
                val groupedByTime: Map<String, List<IndexedValue<LogData>>> =
                    sortedLogsWithScores.withIndex()
                        .filter { it.value.type == selected }
                        .groupBy { it.value.time }

               // 按时间顺序处理（降序）
                groupedByTime.keys
                    .sortedDescending()
                    .forEach { time ->
                        val entries = groupedByTime[time] ?: emptyList()

                        // Time header + Tap 成绩（只显示一次）
                        item {
                            TimeHeaderWithTapBox(
                                time = time,
                                context = context,
                                loadDayData = ::loadDayData // 传入函数引用，便于测试/复用
                            )
                        }

                        // 如果同一时间有多条（重复），则逐条渲染 LogItemRow；否则单条渲染也没问题
                        entries.reversed().forEachIndexed { idx, indexedValue ->
                            val log = indexedValue.value
                            val isLastInGroup = idx == entries.size - 1
                            if (log.content.isNotBlank() && log.head.isNotBlank()) {
                                item {
                                    Column {
                                        LogItemRow(log, isLastInGroup)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("请选择要删除的分类：")
                    LazyColumn(modifier = Modifier.height(50.dp * 3).background(Color.LightGray)) {
                        typeList.forEach {
                            item {
                                Button(
                                    onClick = {
                                        typeToDelete.add(it)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (typeToDelete.contains(it)) Color.Red else Color.Gray
                                    )
                                ) {
                                    Text(text = it)
                                }
                            }
                        }
                    }
                }
                   },
            confirmButton = {
                TextButton(onClick = {
                    if (typeToDelete.isNotEmpty()) {
                        typeToDelete.forEach {
                            if (typeList.contains(it)) typeList.remove(it)
                        }
                        showDeleteDialog = false
                        typeToDelete.clear()
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    typeToDelete.clear()
                }) { Text("取消") }
            }
        )
    }
    FloatAddLog(
        showOverlay = showOverlay,
        onDismiss = { showOverlay = false },
        logList,
        selectedType,
        headEmoji,
        showEmojiChoice,
        showEMOJI = { scope.launch { emojiSheetState.show() } }
    )
    FloatAddCategory(
        showAddCategoryOverlay = showAddCategoryOverlay,
        onDismiss = { showAddCategoryOverlay = false },
        typeList,
        selectedType
    )
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = sheetToShow.time, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(text = sheetToShow.head, fontSize = 34.sp)
                Text(text = sheetToShow.content, fontSize = 20.sp)
            }
        }
    }
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val boxColor by remember { mutableStateOf(primaryContainer) }
    var emojiValue by remember { mutableStateOf("") }
    if (showEmojiChoice.value) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiChoice.value = false },
            sheetState = emojiSheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        listOf(
                            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
                            "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
                            "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
                            "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "😎", "🤓",
                            "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦",
                            "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞",
                            "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿",
                            "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖",
                            "🥷", "🫒", "🫘", "🐢"
                        ).forEach {

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (headEmoji.value == it) invertColor(
                                            boxColor
                                        ) else boxColor
                                    )
                                    .clickable {
                                        headEmoji.value = it
                                    }
                            ) {
                                Text(text = it, fontSize = 28.sp)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .background(Color.Gray),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp) // 设置大小
                            )
                            TextField(
                                value = emojiValue,
                                onValueChange = { emojiValue = it },
                                modifier = Modifier.weight(2f)
                            )
                            Button(
                                onClick = {
                                    headEmoji.value = emojiValue
                                    showEmojiChoice.value = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("使用")
                            }
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(onClick = {
                            showEmojiChoice.value = false
                        }) {
                            Text(text = "确认")
                        }
                    }
                }
            }
        }
    }
}

fun darkenColor(color: Color, fraction: Float): Color {
    // fraction 范围 0~1，越大越接近黑色
    return lerp(color, Color.Black, fraction.coerceIn(0f, 1f))
}

fun lightenColor(color: Color, fraction: Float): Color {
    // fraction 范围0~1，越大越接近白色，变浅程度
    return lerp(color, Color.White, fraction.coerceIn(0f, 1f))
}

fun saveLog(context: Context, logList: SnapshotStateList<LogData>) {
    val f = File(context.getExternalFilesDir(null), "Log/data.json")
    f.parentFile?.mkdirs()
    val targetList = JSONArray()
    for (i in 0 until logList.size) {
        targetList.put(logList[i].toJSONObject())
    }
    f.writeText(targetList.toString())
    Log.d("Log内容", f.readText())
}

fun loadLog(context: Context, logList: SnapshotStateList<LogData>) {
    logList.clear()
    val f = File(context.getExternalFilesDir(null), "Log/data.json")
    if (f.exists() && isJson(f.readText())) { // 有的话
        val targetList = JSONArray(f.readText())
        for (i in 0 until targetList.length()) {
            try {
                val obj = getLogFormJSONObject(targetList[i])
                logList.add(obj)
            } catch (e: JSONException) {
                Log.e("JSONLoad", "第 $i 项解析失败: ${e.localizedMessage}", e)
            }
        }
    }
}

fun saveType(context: Context, typeList: SnapshotStateList<String>) {
    val f = File(context.getExternalFilesDir(null), "Log/type.json")
    f.parentFile?.mkdirs()
    val targetTypeList = JSONArray()
    typeList.forEach {
        targetTypeList.put(it)
    }
    f.writeText(targetTypeList.toString())
}

fun loadType(context: Context, typeList: SnapshotStateList<String>) {
    typeList.clear()
    val f = File(context.getExternalFilesDir(null), "Log/type.json")
    if (f.exists() && isJson(f.readText())) {
        val targetList = JSONArray(f.readText())
        for (i in 0 until targetList.length()) {
            typeList.add(targetList.getString(i))
        }
    } else {
        typeList.add("Daily")
        typeList.add("Dream")
        saveType(context, typeList)
    }
}

// 浮空显示添加今日Log
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FloatAddLog(
    showOverlay: Boolean,
    onDismiss: () -> Unit,
    logList: SnapshotStateList<LogData>,
    selectedType: MutableState<String>,
    headEmoji: MutableState<String>,
    showEmojiChoice: MutableState<Boolean>,
    showEMOJI: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scale = animateFloatAsState(targetValue = if (showOverlay) 1f else 0.6f)
    val backgroundColor = animateColorAsState(
        targetValue = if (showOverlay) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(
            alpha = 0.5f
        )
    )
    var content by remember { mutableStateOf("") }
    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor.value)
                .fillMaxSize()
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .size(350.dp, 400.dp)
                    .border(4.dp, Color.Black),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(200.dp, 50.dp)
                            .padding(top = 10.dp)
                            .border(4.dp, Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = now().toString(),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Box {
                        Text(
                            text = "日志",
                            fontSize = 35.sp,
                            fontStyle = FontStyle.Italic,
                            color = invertColor(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "头部:",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Text(text = headEmoji.value, fontSize = 24.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            showEmojiChoice.value = true
                            showEMOJI()
                        }) {
                            Text("选择")
                        }
                        Spacer(modifier = Modifier.weight(2f))
                        Text(
                            text = "分类:${selectedType.value}",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(3f))
                    }
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "内容:",
                            fontSize = 20.sp
                        )
                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .height(150.dp)
                        )
                    }
                    Button(onClick = {
                        // 保存并重新加载日志
                        logList.add(
                            LogData(
                                time = now().toString(),
                                head = headEmoji.value,
                                content = content,
                                type = selectedType.value
                            )
                        )
                        scope.launch {
                            saveLog(context, logList)
                        }
                        // 清空
                        headEmoji.value = "\uD83D\uDE0A"
                        content = ""
                        // 关闭浮窗
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "确认",
                            tint = Color.Green
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(lightenColor(Color.Red, 0.2f))
                        .align(Alignment.TopEnd)
                        .clickable {
                            // 清空
                            headEmoji.value = "\uD83D\uDE0A"
                            content = ""
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭浮窗"
                    )
                }
            }
        }
    }
}

// 浮窗添加Log分类
@Composable
fun FloatAddCategory(
    showAddCategoryOverlay: Boolean,
    onDismiss: () -> Unit,
    typeList: SnapshotStateList<String>,
    selectType: MutableState<String>
) {
    var category by remember { mutableStateOf("") }
    var showWarning by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(targetValue = if (showAddCategoryOverlay) 1f else 0.6f)
    val warningScale = animateFloatAsState(targetValue = if (showWarning) 1f else 0.6f)
    val backgroundColor = animateColorAsState(
        targetValue = if (showAddCategoryOverlay) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(
            alpha = 0f
        )
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AnimatedVisibility(
        visible = showAddCategoryOverlay,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.value)
                .scale(scale.value),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp, 100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(4.dp, Color.Black)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "分类：")
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            BasicTextField(
                                value = category,
                                onValueChange = { category = it },
                                singleLine = true,
                                modifier = Modifier.background(MaterialTheme.colorScheme.onSecondary),
                            )
                            if (category.isEmpty()) {
                                Text(
                                    text = "输入要添加的分类...",
                                    style = TextStyle(
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            if (category.isBlank()) {
                                if (showWarning) {
                                    scope.launch {
                                        showWarning = false
                                        delay(80)
                                        showWarning = true
                                    }
                                }
                                showWarning = true
                            } else {
                                typeList.add(category.trim())
                                selectType.value = category
                                saveType(context, typeList)
                                // 清空数据，准备退出
                                showWarning = false
                                category = ""
                                onDismiss()
                            }
                        }, modifier = Modifier
                            .offset(15.dp, 0.dp)
                            .border(2.dp, Color.Gray)
                    ) {
                        Text(text = "确认")
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(7.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(lightenColor(Color.Red, 0.2f))
                        .align(Alignment.TopEnd)
                        .clickable {
                            // 清空数据，准备退出
                            showWarning = false
                            category = ""
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭浮窗"
                    )
                }
            }
            if (showWarning) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 80.dp)
                        .scale(warningScale.value)
                        .offset(100.dp, (-150).dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(4.dp, Color.Black),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "空白分类不行",
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(7.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(lightenColor(Color.Red, 0.2f))
                            .align(Alignment.TopEnd)
                            .clickable { showWarning = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭浮窗"
                        )
                    }
                }
            }
        }
    }
}

fun isJson(text: String): Boolean {
    return try {
        JSONObject(text)
        true
    } catch (_: JSONException) {
        try {
            JSONArray(text)
            true
        } catch (_: JSONException) {
            false
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@Preview(showBackground = true, showSystemUi = true)
fun LogPre() {
    val mainColor = remember { mutableStateOf(Color.Blue) }
    mainColor.value = MaterialTheme.colorScheme.inversePrimary
    val logList = mutableStateListOf<LogData>()
    logList.add(LogData(now().toString(), "\uD83D\uDC22", "aaaaaaaaaaaaaaaaaaaaaaaa", "Daily"))
    logList.add(LogData(now().toString(), "\uD83D\uDC22", "aaaaaaaaaa", "Daily"))
    val typeList = mutableStateListOf("Daily", "Dream")
    TapLogTheme {
        val mainColor = MaterialTheme.colorScheme.inversePrimary
        val activity = LocalContext.current.findActivity()

        DisposableEffect(mainColor) {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(
                    darkenColor(mainColor, 0.3f).toArgb() // 背景色
                ),
                navigationBarStyle = SystemBarStyle.dark(
                    darkenColor(mainColor, 0.3f).toArgb()
                )
            )
            onDispose { }
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                loadLog(activity ?: return@withContext, logList)
            }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LogCard(innerPadding, logList, typeList, remember { mutableStateOf(mainColor) })
        }
    }
}

data class LogData(val time: String, val head: String, val content: String, val type: String)

fun LogData.toJSONObject(): JSONObject {
    val obj = JSONObject()
    obj.put("time", time)
    obj.put("head", head)
    obj.put("content", content)
    obj.put("type", type)
    return obj
}

fun getLogFormJSONObject(jsonObj: Any): LogData {
    val obj = jsonObj as JSONObject
    return LogData(
        obj.getString("time"),
        obj.getString("head"),
        obj.getString("content"), obj.getString("type")
    )
}

@Composable
private fun TimeHeaderWithTapBox(
    time: String,
    context: Context,
    loadDayData: (Context, String) -> Pair<Int, Float>?
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = time)
        Spacer(modifier = Modifier.weight(1f))
        TapScoreBox(time = time, context = context, loadDayData = loadDayData)
    }
}

@Composable
private fun TapScoreBox(
    time: String,
    context: Context,
    loadDayData: (Context, String) -> Pair<Int, Float>?
) {
    // 使用和你原来一样的样式尺寸/背景
    Box(
        modifier = Modifier
            .background(darkenColor(MaterialTheme.colorScheme.primaryContainer, 0.15f))
            .size(140.dp, 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tap:", fontSize = 13.sp, color = Color.Blue)
            Spacer(modifier = Modifier.weight(1f))
            val data = loadDayData(context, time) ?: Pair(-1, -1f)
            if (data.first >= 0) {
                Text("${data.first}得分")
                Spacer(modifier = Modifier.weight(1f))
                Text(String.format(Locale.CHINA, "%.2f", data.second) + "s")
            } else {
                Text("今天暂无成绩", fontSize = 13.sp)
            }
        }
    }
}
