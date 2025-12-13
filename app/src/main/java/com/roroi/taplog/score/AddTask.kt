package com.roroi.taplog.score

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roroi.taplog.ui.theme.TapLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.max

const val SCALE_V = 1.7f
private const val DOWNSAMPLE_FACTOR = 5f * SCALE_V
private const val SCALE_DOWN = 1f / DOWNSAMPLE_FACTOR

// ==========================================
// 1. 数据状态类 (ViewModel 的轻量替代)
// ==========================================
// 将表单状态封装起来，方便传递
// 找到 AddTaskState 类，完全替换为：

class AddTaskState(initialTask: Task?) { // [修改] 这里接收参数
    // 如果是编辑模式(initialTask不为空)，沿用旧ID；否则生成新ID
    var id = initialTask?.id ?: java.util.UUID.randomUUID().toString()

    var title by mutableStateOf(initialTask?.title ?: "")
    var description by mutableStateOf(initialTask?.description ?: "")

    // 如果有旧难度，就用旧的，否则默认 EASY
    var difficulty by mutableStateOf(initialTask?.difficulty ?: Difficulty.EASY)
    var income by mutableIntStateOf(initialTask?.income ?: 10)

    fun toTask(): Task {
        return Task(
            id = id, // 保持ID不变
            title = title.ifBlank { "Title" },
            description = description.ifBlank { "Description." },
            difficulty = difficulty,
            income = income,
        )
    }
}

// ==========================================
// 2. 主界面组件
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskApp(onBack: () -> Unit, initialTaskJson: String?) {
    val existingTask = remember(initialTaskJson) {
        if (initialTaskJson != null) {
            try {
                Json.decodeFromString<Task>(initialTaskJson)
            } catch (e: Exception) { null }
        } else null
    }
    val state = remember { AddTaskState(existingTask) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAppVisible by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppVisible = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = {
        AddTaskTopBar(onBack = onBack, onSave = {
            // 保存逻辑
            if (existingTask != null) {
                GlobalV.updateTask(state.toTask())
            } else {
                GlobalV.addTask(state.toTask())
            }
            onBack() // 保存后返回
        })
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景层
            if (isAppVisible) {
                OptimizedBackground()
            }

            // 遮罩层
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.6f else 0.9f))
            )

            // 内容层
            AddTaskContent(
                state = state,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ==========================================
// 3. UI 子组件拆分
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskTopBar(onBack: () -> Unit, onSave: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Add Task", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(0.85f)),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Exit")
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(Icons.Rounded.Check, contentDescription = "Save")
            }
        }
    )
}

@Composable
fun OptimizedBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize(SCALE_DOWN)
            .graphicsLayer {
                scaleX = DOWNSAMPLE_FACTOR
                scaleY = DOWNSAMPLE_FACTOR
                transformOrigin = TransformOrigin(0f, 0f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val optimizedRadius = max(15f, 100f / DOWNSAMPLE_FACTOR)
                    renderEffect = RenderEffect
                        .createBlurEffect(optimizedRadius, optimizedRadius, Shader.TileMode.MIRROR)
                        .asComposeRenderEffect()
                }
            }
    ) {
        AddTaskBackground(Modifier)
    }
}

@Composable
private fun AddTaskContent(state: AddTaskState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 预览卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.65f))
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(), contentAlignment = Alignment.Center
                ) {
                    // 复用之前的 TaskCard，但传入一个空的点击事件，仅作展示
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        TaskCard(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 16.dp, bottom = 16.dp),
                            task = state.toTask(),
                            onTaskClick = {},
                            onLongClick = {}
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 文本输入
        item {
            TransparentTextField(
                value = state.title,
                onValueChange = { state.title = it },
                label = "Enter title",
                placeholder = "Title"
            )
        }
        item {
            TransparentTextField(
                value = state.description,
                onValueChange = { state.description = it },
                label = "Enter description",
                placeholder = "Description."
            )
        }

        // 难度选择
        item { DifficultySelector(state) }

        // 收益设置
        item {
            SettingRow {
                Text(text = "Income", modifier = Modifier.padding(end = 8.dp))
                NumberStepper(
                    value = state.income,
                    onValueChange = { state.income = it },
                    range = 0..10000
                )
            }
        }
    }
}

@Composable
fun TransparentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(0.65f),
            unfocusedContainerColor = Color.White.copy(0.65f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun DifficultySelector(state: AddTaskState) {
    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { diff ->
            val isSelected = state.difficulty == diff
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        1.dp,
                        if (isSelected) diff.getColor() else Color.Gray,
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color.White.copy(0.65f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { state.difficulty = diff },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(diff.getColor().copy(alpha = if (isSelected) 1f else 0.3f))
                )
            }
        }
    }
}

@Composable
private fun SettingRow(distance: Float = 1f, content: @Composable RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.weight(max(0.1f, distance)))
        Row(
            modifier = Modifier.weight(3.5f),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        Spacer(modifier = Modifier.weight(max(0.1f, distance)))
    }
}


// ==========================================
// 4. 通用组件 (NumberStepper & RepeatingButton)
// ==========================================

@Composable
fun NumberStepper(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = 0..1000,
    unit: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .background(Color(0xFFF5F6F8), RoundedCornerShape(12.dp))
            .padding(4.dp)
            .height(40.dp)
    ) {
        RepeatingIconButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            tint = if (value > range.first) Color.Gray else Color.LightGray
        )

        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color(0xFF333333),
            maxLines = 1
        )

        RepeatingIconButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            tint = if (value < range.last) MaterialTheme.colorScheme.primary else Color.LightGray
        )
    }
}

@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)

    val customRipple = remember { CircularRippleNodeFactory(false, 24.dp, Color.Unspecified) }

    Box(
        modifier = modifier
            .size(40.dp)
            .indication(interactionSource, customRipple)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        val logicJob = scope.launch {
                            currentOnClick()
                            delay(400L)
                            var currentDelay = 150L
                            while (true) {
                                currentOnClick()
                                delay(currentDelay)
                                currentDelay = (currentDelay * 0.8).toLong().coerceAtLeast(50L)
                            }
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            logicJob.cancel()
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

private class CircularRippleNodeFactory(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: Color
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return createRippleModifierNode(
            interactionSource = interactionSource,
            bounded = bounded,
            radius = radius,
            color = { color },
            rippleAlpha = { RippleAlpha(0.16f, 0.12f, 0.08f, 0.12f) }
        )
    }

    override fun equals(other: Any?) =
        other is CircularRippleNodeFactory && bounded == other.bounded && radius == other.radius && color == other.color

    override fun hashCode() = (bounded.hashCode() * 31 + radius.hashCode()) * 31 + color.hashCode()
}

@Preview(showBackground = true)
@Composable
fun AddTaskPreview() {
    TapLogTheme { AddTaskApp(onBack = {}, "") }
}