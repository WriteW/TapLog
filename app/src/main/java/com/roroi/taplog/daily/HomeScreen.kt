package com.roroi.taplog.daily

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Output
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.CoroutineScope
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
    viewModel: DailyViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToImagePicker: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }
    val rightSidebarListState = rememberLazyListState()
    val groups by viewModel.groupedEntries.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current

    val currentTheme = remember { DailyTimeTheme.getCurrent() }

    // 侧边栏状态
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val hazeState = remember { HazeState() }

    // 图片查看器状态
    var viewingImageEntry by remember { mutableStateOf<DailyEntry?>(null) }

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

    // Toast 处理
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // 图片全屏查看器
    if (viewingImageEntry != null) {
        ImageViewerDialog(
            entry = viewingImageEntry!!,
            viewModel = viewModel,
            onDismiss = { viewingImageEntry = null },
            onDelete = {
                viewModel.deleteEntry(viewingImageEntry!!.id)
                viewingImageEntry = null
            }
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
                }
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
                                rightDrawerState.close() // 关闭侧边栏
                                listState.scrollToItem(index) // 跳转到对应位置
                            }
                        }
                    )
                }
            ) {
                // --- 第三层：主界面内容 ---
                // 必须改回 LTR (从左向右)，否则主界面的文字和布局全是反的！
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {


                    var showAddDialog by remember { mutableStateOf(false) }
                    @Suppress("DEPRECATION")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(
                                state = hazeState,
                                // 告诉 Haze 背景的基础色，有助于计算混合，但不要乱加 tint
                                backgroundColor = currentTheme.backgroundColor
                            )
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        val touchX = change.position.x
                                        val screenWidth = size.width

                                        // 打开左侧框
                                        if (touchX < screenWidth / 2 && dragAmount > 20) {
                                            if (leftDrawerState.isClosed && rightDrawerState.isClosed) {
                                                scope.launch { leftDrawerState.open() }
                                            }
                                        }
                                        // 打开右侧框
                                        else if (touchX > screenWidth / 2 && dragAmount < -20) {
                                            if (leftDrawerState.isClosed && rightDrawerState.isClosed) {
                                                scope.launch {
                                                    rightSidebarListState.scrollToItem(0)
                                                    rightDrawerState.open()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        DailyDynamicBackground(theme = currentTheme)

                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                CenterAlignedTopAppBar(
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { leftDrawerState.open() } }) {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Menu",
                                                tint = currentTheme.onSurfaceColor
                                            )
                                        }
                                    },
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
                                                            listState.animateScrollToItem(0)
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
                                                modifier = Modifier.padding(top = 2.dp)
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
                                                color = currentTheme.primaryColor // 时间大字颜色
                                            )
                                        }
                                    }
                                )
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { showAddDialog = true },
                                    containerColor = currentTheme.primaryColor, // FAB 颜色跟随主题
                                    contentColor = Color.White,
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .padding(padding)
                                    .fillMaxSize()
                            ) {
                                // 背景灰线
                                val lineColor = TimelineGray
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
                                            viewModel = viewModel,
                                            theme = currentTheme,
                                            onEdit = onNavigateToEditor,
                                            onImageClick = { entry -> viewingImageEntry = entry }
                                        )
                                    }
                                }

                                if (showAddDialog) {
                                    AddSelectionDialog(
                                        onDismiss = { showAddDialog = false },
                                        onDailyClick = {
                                            showAddDialog = false
                                            onNavigateToEditor(null)
                                        },
                                        onPhotoClick = {
                                            showAddDialog = false
                                            onNavigateToImagePicker()
                                        }
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

// 渲染时间轴单行，包括：时间/日期/年份,显示圆点颜色和位置,日志卡片列表（文本/图片）
@Composable
fun TimelineRow(
    group: TimelineGroup,
    viewModel: DailyViewModel,
    theme: DailyTimeTheme,
    onEdit: (String) -> Unit,
    onImageClick: (DailyEntry) -> Unit
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
    val textColor = theme.onSurfaceColor // 保证文字清晰可读

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
                    textAlign = TextAlign.End
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
                drawCircle(color = Color.White, radius = size.minDimension / 2 + 2.dp.toPx())
                drawCircle(color = dynamicDotColor, radius = size.minDimension / 2)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            FlowLayout(spacing = 10.dp) {
                group.items.forEach { entry ->
                    DiaryCard(entry, viewModel, onEdit, onImageClick)
                }
            }
        }
    }
}

// DiaryCard: 日记卡片显示文本或图片
@Composable
fun DiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    onEdit: (String) -> Unit,
    onImageClick: (DailyEntry) -> Unit
) {
    val cardModifier = Modifier.clip(RoundedCornerShape(16.dp))

    if (entry.type == EntryType.TEXT) {
        Card(
            modifier = cardModifier
                .widthIn(max = 220.dp)
                .clickable { onEdit(entry.id) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = entry.content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    color = TextColor
                ),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        val file = viewModel.getFullImagePath(entry.content)
        val imgWidth = if (entry.imageRatio > 1.5f || entry.isLarge) 240.dp else 120.dp

        Surface(
            modifier = cardModifier.clickable { onImageClick(entry) },
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

// 左侧边栏
@Composable
fun LeftSidebarContent(
    theme: DailyTimeTheme,
    hazeState: HazeState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    var clearConfirmCount by remember { mutableIntStateOf(0) }

    if (clearConfirmCount > 0) {
        ClearConfirmationDialog(clearConfirmCount, onClear) { clearConfirmCount = it }
    }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = Color.Transparent,
        drawerContentColor = theme.onSurfaceColor,
        modifier = Modifier
            .width(300.dp)
            .hazeChild(state = hazeState)
    ) {
        LeftSidebarHeader(theme)
        LeftSidebarActions(onExport, onImport)
        LeftSidebarDangerZone { clearConfirmCount = 1 }
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

@Composable
fun RightSidebarContent(
    listState: LazyListState,
    scope: CoroutineScope,
    viewModel: DailyViewModel, // [ADDED] Parameter
    theme: DailyTimeTheme,
    hazeState: HazeState,
    onJumpToGroup: (Int) -> Unit
) {
    val groups by viewModel.groupedEntries.collectAsState()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        ModalDrawerSheet(
            drawerShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            drawerContainerColor = Color.Transparent, // Transparent to show Haze
            drawerContentColor = theme.onSurfaceColor,
            modifier = Modifier
                .width(340.dp) // Slightly wider to accommodate zigzag layout
                .hazeChild(state = hazeState)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = theme.primaryColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 这里的点击不要波纹，因为是背景线
                        ) {
                            scope.launch {
                                if (groups.isNotEmpty()) {
                                    // 滚动到最后一项
                                    listState.animateScrollToItem(groups.size - 1)
                                }
                            }
                        }
                )
            }

            // Timeline List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 40.dp),
                state = listState
            ) {
                itemsIndexed(groups) { index, group ->
                    RightSidebarGroupItem(
                        index = index, // 传入索引
                        group = group,
                        viewModel = viewModel,
                        theme = theme,
                        onTimeClick = { onJumpToGroup(index) } // [新增] 传递点击事件
                    )
                }
            }
        }
    }
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

@Composable
fun RightSidebarGroupItem(
    index: Int,
    group: TimelineGroup,
    viewModel: DailyViewModel,
    theme: DailyTimeTheme,
    onTimeClick: () -> Unit
) {
    val dateObj = Date(group.timestamp)
    val timeStr =
        remember(group.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(dateObj) }
    val dateStr =
        remember(group.timestamp) { SimpleDateFormat("MM/dd", Locale.getDefault()).format(dateObj) }

    val dotColor = viewModel.getTimelineColor(group.timestamp)
    val entries = group.items

    // 偶数行(0,2,4) -> 内容在右，时间在左
    // 奇数行(1,3,5) -> 内容在左，时间在右
    val isFirstEntryRight = index % 2 == 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 1. 中轴线
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.Center)
        ) {
            drawRect(color = Color(0xFFE0E0E0))
        }

        Column(modifier = Modifier.fillMaxWidth()) {

            // --- 第一行：根据 isFirstEntryRight 决定布局 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // [左侧区域]
                if (isFirstEntryRight) {
                    // A情况：时间在左 (靠右对齐，贴近中线)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp), contentAlignment = Alignment.TopEnd
                    ) {
                        TimeJumpButton(
                            time = timeStr,
                            date = dateStr,
                            theme = theme,
                            alignment = Alignment.End,
                            onClick = onTimeClick
                        )
                    }
                } else {
                    // B情况：内容在左 (靠右对齐，贴近中线)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp), contentAlignment = Alignment.TopEnd
                    ) {
                        if (entries.isNotEmpty()) CompactDiaryCard(entries[0], viewModel)
                    }
                }

                // [中间圆点]
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension / 2 + 2.dp.toPx()
                        )
                        drawCircle(color = dotColor, radius = size.minDimension / 2)
                    }
                }

                // [右侧区域]
                if (isFirstEntryRight) {
                    // A情况：内容在右 (靠左对齐，贴近中线)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp), contentAlignment = Alignment.TopStart
                    ) {
                        if (entries.isNotEmpty()) CompactDiaryCard(entries[0], viewModel)
                    }
                } else {
                    // B情况：时间在右 (靠左对齐，贴近中线)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp), contentAlignment = Alignment.TopStart
                    ) {
                        TimeJumpButton(
                            time = timeStr,
                            date = dateStr,
                            theme = theme,
                            alignment = Alignment.Start,
                            onClick = onTimeClick
                        )
                    }
                }
            }

            // --- 组内后续内容：交错排列 ---
            if (entries.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))

                entries.drop(1).forEachIndexed { subIndex, entry ->
                    // 计算逻辑：
                    // 如果首条在右 (isFirstEntryRight=true)，则次条(subIndex=0)应该在左
                    // 如果首条在左 (isFirstEntryRight=false)，则次条(subIndex=0)应该在右
                    val isCurrentSubItemLeft = if (isFirstEntryRight) {
                        subIndex % 2 == 0 // true -> Left
                    } else {
                        subIndex % 2 != 0 // false -> Right (Corrected logic)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        if (isCurrentSubItemLeft) {
                            // 内容在左
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp), contentAlignment = Alignment.CenterEnd
                            ) {
                                CompactDiaryCard(entry, viewModel)
                            }
                            Spacer(modifier = Modifier.width(16.dp)) // 中轴占位
                            Spacer(modifier = Modifier.weight(1f))     // 右侧留白
                        } else {
                            // 内容在右
                            Spacer(modifier = Modifier.weight(1f))     // 左侧留白
                            Spacer(modifier = Modifier.width(16.dp)) // 中轴占位
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CompactDiaryCard(entry, viewModel)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TimeJumpButton(
    time: String,
    date: String,
    theme: DailyTimeTheme,
    alignment: Alignment.Horizontal,
    onClick: () -> Unit
) {
    // 1. 定义波纹颜色 (深色主题色)
    val rippleColor = theme.primaryColor.darken(0.6f)

    // 2. 使用 Box + clickable 实现，这比 Surface 更灵活
    Box(
        modifier = Modifier
            // (A) 裁剪成圆角 (对应 Surface 的 shape)
            .clip(RoundedCornerShape(12.dp))
            // (B) 自定义点击事件和波纹
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // [重点修复] 使用新的 ripple() API，而不是 rememberRipple
                indication = ripple(color = rippleColor),
                onClick = onClick
            )
            // (C) 稍微增加一点内边距，保证文字和波纹不贴边
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = alignment
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = theme.primaryColor
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = theme.onSurfaceColor.copy(alpha = 0.6f)
            )
        }
    }
}

// A simpler, non-interactive version of DiaryCard for the sidebar view
@Composable
fun CompactDiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel
) {
    if (entry.type == EntryType.TEXT) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = entry.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        val file = viewModel.getFullImagePath(entry.content)
        Surface(
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
            color = Color.White
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

// ImageViewerDialog: 全屏图片查看器，支持缩放拖动和删除
@Composable
fun ImageViewerDialog(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val file = viewModel.getFullImagePath(entry.content)

    // 删除确认弹窗状态
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 缩放和平移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除图片") },
            text = { Text("确定要删除这张图片吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete() // 确认删除
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 全屏
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. 图片层：处理手势
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 监听手势
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 计算缩放：限制最小为 1f (原大小)，最大为 4f
                            scale = (scale * zoom).coerceIn(1f, 4f)

                            // 计算位移：如果缩放比例为 1，则强制归位(0,0)，否则允许移动
                            if (scale == 1f) {
                                offset = Offset.Zero
                            } else {
                                // 简单的累加位移，实际项目中可增加边界限制算法
                                val newOffset = offset + pan
                                offset = newOffset
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    // 点击图片区域也可以关闭 (如果未缩放)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (scale == 1f) onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full Image",
                    contentScale = ContentScale.Fit, // Fit 保证完整显示
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. 顶部关闭按钮 (可选，方便用户知道怎么关)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // 3. 底部操作栏 (删除按钮)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(), // 适配手势导航条
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(
                    onClick = { showDeleteConfirm = true }, // 点击触发弹窗
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
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

// AddSelectionDialog: 添加新条目选择对话框
@Composable
fun AddSelectionDialog(
    onDismiss: () -> Unit,
    onDailyClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier.width(240.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Add New",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

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
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextColor
                        )
                    }
                }

                OptionItem(Icons.Default.Edit, "Daily", onDailyClick)
                OptionItem(Icons.Default.AccountBox, "Photo", onPhotoClick)
            }
        }
    }
}

// 左侧边栏的分裂
@Composable
fun LeftSidebarHeader(theme: DailyTimeTheme) {
    Spacer(Modifier.height(32.dp))
    Text(
        "Data Management",
        modifier = Modifier.padding(start = 28.dp, bottom = 24.dp),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = theme.primaryColor
    )
}

@Composable
fun LeftSidebarActions(onExport: () -> Unit, onImport: () -> Unit) {
    NavigationDrawerItem(
        label = { Text("Export Data (ZIP)") },
        selected = false,
        onClick = onExport,
        icon = { Icon(Icons.Default.Output, null) })
    NavigationDrawerItem(
        label = { Text("Import Data (ZIP)") },
        selected = false,
        onClick = onImport,
        icon = { Icon(Icons.AutoMirrored.Filled.Input, null) })
    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp, horizontal = 28.dp))
}

@Composable
fun LeftSidebarDangerZone(onClear: () -> Unit) {
    Text("Danger Zone", modifier = Modifier.padding(start = 28.dp, bottom = 12.dp))
    NavigationDrawerItem(
        label = { Text("Clear All Data") },
        selected = false,
        onClick = onClear,
        icon = { Icon(Icons.Default.Delete, null) })
}

@Composable
fun ClearConfirmationDialog(count: Int, onClear: () -> Unit, onDismiss: (Int) -> Unit) {
    val (title, text) = when (count) {
        1 -> "删除所有数据？" to "这将删除您的所有条目和照片。确定吗？"
        2 -> "确定要删除？" to "此操作不可撤销。所有数据将永久丢失。"
        3 -> "最终警告" to "点击确认将清空所有数据。"
        else -> "" to ""
    }

    AlertDialog(
        onDismissRequest = { onDismiss(0) },
        title = { Text(title, fontWeight = FontWeight.Bold, color = Color.Red) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                if (count == 3) {
                    onClear(); onDismiss(0)
                } else onDismiss(count + 1)
            }) { Text(if (count == 3) "WIPE" else "Confirm", color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { onDismiss(0) }) { Text("Cancel") } }
    )
}