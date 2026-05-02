package com.roroi.taplog.daily.subScreen

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.roroi.taplog.daily.cascadiaFont
import com.roroi.taplog.daily.soBiscuitFont
import com.roroi.taplog.daily.viewmodel.DSpace
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.score.OptimizedBackground
import kotlinx.coroutines.delay

@Preview
@Composable
fun PortalAdderPre() {
    PortalEditor(null, "")
}

@Composable
fun PortalEditor(viewModel: DailyViewModel?, fatherId: String) {
    val portalName = remember { mutableStateOf(TextFieldValue("Portal")) }
    val selectedSpaceId = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val text = portalName.value.text
        portalName.value = portalName.value.copy(selection = TextRange(text.length))
    }

    Scaffold(
        topBar = {
            PortalTB(viewModel)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            OptimizedBackground()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.6f else 0.9f))
            )
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SelectSpace(viewModel, selectedSpaceId, fatherId)
        }
    }
}

@Composable
fun SelectSpace(
    viewModel: DailyViewModel?,
    selectedDSpaceId: MutableState<String?>,
    fatherId: String
) {
    var newSpaceName by remember { mutableStateOf("new Space") }
    var currentTheme by remember { mutableStateOf(targetColorList.random()) }
    var showDelDSpaceDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    val ballColor = currentTheme.primaryColor
    val backgroundColor = currentTheme.backgroundColor
    var showChangePortal by remember { mutableStateOf(false) }

    if (showChangePortal) {
        ConfirmDialog(title = "更换入口", text = "是否确认更换入口", onConfirm = {
            selectedDSpaceId.value?.let {
                if (viewModel != null) {
                    viewModel.changeEntryFId(entryId = fatherId, spaceId = it)
                    viewModel.toastOut("修改完成✅")
                }
            }
            showChangePortal = false
        }, onDismiss = { showChangePortal = false })
    }

    Box(
        modifier = Modifier
            .height(360.dp)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .background(
                Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            LazyColumn(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                viewModel?.spaces?.forEach {
                    item {
                        SpaceCard(
                            onSelected = { selectedDSpaceId.value = it.id },
                            onChangePortal = {
                                selectedDSpaceId.value = it.id
                                showChangePortal = true
                            },
                            it.name,
                            selectedDSpaceId.value == it.id
                        )
                    }
                }
            }

            // 输入密钥
            TextField(
                value = password,
                onValueChange = { password = it },
                // 告诉输入法这是密码，触发安全模式
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Unspecified,
                    autoCorrectEnabled = false, // 显式关闭自动纠错，防止密码被记录
                    keyboardType = KeyboardType.Password, // 或 KeyboardType.NumberPassword (纯数字)
                    imeAction = ImeAction.Unspecified
                ),
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("空间密码") }
            )

            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // 选择颜色
                ThemeEditorCircle(null, modifier = Modifier.zIndex(1f)) { newTheme ->
                    currentTheme = newTheme
                }
                // 名字
                NameSpace(newSpaceName) { newName ->
                    newSpaceName = newName
                }

                // 新建空间按钮
                Button(
                    onClick = {
                        viewModel?.addSpace(
                            DSpace(
                                backgroundColor.toArgb(),
                                ballColor.toArgb(),
                                entryId = fatherId,
                                name = newSpaceName,
                                password = password,
                                isDark = currentTheme.isDark
                            )
                        )
                        viewModel?.let {
                            if (!it.hasSpace(fatherId)) {
                                it.navigatePop()
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .width(buttonWidth)
                        .fillMaxHeight()
                ) {
                    Text(text = "NEW", fontFamily = soBiscuitFont, fontSize = 24.sp)
                }

                // 删除按钮
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.onPrimary)
                        .clickable { showDelDSpaceDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "delete",
                        tint = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        selectedDSpaceId.value?.let { sDsId ->
            val currentDSpaceName = viewModel?.spaces?.find { it.id == sDsId }?.name
            if (viewModel != null && currentDSpaceName != null && showDelDSpaceDialog) {
                DelDSpaceDialog(onDismiss = {
                    showDelDSpaceDialog = false
                }, onConfirm = {
                    viewModel.delSpace(sDsId)
                    showDelDSpaceDialog = false
                }, currentDSpaceName)
            }
        }
    }
}

val buttonWidth = 108.dp

@Composable
fun ThemeEditorCircle(
    space: DSpace?,
    height: Dp = 0.dp,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onChangeColorPair: (ThemePreset) -> Unit
) {
    var boxTheme by remember { mutableStateOf(targetColorList.random()) }

    // 区分“弹窗是否挂载”和“是否处于展开动画状态”
    var showPopup by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // 【修复1】外部状态同步必须使用 LaunchedEffect，否则会导致无限重组和覆盖用户操作！
    LaunchedEffect(space) {
        space?.let {
            boxTheme = boxTheme.copy(
                backgroundColor = Color(it.colorBgArgb),
                primaryColor = Color(it.colorBallArgb)
            )
        }
    }

    // 动画状态管理
    val columnHeight by animateDpAsState(
        targetValue = if (isExpanded) 450.dp else 0.dp,
        animationSpec = tween(durationMillis = 200, easing = EaseOutSine),
        finishedListener = { if (it <= 0.dp) showPopup = false }, // 动画收起完毕后才销毁弹窗
        label = "ThemeColumnHeight"
    )

    Box(modifier = modifier) {
        // 【修复2】使用官方的 Popup 代替 hack 方式的“超大 Box”
        if (showPopup) {
            val popupXOffset = with(density) { 50.dp.roundToPx() } // 将 50dp 偏移转换为像素

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = popupXOffset, y = -225),
                onDismissRequest = {
                    // 点击弹窗外部时触发：开始收起动画，而不是直接暴毙
                    isExpanded = false
                },
                properties = PopupProperties(
                    focusable = true, // 关键：允许拦截外部点击事件
                    dismissOnClickOutside = true
                )
            ) {
                // 主题列表容器
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(columnHeight) // 使用动画高度
                        .clip(RoundedCornerShape(30.dp)) // 先裁切圆角，防止模糊背景溢出
                ) {
                    // 【修复3】优化高斯模糊的实现，确保在低版本平稳降级
                    val isAndroidS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    val textBg = if (isAndroidS) Color.White.copy(alpha = 0.8f) else Color.White

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                if (isAndroidS) {
                                    renderEffect = RenderEffect
                                        .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)
                                        .asComposeRenderEffect()
                                }
                            }
                            .background(textBg)
                    )

                    // 【修复4】添加 fillMaxWidth 让内部元素真正水平居中
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally // 配合 fillMaxSize 生效
                    ) {
                        items(targetColorList.size) { index ->
                            val theme = targetColorList[index]
                            ThemeItem(theme) {
                                boxTheme = theme
                                onChangeColorPair(theme)
                                isExpanded = false // 点击后触发收起动画
                            }
                        }
                    }
                }
            }
        }

        // 颜色选择按钮（锚点）
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .then(if (height == 0.dp) Modifier.fillMaxHeight() else Modifier.height(height))
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // 单击切换颜色
                            val currentIndex = targetColorList.indexOf(boxTheme)
                            val nextIndex = (currentIndex + 1) % targetColorList.size
                            boxTheme = targetColorList[nextIndex]
                            onChangeColorPair(boxTheme)
                        },
                        onLongPress = {
                            // 长按立即震动并展开
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isExpanded) {
                                showPopup = true
                                isExpanded = true
                            } else {
                                isExpanded = false
                            }
                        }
                    )
                }
        ) {
            ThemeCanvas(boxTheme.backgroundColor, boxTheme.primaryColor)
        }
    }
}

// 抽取出来的绘制组件
@Composable
fun ThemeCanvas(bgColor: Color, primaryColor: Color) {
    Canvas(modifier = Modifier.size(60.dp)) { // [修改] 删除了 zIndex(120f)
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            },
            color = bgColor
        )
        drawPath(
            path = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            },
            color = primaryColor
        )
    }
}

// 列表中的单个主题项
@Composable
fun ThemeItem(theme: ThemePreset, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp) // [修改] 删除了 zIndex(0f)
            .size(45.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        ThemeCanvas(theme.backgroundColor, theme.primaryColor)
    }
}

@Composable
fun NameSpace(name: String, height: Dp = 0.dp, onName: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var newSpaceName by remember {
        mutableStateOf(
            TextFieldValue(
                text = name,
                selection = TextRange(name.length)
            )
        )
    }
    BasicTextField(
        value = newSpaceName,
        onValueChange = { newValue ->
            newSpaceName = newSpaceName.copy(newValue.text, TextRange(newValue.text.length))
            onName(newValue.text)
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .width(0.dp)
    )
    // 空间名称输入框（点击触发）
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .width(buttonWidth)
            .then(if (height == 0.dp) Modifier.fillMaxHeight() else Modifier.height(height))
            .background(
                color = Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (newSpaceName.text == "new Space") {
                        newSpaceName = newSpaceName.copy(text = "")
                        onName("")
                    }
                    focusManager.clearFocus(force = true)
                    focusRequester.requestFocus()
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = newSpaceName.text, textAlign = TextAlign.Center, fontSize = 16.sp)
    }
}

data class ThemePreset(
    val backgroundColor: Color,
    val primaryColor: Color,
    val isDark: Boolean
)

@Composable
fun SpaceCard(onSelected: () -> Unit, onChangePortal: () -> Unit, name: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(
                    alpha = 0.5f
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelected,
                onLongClick = onChangePortal
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(name, modifier = Modifier.padding(start = 16.dp), fontSize = 24.sp)
    }
}

val targetColorList = listOf(
    // 浅色模式 (isDark = false)
    ThemePreset(Color(0xFFF8F8F8), Color(0xFFE57373), false),
    ThemePreset(Color(0xFFF9F7F3), Color(0xFFFFA726), false),
    ThemePreset(Color(0xFFF9F9F5), Color(0xFFFFD54F), false),
    ThemePreset(Color(0xFFF6FAF7), Color(0xFF66BB6A), false),
    ThemePreset(Color(0xFFF5FAFA), Color(0xFF26C6DA), false),
    ThemePreset(Color(0xFFF5F7FB), Color(0xFF42A5F5), false),
    ThemePreset(Color(0xFF42A5F5), Color(0xFFF5F7FB), false), // 注意：这一项背景是蓝色，主色是浅色
    ThemePreset(Color(0xFFF8F6FA), Color(0xFFAB47BC), false),
    ThemePreset(Color(0xFFFBF6F8), Color(0xFFEC407A), false),

    // 深色模式 (isDark = true)
    ThemePreset(Color(0xFF121212), Color(0xFFFF6F61), true),
    ThemePreset(Color(0xFF101010), Color(0xFFFF8A50), true),
    ThemePreset(Color(0xFF121212), Color(0xFFFFD54F), true),
    ThemePreset(Color(0xFF0F1412), Color(0xFF66BB6A), true),
    ThemePreset(Color(0xFF0E1416), Color(0xFF26C6DA), true),
    ThemePreset(Color(0xFF0E1018), Color(0xFF64B5F6), true),
    ThemePreset(Color(0xFF120F16), Color(0xFFBA68C8), true),
    ThemePreset(Color(0xFF141012), Color(0xFFF06292), true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalTB(viewModel: DailyViewModel?) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    CenterAlignedTopAppBar(
        title = {
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        focusRequester.requestFocus()
                    }
                )) {
                Text(
                    "Portal",
                    fontFamily = cascadiaFont,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { viewModel?.navigatePop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Cyan.copy(alpha = 0.3f))
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun DelDSpaceDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, spaceName: String) {
    var step by remember { mutableIntStateOf(0) }
    var confirmText by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(2) }

    LaunchedEffect(step) {
        if (step == 3) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    val stepsContent = listOf(
        "确定要删除此空间吗？" to "此操作不可撤销。",
        "警告：数据将永久丢失" to "操作后，我们将无法恢复您的笔记。",
        "最后确认" to "请确保您不再需要这些内容。",
        "Wait..." to "请等待 ${countdown}s 后继续...",
        "请输入名称确认" to ""
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stepsContent[step].first,
                color = if (step == 4) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                if (step == 4) {
                    TextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("输入空间名称以确认...") }
                    )
                }
                Text(text = stepsContent[step].second)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (step < 4) step++ else onConfirm() },
                enabled = step < 3 || (step == 3 && countdown == 0) || (step == 4 && confirmText == spaceName),
                colors = if (step == 4) ButtonDefaults.buttonColors(containerColor = Color.Red) else ButtonDefaults.buttonColors()
            ) {
                Text(if (step < 4) "明白，下一步" else "立即删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("点错了，停下") }
        }
    )
}