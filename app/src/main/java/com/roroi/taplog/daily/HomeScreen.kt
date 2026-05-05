package com.roroi.taplog.daily

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.roroi.taplog.daily.subScreen.SpaceCard
import com.roroi.taplog.daily.subUi.ChangePasswordDialog
import com.roroi.taplog.daily.subUi.ImageViewerDialog
import com.roroi.taplog.daily.subUi.LeftSidebarContent
import com.roroi.taplog.daily.subUi.LoadingDialog
import com.roroi.taplog.daily.subUi.PasswordCheckDialog
import com.roroi.taplog.daily.subUi.RightSidebarContent
import com.roroi.taplog.daily.viewmodel.CropParams
import com.roroi.taplog.daily.viewmodel.DailyEntry
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.daily.viewmodel.EntryType
import com.roroi.taplog.daily.viewmodel.TimelineGroup
import com.roroi.taplog.daily.viewmodel.calculateTransform
import com.roroi.taplog.daily.viewmodel.getDotColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// --- 布局常量配置 ---
// 左侧：时间文字区域的宽度 (控制时间离左屏幕的距离 + 文字活动空间)
private val TEXT_AREA_WIDTH = 70.dp

// 右侧：时间轴线条到日记卡片的距离 (控制卡片离线的距离)
private val GAP_WIDTH = 24.dp

// 计算出侧边栏的总宽度 (用于 Box 和 LazyColumn)
private val TOTAL_SIDEBAR_WIDTH = TEXT_AREA_WIDTH + GAP_WIDTH
private val DOT_SIZE = 12.dp
private val CARD_TOP_OFFSET = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DailyViewModel
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val rightSidebarListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val groups by viewModel.groupedEntries.collectAsState() // 比较核心
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current
    val currentTheme = viewModel.getThemeBySpace()

    // 短期状态===
    // 侧边栏状态
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val hazeState = remember { HazeState() }
    val isDark = viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.isDark ?: false // 当前空间是否为暗色

    // 导入：打开文件
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    // 导出：创建文件 (让用户选择保存位置)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    // 实时时间
    val currentTimeMillis = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis.longValue = System.currentTimeMillis()
            delay(1000)
        }
    }
    val currentTime = remember(currentTimeMillis.longValue) { Date(currentTimeMillis.longValue) }

    val isAddFABExpand = remember { mutableStateOf(false) }

    // Toast 处理
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Log.d("cat is cute", "uiM: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }

    }

    // 图片全屏查看器
    if (viewModel.viewingImageEntry != null) {
        ImageViewerDialog(
            entry = viewModel.viewingImageEntry!!,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissShowImage() },
            onDelete = {
                viewModel.deleteEntry(viewModel.viewingImageEntry!!.id)
                viewModel.dismissShowImage()
            }
        )
    }

    // 显示密码输入
    if (viewModel.showPasswordCheck) {
        PasswordCheckDialog(
            onDismiss = { viewModel.showPasswordCheck = false },
            onConfirm = { inputPassword ->
                viewModel.spaces.find { it.id == viewModel.spaceDestination }?.let { space ->
                    if (space.password == inputPassword) {
                        viewModel.changeSpace()
                        viewModel.showPasswordCheck = false
                    } else {
                        viewModel.toastOut("密码错误❌")
                    }
                }
            }
        )
    }

    // 显示加载中
    if (viewModel.showLoadingDialog) {
        LoadingDialog()
    }

    // 修改密码
    val currentSpace = viewModel.getSpaceFromId(viewModel.selectedDSpaceId)
    if (viewModel.showChangePassword && currentSpace != null) {
        ChangePasswordDialog(
            onDismiss = { viewModel.showChangePassword = false },
            onConfirm = { oldPass, newPass ->
                currentSpace.let {
                    viewModel.changeSpaceP(it.copy(password = newPass))
                }
                viewModel.showChangePassword = false
            },
            !currentSpace.password.isBlank()
        )
    }

    // 选择目标空间移动
    if (viewModel.showSelectSpaceM) {
        MoveEntryTo(
            onDismiss = { viewModel.showSelectSpaceM = false },
            onConfirm = { targetSpaceId ->
                viewModel.showSelectSpaceM = false
                val realTarget = if (targetSpaceId == "main") null else targetSpaceId
                viewModel.moveEntry(
                    originalSpaceId = viewModel.selectedDSpaceId,
                    targetSpaceId = realTarget
                )
            },
            viewModel
        )
    }

    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        scrimColor = Color.Transparent,
        drawerContent = {
            LeftSidebarContent(
                // 【UI应用】：传入主题颜色给侧边栏
                theme = currentTheme,
                hazeState = hazeState,
                onExport = {
                    // 生成默认文件名：daily_backup_时间戳.zip
                    val fileName = "daily_backup_${System.currentTimeMillis()}.zip"
                    exportLauncher.launch(fileName)
                    scope.launch { leftDrawerState.close() }
                },
                onImport = {
                    importLauncher.launch(arrayOf("application/zip"))
                    scope.launch { leftDrawerState.close() }
                },
                onClear = {
                    viewModel.clearAllData()
                    scope.launch { leftDrawerState.close() }
                },
                viewModel
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                scrimColor = Color.Transparent,
                drawerContent = {
                    // 调用刚才写的右侧内容
                    RightSidebarContent(
                        listState = rightSidebarListState,
                        scope = scope,
                        viewModel = viewModel,
                        theme = currentTheme,
                        hazeState = hazeState,
                        onJumpToGroup = { index ->
                            scope.launch {
                                scope.launch {
                                    rightDrawerState.close() // 关闭侧边栏
                                }
                                listState.scrollToItem(index) // 跳转到对应位置
                            }
                        }
                    )
                }
            ) {
                // --- 第三层：主界面内容 ---
                // 必须改回 LTR (从左向右)，否则主界面的文字和布局全是反的！
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    @Suppress("DEPRECATION")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(
                                state = hazeState,
                                // 告诉 Haze 背景的基础色
                                backgroundColor = currentTheme.backgroundColor
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.unFocusEntry()
                                isAddFABExpand.value = false
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        val touchX = change.position.x
                                        val screenWidth = size.width

                                        // 打开左侧框
                                        if (touchX < screenWidth / 2 && dragAmount > 20) {
                                            if (leftDrawerState.isClosed && rightDrawerState.isClosed) {
                                                scope.launch {
                                                    leftDrawerState.open()
                                                    rightDrawerState.close()
                                                }
                                            }
                                        }
                                        // 打开右侧框
                                        else if (touchX > screenWidth / 2 && dragAmount < -20) {
                                            if (leftDrawerState.isClosed && rightDrawerState.isClosed) {
                                                scope.launch {
                                                    rightSidebarListState.scrollToItem(0)
                                                    leftDrawerState.close()
                                                    rightDrawerState.open()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        DailyDynamicBackground(theme = currentTheme)
                        // 处理返回键退出空间
                        BackHandler(
                            enabled = true
                        ) {
                            if (leftDrawerState.isOpen) {
                                scope.launch {
                                    leftDrawerState.close()
                                }
                            } else if (rightDrawerState.isOpen) {
                                scope.launch {
                                    rightDrawerState.close()
                                }
                            } else if (isAddFABExpand.value) {
                                isAddFABExpand.value = false
                            } else if (viewModel.selectedDSpaceId?.isNotBlank() ?: false) {
                                viewModel.exitToMainSpace()
                            } else if (!viewModel.showPasswordCheck) {
                                (context as? Activity)?.finish()
                            }
                        }

                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                val spaceColor =
                                    viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.colorBgArgb
                                val finalColor = spaceColor?.let {
                                    Color(it).copy(alpha = 0.5f)
                                } ?: Color.White.copy(alpha = 0.5f)
                                CenterAlignedTopAppBar(
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = finalColor
                                    ),
                                    // 打开左侧栏
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { leftDrawerState.open() } }) {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Menu",
                                                tint = currentTheme.onSurfaceColor
                                            )
                                        }
                                    },
                                    actions = {
                                        if (viewModel.isBatchManaging) {
                                            IconButton({
                                                viewModel.stopBatchSelecting()
                                            }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "stop batch-managing",
                                                    tint = currentTheme.onSurfaceColor
                                                )
                                            }
                                        }
                                    },
                                    // 显示顶部时间
                                    title = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .padding(vertical = 2.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        scope.launch {
                                                            // 动画滚动到第0项 (最顶部)
                                                            listState.scrollToItem(0)
                                                        }
                                                    }
                                                )
                                        ) {
                                            Text(
                                                text = SimpleDateFormat(
                                                    "yyyy年MM月dd日 EEEE",
                                                    Locale.getDefault()
                                                ).format(currentTime),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = currentTheme.onSurfaceColor.copy(
                                                    alpha = 0.7f
                                                ),
                                                modifier = Modifier.padding(top = 2.dp),
                                                fontFamily = dymonFont
                                            )
                                            Text(
                                                text = SimpleDateFormat(
                                                    "HH:mm",
                                                    Locale.getDefault()
                                                ).format(
                                                    currentTime
                                                ),
                                                style = MaterialTheme.typography.displaySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = currentTheme.primaryColor, // 时间大字颜色
                                                fontFamily = dymonFont
                                            )
                                        }
                                    }
                                )
                            },
                            floatingActionButton = {
                                AddFAB(currentTheme, viewModel, isAddFABExpand)
                            }
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .padding(
                                        start = padding.calculateStartPadding(LayoutDirection.Ltr),
                                        top = padding.calculateTopPadding(),
                                        end = padding.calculateEndPadding(LayoutDirection.Ltr)
                                        // 不设置 bottom
                                    )
                                    .fillMaxSize()
                            ) {
                                // 背景灰线
                                val lineColor =
                                    viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.colorBgArgb?.let {
                                        Color(it)
                                    } ?: TimelineGray
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(TOTAL_SIDEBAR_WIDTH) // 使用总宽度
                                        .align(Alignment.TopStart)
                                ) {
                                    val lineX = TEXT_AREA_WIDTH.toPx()

                                    drawLine(
                                        color = lineColor,
                                        start = Offset(lineX, 0f),
                                        end = Offset(lineX, size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .simpleVerticalScrollbar(
                                            listState,
                                            color = currentTheme.primaryColor.copy(alpha = 0.5f)
                                        ),
                                    contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    items(groups) { group ->
                                        TimelineRow(
                                            group = group,
                                            theme = currentTheme,
                                            viewModel = viewModel
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}

// 渲染时间轴单行，包括：时间/日期/年份,显示圆点颜色和位置,日志卡片列表（文本/图片）
@Composable
fun TimelineRow(
    group: TimelineGroup,
    theme: DailyTimeTheme,
    viewModel: DailyViewModel
) {
    // --- 1. 准备时间格式 ---
    val dateObj = Date(group.timestamp)
    val timeStr = remember(group.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateObj)
    }
    val dateStr = remember(group.timestamp) {
        // 你可以改成 "MM月dd日" 或者 "MM/dd"
        SimpleDateFormat("MM/dd", Locale.getDefault()).format(dateObj)
    }
    val yearStr = remember(group.timestamp) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(dateObj)
    }

    // 动态计算的颜色
    val dynamicDotColor = viewModel.getTimelineColor(group.timestamp)
    val textColor = theme.primaryColor // 保证文字清晰可读

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(TOTAL_SIDEBAR_WIDTH), // 使用总宽度
            contentAlignment = Alignment.TopStart // 以此为基准
        ) {
            // --- 2. 修改部分：使用 Column 垂直排列时间、日期、年份 ---
            Column(
                horizontalAlignment = Alignment.End, // 让文字靠右对齐（紧贴时间轴线条）
                modifier = Modifier
                    .width(TEXT_AREA_WIDTH) // 限制宽度
                    // top padding 保持原样或微调，确保第一行(时间)与圆点对齐
                    // 这里稍微减了一点(-5dp)是因为 labelLarge 字体本身有行高，为了视觉中心对齐
                    .padding(top = CARD_TOP_OFFSET - 5.dp)
                    .padding(end = 16.dp, start = 4.dp) // start稍微给小点，防止文字太长换行
            ) {
                // 第一行：时间 (位置基本不变，最显眼)
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp // 稍微加大一点
                    ),
                    color = textColor,
                    textAlign = TextAlign.End,
                    fontFamily = soBiscuitFont
                )

                // 第二行：日期 (稍小，透明度降低)
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.End
                )

                // 第三行：年份 (最小，最淡)
                Text(
                    text = yearStr,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = textColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.End
                )
            }

            // 3. 时间轴圆点 (位置保持不变)
            Canvas(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .offset(x = TEXT_AREA_WIDTH - (DOT_SIZE / 2), y = CARD_TOP_OFFSET)
            ) {
                drawCircle(
                    color = group.getDotColor(),
                    radius = size.minDimension / 2 + 2.dp.toPx()
                )
                drawCircle(color = dynamicDotColor, radius = size.minDimension / 2)
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            // 在 TimelineRow 函数内部修改 FlowLayout 部分
            FlowLayout(spacing = 10.dp) {
                val items = group.items
                var i = 0
                while (i < items.size) {
                    val current = items[i]
                    val next = items.getOrNull(i + 1)

                    // 判断是否为可以横排的小图片：
                    // 1. 类型是 IMAGE
                    // 2. 不是大图 (!isLarge)
                    // 3. 比例合适 (例如 imageRatio < 1.5f)
                    val canRow =
                        current.type == EntryType.IMAGE && !current.isLarge && current.imageRatio < 1.5f
                    val nextCanRow =
                        next?.type == EntryType.IMAGE && !next.isLarge && next.imageRatio < 1.5f

                    if (canRow && nextCanRow) {
                        // 如果连续两个都是小图，用 Row 包裹
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .animateContentSize( // 当子项换行导致 FlowRow 高度改变时，平滑过渡
                                    animationSpec = tween(basicAnimLong)
                                )
                        ) {
                            EntryWithButtons(current, viewModel)
                            EntryWithButtons(next, viewModel)
                        }
                        i += 2 // 跳过两个
                    } else {
                        // 否则按原样单排
                        EntryWithButtons(current, viewModel)
                        i += 1
                    }
                }
            }
        }
    }
}

@Composable
fun EntryWithButtons(entry: DailyEntry, viewModel: DailyViewModel) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row {
            val isSelected = entry.id == viewModel.selectedEntryId
            val animatedButtonSize by animateDpAsState(
                targetValue = if (isSelected) 45.dp else 0.dp,
                animationSpec = tween(200, easing = EaseOutSine)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(animatedButtonSize)
            ) {
                if (entry.type == EntryType.IMAGE && !viewModel.isBatchManaging) {
                    Box(
                        modifier = Modifier
                            .width(animatedButtonSize)
                            .aspectRatio(1f)
                    ) {
                        PortalButton(entry, viewModel)
                    }
                    if (isSelected) Spacer(modifier = Modifier.height(2.dp))
                }
                if (!viewModel.isBatchManaging) {
                    Box(
                        modifier = Modifier
                            .width(animatedButtonSize)
                            .aspectRatio(1f)
                    ) {
                        PinButton(entry, viewModel)
                    }
                }
                if (isSelected) Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(animatedButtonSize)
                        .aspectRatio(1f)
                ) {
                    MoveButton(entry, viewModel)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box {
                    DiaryCard(entry, viewModel)

                    // 【修复区域】：加上 if (isSelected) 判断
                    if (isSelected || viewModel.batchEntries.contains(entry.id)) {
                        Box(
                            modifier = Modifier
                                // 改用 padding 将打勾图标放在卡片内部，防止被外层 FlowRow 的 animateContentSize 裁剪
                                .padding(top = 6.dp, end = 6.dp)
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Blue, CircleShape) // 用 CircleShape 更贴合 Icon
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "selected",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp) // 给 Icon 加一点 padding 以免顶格，视觉更舒适
                            )
                        }
                    }
                }
            }
        }
    }
}

const val DownDp = 4
const val basicAnimLong = 200

@Composable
fun EntryActionButton(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    icon: ImageVector,
    contentDescription: String,
    // 允许传入不同的偏移修正，保持原有的错位弹出感
    offsetYCorrection: Int = 0,
    animDuration: Int = basicAnimLong,
    onClick: () -> Unit,
    isSelected: Boolean = entry.id == viewModel.selectedEntryId
) {
    // 统一管理动画
    val animSpec = tween<Float>(animDuration, easing = EaseOutSine)
    val dpAnimSpec = tween<Dp>(animDuration, easing = EaseOutSine)

    val offsetX by animateDpAsState(if (isSelected) 0.dp else 60.dp, dpAnimSpec)
    val offsetY by animateDpAsState(
        if (isSelected) 0.dp else (offsetYCorrection + DownDp).dp,
        dpAnimSpec
    )
    val alpha by animateFloatAsState(if (isSelected) 1f else 0f, animSpec)
    val scale by animateFloatAsState(if (isSelected) 1f else 0.5f, animSpec)

    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
            }
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.White.copy(0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun MoveButton(entry: DailyEntry, viewModel: DailyViewModel) {
    EntryActionButton(
        entry = entry,
        viewModel = viewModel,
        icon = Icons.Filled.SubdirectoryArrowRight,
        contentDescription = "move entry",
        offsetYCorrection = -44, // 对应原有的 -44 + DownDp
        animDuration = basicAnimLong + 100,
        onClick = {
            viewModel.showSelectSpaceM = true
        }
    )
}

@Composable
fun PortalButton(entry: DailyEntry, viewModel: DailyViewModel) {
    EntryActionButton(
        entry = entry,
        viewModel = viewModel,
        icon = Icons.Filled.SensorDoor,
        contentDescription = "portal",
        offsetYCorrection = 44, // 对应原有的 44 + DownDp[cite: 5]
        animDuration = basicAnimLong - 100,
        onClick = { viewModel.navigateToPortal(entry.id) }
    )
}

@Composable
fun PinButton(entry: DailyEntry, viewModel: DailyViewModel) {
    EntryActionButton(
        entry = entry,
        viewModel = viewModel,
        icon = if (entry.isPin) Icons.Outlined.PushPin else Icons.Filled.PushPin,
        contentDescription = "pin",
        offsetYCorrection = 0, // 对应原有的 DownDp
        animDuration = basicAnimLong,
        onClick = { viewModel.toggleEntryPin(entry.id) }
    )
}

// DiaryCard: 日记卡片显示文本或图片
@Composable
fun DiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel
) {
    Log.d("I love my life", "entry's isPin:${entry.isPin}")
    val cardModifier = Modifier.clip(RoundedCornerShape(16.dp))
    val haptic = LocalHapticFeedback.current

    if (entry.type == EntryType.TEXT) {
        Box(
            modifier = cardModifier
                .widthIn(max = 220.dp) // 保持原有的最大宽度限制
                .clip(RoundedCornerShape(16.dp)) // 对应原有的 Card 圆角
                .pointerInput(entry.id) {
                    awaitEachGesture {
                        awaitFirstDown()
                        try {
                            withTimeout(longClickMs) { // 自定义时长
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    // 单击
                                    if (viewModel.isBatchManaging) {
                                        if (viewModel.batchEntries.contains(entry.id)) {
                                            viewModel.batchEntries.remove(entry.id)
                                        } else {
                                            viewModel.batchEntries.add(entry.id)
                                        }
                                    } else {
                                        viewModel.navigateToEditor(entry.id)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // --- 执行震动 ---
                            // LongPress 是最标准的“长按”震动效果
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 执行原本的长按逻辑[cite: 1]
                            if (viewModel.selectedEntryId != entry.id) {
                                viewModel.selectEntry(entry.id)
                            } else {
                                viewModel.unFocusEntry()
                            }

                            // 消耗后续事件直到抬起
                            var event: PointerEvent
                            do {
                                event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
        ) {
            // 1. 底层：专门负责模糊的背景层
            val textBg =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Color.White.copy(alpha = cardTransparentScale) else Color.White
            Box(
                modifier = Modifier
                    .matchParentSize() // 强制填充与外层 Box 一样的大小
                    .background(textBg) // 使用半透明白色，效果更接近毛玻璃
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // 硬件级模糊，仅 Android 12+ 支持
                            renderEffect = RenderEffect
                                .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)
                                .asComposeRenderEffect()
                        }
                    }
            )

            // 2. 内容层：文字部分
            Text(
                text = entry.content,
                modifier = Modifier.padding(16.dp), // 保持原有的 16.dp 间距
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    color = getTextColor(false)
                ),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
        }

    } else {
        val file = viewModel.getFullImagePath(entry.content)
        val imgWidth = if (entry.imageRatio > 1.5f || entry.isLarge) 240.dp else 120.dp

        Surface(
            modifier = cardModifier
                .pointerInput(entry.id) {
                    awaitEachGesture {
                        awaitFirstDown()

                        try {
                            withTimeout(longClickMs) { // 这里控制长按触发的时长
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    // --- 原 onClick 逻辑开始 ---
                                    // 单击
                                    if (viewModel.isBatchManaging) {
                                        if (viewModel.batchEntries.contains(entry.id)) {
                                            viewModel.batchEntries.remove(entry.id)
                                        } else {
                                            viewModel.batchEntries.add(entry.id)
                                        }
                                    } else {
                                        if (viewModel.hasSpace(entry.id)) {
                                            val spaceT =
                                                viewModel.spaces.find { it.entryId == entry.id }
                                            viewModel.setSDestination(spaceT?.id)
                                            if (spaceT?.password == "") {
                                                viewModel.changeSpace()
                                            } else {
                                                viewModel.showPasswordCheck = true
                                            }
                                        } else {
                                            viewModel.showImage(entry)
                                        }
                                        // --- 原 onClick 逻辑结束 ---
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // 1. 震动反馈
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // 2. 执行原 onLongClick 逻辑[cite: 1]
                            if (viewModel.selectedEntryId != entry.id) {
                                viewModel.selectEntry(entry.id)
                            } else {
                                viewModel.unFocusEntry()
                            }

                            // 3. 消耗掉剩余事件，直到手指抬起，防止误触点击
                            var event: PointerEvent
                            do {
                                event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .width(imgWidth)
                    .aspectRatio(entry.imageRatio)
                    .clipToBounds() // 必须裁剪超出部分
            ) {
                CroppedDisplayImage(
                    file = file,
                    // 如果是小卡片(120dp)，但 cropParams 是基于 240dp 保存的，
                    // 所以这里的 offset 需要除以 2。
                    // 逻辑：imgWidth / 240.dp
                    scaleAdjustment = if (imgWidth < 200.dp) 0.5f else 1f,
                    cropParams = entry.cropParams ?: CropParams() // 默认无裁剪
                )
            }
        }

    }
}

//CroppedDisplayImage: 渲染裁剪后的图片，不处理手势
@Composable
fun CroppedDisplayImage(
    file: File,
    scaleAdjustment: Float,
    cropParams: CropParams
) {
    // 状态：存储图片原始尺寸，初始为 0
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val transform =
            remember(imageIntrinsicSize, constraints.maxWidth, constraints.maxHeight, cropParams) {
                calculateTransform(
                    imageIntrinsicSize,
                    IntSize(constraints.maxWidth, constraints.maxHeight),
                    cropParams,
                    scaleAdjustment
                )
            }

        // 使用 Coil 的 AsyncImage
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .size(1024) // 列表显示限制大小，优化性能
                .scale(Scale.FIT) // 必须加载完整图像
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit, // 【关键】：保持 Fit 模式，由 graphicsLayer 负责放大
            alignment = Alignment.Center,
            // 【关键修改】：加载成功后回调，获取尺寸，触发重组以计算 totalScale
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                imageIntrinsicSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 应用计算好的变换
                    scaleX = transform.scale
                    scaleY = transform.scale
                    translationX = transform.offsetX
                    translationY = transform.offsetY
                }
        )
    }
}

// [新增] 颜色变暗的辅助函数
fun Color.darken(factor: Float = 0.7f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

// 简单的垂直滚动条修饰符
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    color: Color = Color.Gray.copy(alpha = 0.5f)
): Modifier = drawWithContent {
    drawContent()

    val firstVisibleElementIndex = state.firstVisibleItemIndex
    val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
    val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
    val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

    // 只有当内容超出屏幕时才绘制
    if (state.layoutInfo.totalItemsCount > state.layoutInfo.visibleItemsInfo.size) {
        drawRect(
            color = color,
            topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
            // 可以加上圆角逻辑，这里用简单的矩形
        )
    }
}


// A simpler, non-interactive version of DiaryCard for the sidebar view
@Composable
fun CompactDiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel
) {
    if (entry.type == EntryType.TEXT) {
        val textBg =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Color.White.copy(alpha = cardTransparentScale) else Color.White
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { viewModel.navigateToEditor(entry.id) }
        ) {
            // 1. 底层：专门负责模糊的背景层
            Box(
                modifier = Modifier
                    .matchParentSize() // 填充整个父布局
                    .background(textBg)
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // 这里设置模糊，只会影响背景色
                            renderEffect = RenderEffect
                                .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)
                                .asComposeRenderEffect()
                        }
                    }
            )
            Text(
                text = entry.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = getTextColor(false),
                fontWeight = FontWeight.Bold
            )
        }

    } else {
        val file = viewModel.getFullImagePath(entry.content)
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
            color = Color.White,
            modifier = Modifier.clickable {
                viewModel.showImage(entry)
            }
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(entry.imageRatio)
                    .clipToBounds()
            ) {
                // Reusing the crop logic but statically
                CroppedDisplayImage(
                    file = file,
                    scaleAdjustment = 0.5f, // Smaller scale for sidebar
                    cropParams = entry.cropParams ?: CropParams()
                )
            }
        }
    }
}


// FlowLayout: 流式布局，子组件自动换行
@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    spacing: Dp,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        content()
    }
}


@Composable
fun MoveEntryTo(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    viewModel: DailyViewModel
) {
    val targetSpaceId = remember { mutableStateOf("") }
    AlertDialog(
        title = {
            Text("移动Entry!")
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    viewModel.spaces.forEach {
                        item {
                            SpaceCard(
                                onSelected = { targetSpaceId.value = it.id },
                                onChangePortal = {},
                                it.name,
                                targetSpaceId.value == it.id
                            )
                        }
                    }
                    item {
                        SpaceCard(
                            onSelected = { targetSpaceId.value = "main" },
                            onChangePortal = {},
                            "主空间",
                            targetSpaceId.value == "main"
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                {
                    if (targetSpaceId.value.isBlank()) {
                        viewModel.toastOut("请选择空间💀")
                    } else {
                        onConfirm(targetSpaceId.value)
                    }
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AddFAB(
    currentTheme: DailyTimeTheme,
    viewModel: DailyViewModel,
    isExpanded: MutableState<Boolean>
) {
    val animLong = 200
    // 定义动画参数
    val containerWidth by animateDpAsState(
        targetValue = if (isExpanded.value) 135.dp else 56.dp,
        animationSpec = tween(animLong, easing = EaseOutSine)
    )
    val containerHeight by animateDpAsState(
        targetValue = if (isExpanded.value) 135.dp else 56.dp,
        animationSpec = tween(animLong, easing = EaseOutSine)
    )
    val cornerSize by animateDpAsState(
        targetValue = if (isExpanded.value) 16.dp else 28.dp, // 从圆滑变稍微方一点
        animationSpec = tween(animLong)
    )

    val onPrimaryColor = currentTheme.backgroundColor

    Box(
        modifier = Modifier
            .size(width = containerWidth, height = containerHeight) // FAB 的标准尺寸
            // (A) 阴影：必须放在 clip 和 background 之前，否则阴影会被剪掉
            .graphicsLayer {
                shadowElevation = 6.dp.toPx() // FAB 默认高度
                shape = RoundedCornerShape(cornerSize)
                clip = false // 允许阴影溢出显示
            }
            .clip(RoundedCornerShape(cornerSize))
            .background(currentTheme.primaryColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = {
                    isExpanded.value = true
                }), // 使用主题色
        contentAlignment = Alignment.Center
    ) {
        if (!isExpanded.value) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                tint = onPrimaryColor // 保持图标颜色一致
            )
        } else {
            Column(modifier = Modifier.padding(8.dp)) {
                @Composable
                fun OptionItem(
                    icon: ImageVector,
                    text: String,
                    onClick: () -> Unit
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onClick)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = onPrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = onPrimaryColor
                        )
                    }
                }

                OptionItem(Icons.Default.Edit, "Daily") {
                    isExpanded.value = false
                    viewModel.navigateToEditor(null)
                }
                // 方案 B：使用系统自带的分割线组件（推荐）
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = onPrimaryColor.copy(alpha = 0.2f)
                )
                OptionItem(Icons.Default.AccountBox, "Photo") {
                    isExpanded.value = false
                    viewModel.navigateToImagePicker()
                }
            }
        }
    }
}
