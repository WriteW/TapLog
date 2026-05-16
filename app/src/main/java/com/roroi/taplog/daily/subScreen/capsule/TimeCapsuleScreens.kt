package com.roroi.taplog.daily.subScreen.capsule

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.roroi.taplog.daily.DailyDynamicBackground
import com.roroi.taplog.daily.DailyTimeTheme
import com.roroi.taplog.daily.DateFormat
import com.roroi.taplog.daily.FullDateFormat
import com.roroi.taplog.daily.GlassmorphismBackground
import com.roroi.taplog.daily.TimeFormat
import com.roroi.taplog.daily.YearFormat
import com.roroi.taplog.daily.cascadiaFont
import com.roroi.taplog.daily.soBiscuitFont
import com.roroi.taplog.daily.subScreen.ThemeEditorCircle
import com.roroi.taplog.daily.subScreen.targetColorList
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Date

// ==================== 自适应动态文字颜色 ====================
// 如果背景太亮，字用黑色；如果背景太暗，字用白色。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTCScreen(viewModel: DailyViewModel?) {
    val theme = viewModel?.getThemeBySpace() ?: DailyTimeTheme.getCurrent()
    var name by remember { mutableStateOf("") }
    var capsuleColorP by remember { mutableStateOf(targetColorList.random()) }

    // 默认往后推迟 1 天
    var targetDate by remember { mutableLongStateOf(System.currentTimeMillis() + 86400000L) }

    // 日期/时间选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
    val timePickerState = rememberTimePickerState()

    // --- 日期选择器 ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true // 选完日期自动跳到时间选择
                }) { Text("Next") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- 时间选择器 ---
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Exact Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    val cal = Calendar.getInstance()
                    datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    targetDate = cal.timeInMillis
                }) { Text("Confirm") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Seal a Memory",
                        fontFamily = soBiscuitFont,
                        color = theme.primaryColor
                    )
                },
                navigationIcon = {
                    IconButton({ viewModel?.navigatePop() }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            "Exit"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DailyDynamicBackground(theme)
            GlassmorphismBackground(modifier = Modifier.fillMaxSize(), alpha = 0.4f)

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 实时预览胶囊 UI
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .height(125.dp)
                        .width(316.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (theme.isDark) Color.DarkGray.copy(0.6f) else Color.White.copy(
                                0.6f
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimeCapsule(
                        leftTop = YearFormat.format(Date(targetDate)) + " " + DateFormat.format(Date()),
                        leftBottom = TimeFormat.format(Date()),
                        rightTop = TimeFormat.format(Date(targetDate)),
                        rightBottom = YearFormat.format(Date(targetDate)) + " " + DateFormat.format(Date(targetDate)),
                        baseColor = capsuleColorP.primaryColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Capsule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Theme Color",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = capsuleColorP.primaryColor
                    )
                    ThemeEditorCircle(defaultThemePreset = capsuleColorP, space = null, height = 40.dp) { preset ->
                        if (preset.primaryColor == Color(0xFFF5F7FB)) {
                            capsuleColorP = targetColorList[targetColorList.indexOf(preset) + 1]
                        } else {
                        capsuleColorP = preset
                            }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(0.4f),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "${FullDateFormat.format(Date(targetDate))} ${
                            TimeFormat.format(
                                Date(
                                    targetDate
                                )
                            )
                        }"
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            viewModel?.toastOut("Please enter a name")
                            return@Button
                        }
                        viewModel?.startCapsuleSelection(name, capsuleColorP.primaryColor.toArgb(), targetDate)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = capsuleColorP.primaryColor)
                ) {
                    Text(
                        "Select Entries to Seal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== 2. 查看胶囊列表页 ====================
@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun ViewTCScreen(viewModel: DailyViewModel?) {
    val theme = viewModel?.getThemeBySpace() ?: DailyTimeTheme.getCurrent()
    // 按 openAt 时间升序（最旧的，最快解锁的在最上面）
    val capsules = viewModel?.timeCapsules?.sortedBy { it.openAt } ?: emptyList()

    var selectedLockedCapsule by remember { mutableStateOf<Long?>(null) }
    var capsuleToDelete by remember {
        mutableStateOf<com.roroi.taplog.daily.viewmodel.TimeCapsule?>(
            null
        )
    }

    // 倒计时弹窗
    if (selectedLockedCapsule != null) {
        CountdownDialog(targetTime = selectedLockedCapsule!!) { selectedLockedCapsule = null }
    }

    // 删除校验弹窗
    if (capsuleToDelete != null) {
        var inputName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { capsuleToDelete = null },
            title = { Text("Destroy Capsule", color = Color.Red) },
            text = {
                Column {
                    Text("Are you sure? This will permanently delete the capsule and ALL entries inside it.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputName, onValueChange = { inputName = it },
                        label = { Text("Type '${capsuleToDelete!!.name}' to confirm") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputName == capsuleToDelete!!.name) {
                            viewModel?.deleteCapsule(capsuleToDelete!!.id)
                            capsuleToDelete = null
                        } else {
                            viewModel?.toastOut("Name doesn't match")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Destroy") }
            },
            dismissButton = { TextButton(onClick = { capsuleToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Capsules",
                        fontFamily = soBiscuitFont,
                        color = theme.primaryColor
                    )
                },
                navigationIcon = {
                    IconButton({ viewModel?.navigatePop() }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            DailyDynamicBackground(theme)
            GlassmorphismBackground(modifier = Modifier.fillMaxSize(), alpha = 0.4f)

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(capsules) { capsule ->
                    val isUnlocked = System.currentTimeMillis() >= capsule.openAt
                    val progress by animateFloatAsState(
                        targetValue = if (isUnlocked) 1f else ((System.currentTimeMillis() - capsule.createdAt).toFloat() / (capsule.openAt - capsule.createdAt).coerceAtLeast(
                            1
                        ).toFloat()).coerceIn(0f, 1f)
                    )

                    Column(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (theme.isDark) Color.DarkGray.copy(0.6f) else Color.White.copy(
                                    0.6f
                                )
                            )
                            .combinedClickable(
                                onLongClick = { capsuleToDelete = capsule },
                                onClick = {
                                    if (isUnlocked) {
                                        // 跳转并关闭侧边栏（虽然从这个页跳不需要真的关侧边栏，为了适配传个空闭包）
                                        viewModel?.openCapsuleSpace(capsule.id)
                                    } else selectedLockedCapsule = capsule.openAt
                                }
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                capsule.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(capsule.colorArgb)
                            )
                            Spacer(Modifier.weight(1f))
                            if (!capsule.isViewed && isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        TimeCapsule(
                            leftTop = YearFormat.format(Date(capsule.openAt)) + " " + DateFormat.format(
                                Date(capsule.createdAt)
                            ),
                            leftBottom = TimeFormat.format(Date(capsule.createdAt)),
                            rightTop = TimeFormat.format(Date(capsule.openAt)),
                            rightBottom = YearFormat.format(Date(capsule.openAt)) + " " + DateFormat.format(
                                Date(capsule.openAt)
                            ),
                            baseColor = Color(capsule.colorArgb),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )

                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = Color(capsule.colorArgb),
                            trackColor = Color.LightGray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

// ==================== 3. 唯美动态倒计时弹窗 ====================
@Composable
fun CountdownDialog(targetTime: Long, onDismiss: () -> Unit) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 心跳呼吸动画
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        while (currentTime < targetTime) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val diff = (targetTime - currentTime).coerceAtLeast(0)
    val days = diff / (1000 * 60 * 60 * 24)
    val hours = (diff / (1000 * 60 * 60)) % 24
    val mins = (diff / (1000 * 60)) % 60
    val secs = (diff / 1000) % 60

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(0.85f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LockClock,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Time Locked",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "${days}d ${hours}h ${mins}m ${secs}s",
                    fontSize = 24.sp,
                    fontFamily = cascadiaFont,
                    color = Color.Black
                )
            }
        }
    }
}

// ==================== 4. 优化后的自适应双色胶囊 UI ====================
@Composable
fun TimeCapsule(
    leftTop: String, leftBottom: String, rightTop: String, rightBottom: String,
    baseColor: Color, modifier: Modifier = Modifier
) {
    // 动态适应深色/浅色模式的左侧背景色
    val leftBgColor = Color.White.copy(alpha = 0.85f)
    val leftTextColor = baseColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(leftBgColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧（空间主体色，文字为胶囊色）
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    leftTop,
                    fontSize = 20.sp,
                    color = leftTextColor,
                    fontFamily = cascadiaFont,
                    fontWeight = FontWeight.Bold
                )
                Text(leftBottom, fontSize = 11.sp, color = leftTextColor, fontFamily = cascadiaFont)
            }
        }
        // 右侧（背景为胶囊色，文字自适应黑/白）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(baseColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(rightTop, fontSize = 11.sp, color = leftBgColor)
                Text(
                    rightBottom,
                    fontSize = 20.sp,
                    color = leftBgColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}