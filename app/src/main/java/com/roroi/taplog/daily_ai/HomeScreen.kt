package com.roroi.taplog.daily_ai

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// --- 布局常量配置 ---
// 左侧：时间文字区域的宽度 (控制时间离左屏幕的距离 + 文字活动空间)
private val TEXT_AREA_WIDTH = 65.dp

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
    val groups by viewModel.groupedEntries.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val context = LocalContext.current

    val currentTheme = remember { DailyTimeTheme.getCurrent() }

    // 侧边栏状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val hazeState = remember { HazeState() }

    // 图片查看器状态
    var viewingImageEntry by remember { mutableStateOf<DailyEntry?>(null) }

    // --- 导入/导出 Launchers ---

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
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            kotlinx.coroutines.delay(1000)
        }
    }

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
        drawerState = drawerState,
        scrimColor = Color.Transparent,
        drawerContent = {
            SidebarContent(
                // 【UI应用】：传入主题颜色给侧边栏
                theme = currentTheme,
                hazeState = hazeState,
                onExport = { /* ... */ },
                onImport = { /* ... */ },
                onClear = { /* ... */ }
            )
        }
    ) {
        var showAddDialog by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = hazeState,
                    // 告诉 Haze 背景的基础色，有助于计算混合，但不要乱加 tint
                    backgroundColor = currentTheme.backgroundColor
                )
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
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
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
                                modifier = Modifier.padding(vertical = 2.dp)
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
                                    color = currentTheme.onSurfaceColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
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
                        modifier = Modifier.fillMaxSize(),
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

@Composable
fun TimelineRow(
    group: TimelineGroup,
    viewModel: DailyViewModel,
    theme: DailyTimeTheme,
    onEdit: (String) -> Unit,
    onImageClick: (DailyEntry) -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(group.timestamp))

    // 动态计算的颜色
    val dynamicDotColor = viewModel.getTimelineColor(group.timestamp)
    val textColor = theme.onSurfaceColor // 保证文字清晰可读

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(TOTAL_SIDEBAR_WIDTH), // 使用总宽度
            contentAlignment = Alignment.TopStart //以此为基准
        ) {
            // 1. 时间文本
            // 我们限制它的宽度为 TEXT_AREA_WIDTH，这样它就在线条左侧
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                textAlign = TextAlign.End, // 文字靠右对齐(贴近线条)
                modifier = Modifier
                    .width(TEXT_AREA_WIDTH) // 关键：限制宽度
                    .padding(top = CARD_TOP_OFFSET - 3.dp)
                    // end: 文字和线的距离; start: 文字和屏幕左边的距离
                    .padding(end = 16.dp, start = 16.dp)
            )

            // 2. 时间轴圆点
            // 圆点需要精确地压在线条上 (x = TEXT_AREA_WIDTH)
            // 但 Canvas 默认从左上角画，所以要减去半径来居中
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

// 【新增组件】：只负责显示裁剪后的结果，不处理手势
@Composable
fun CroppedDisplayImage(
    file: java.io.File,
    scaleAdjustment: Float,
    cropParams: CropParams
) {
    // 状态：存储图片原始尺寸，初始为 0
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // 默认值
        var totalScale = 1f
        var adjustedOffsetX = 0f
        var adjustedOffsetY = 0f

        // 只有当图片加载完成(获取到尺寸)且容器有尺寸时，才进行复杂的裁剪计算
        if (imageIntrinsicSize.width > 0 && containerWidth > 0) {
            val imageRatio = imageIntrinsicSize.width.toFloat() / imageIntrinsicSize.height
            val containerRatio = containerWidth / containerHeight

            val fittedWidth: Float
            val fittedHeight: Float

            if (imageRatio > containerRatio) {
                fittedWidth = containerWidth
                fittedHeight = fittedWidth / imageRatio
            } else {
                fittedHeight = containerHeight
                fittedWidth = fittedHeight * imageRatio
            }

            // 1. 计算填满黑边的基础缩放
            val baseScale = max(
                containerWidth / fittedWidth,
                containerHeight / fittedHeight
            )

            // 2. 叠加用户缩放
            totalScale = baseScale * cropParams.userScale

            // 3. 叠加用户位移 (根据卡片大小比例适配)
            adjustedOffsetX = cropParams.userOffsetX * scaleAdjustment
            adjustedOffsetY = cropParams.userOffsetY * scaleAdjustment
        }

        // 使用 Coil 的 AsyncImage
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(file)
                .size(1024) // 列表显示限制大小，优化性能
                .scale(coil.size.Scale.FIT) // 必须加载完整图像
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
                    scaleX = totalScale
                    scaleY = totalScale
                    translationX = adjustedOffsetX
                    translationY = adjustedOffsetY
                }
        )
    }
}

// 侧边栏内容
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun SidebarContent(
    theme: DailyTimeTheme, // 接收主题
    hazeState: HazeState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    // 清空确认逻辑 (保持不变)
    var clearConfirmCount by remember { mutableIntStateOf(0) }

    if (clearConfirmCount > 0) {
        // ... (弹窗逻辑保持不变) ...
        val (title, text) = when (clearConfirmCount) {
            1 -> "删除所有数据？" to "这将删除您的所有条目和照片。确定吗？"
            2 -> "确定要删除？" to "此操作不可撤销。所有数据将永久丢失。"
            3 -> "最终警告" to "点击确认将清空所有数据。"
            else -> "" to ""
        }

        AlertDialog(
            onDismissRequest = { clearConfirmCount = 0 },
            title = { Text(title, fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    if (clearConfirmCount == 3) {
                        onClear()
                        clearConfirmCount = 0
                    } else {
                        clearConfirmCount++
                    }
                }) {
                    Text(if (clearConfirmCount == 3) "WIPE" else "Confirm", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmCount = 0 }) { Text("Cancel") }
            },
            containerColor = Color.White, // 弹窗保持白色实心，防止看不清
            tonalElevation = 6.dp
        )
    }
    val sidebarShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)

    // 【核心修改】：毛玻璃质感抽屉
    ModalDrawerSheet(
        // 1. 形状：右侧圆角
        drawerShape = sidebarShape,

        // 2. 颜色：使用主题背景色，但透明度设为 0.85
        // 这样既能透出背后运动的小球，又能看清上面的文字
        // 早上是半透明蓝，晚上是半透明粉
        drawerContainerColor = Color.Transparent,

        // 3. 内容颜色：跟随主题
        drawerContentColor = theme.onSurfaceColor,

        // 4. 宽度：限制一下宽度，显得更精致
        modifier = Modifier
            .width(300.dp)
            .hazeChild(
                state = hazeState,
                shape = sidebarShape, // 3. 必须把形状传给 Haze，否则就是直角！
                style = HazeMaterials.regular( // 4. 改用 regular，模糊感更强
                    // 5. 调整颜色：Alpha 设为 0.4f (越低越透，越高越实)
                    containerColor = theme.backgroundColor.copy(alpha = 0.4f),
                )
            )
            // 5. 【可选】给抽屉加一个极细的白色边框，增强玻璃的厚度感
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ),
    ) {
        // 顶部留白
        Spacer(Modifier.height(32.dp))

        // 标题
        Text(
            "Data Management",
            modifier = Modifier.padding(start = 28.dp, bottom = 24.dp),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = theme.primaryColor // 标题跟随主题色
        )

        // 选项列表
        NavigationDrawerItem(
            label = { Text("Export Data (ZIP)") },
            selected = false,
            onClick = onExport,
            icon = { Icon(Icons.Default.Output, null) },
            // Item 的颜色配置：让它也是半透明的，与背景融合
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = theme.primaryColor,
                unselectedTextColor = theme.onSurfaceColor
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            label = { Text("Import Data (ZIP)") },
            selected = false,
            onClick = onImport,
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Input,
                    null
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent,
                unselectedIconColor = theme.primaryColor,
                unselectedTextColor = theme.onSurfaceColor
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // 分割线：半透明白色，更像玻璃划痕
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 28.dp),
            thickness = DividerDefaults.Thickness, color = theme.onSurfaceColor.copy(alpha = 0.1f)
        )

        Text(
            "Danger Zone",
            modifier = Modifier.padding(start = 28.dp, bottom = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = theme.onSurfaceColor.copy(alpha = 0.6f)
        )

        NavigationDrawerItem(
            label = { Text("Clear All Data", fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = { clearConfirmCount = 1 },
            icon = { Icon(Icons.Default.Delete, null) },
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Red.copy(alpha = 0.1f), // 危险区域给一点点红色背景
                unselectedIconColor = Color.Red.copy(alpha = 0.8f),
                unselectedTextColor = Color.Red.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// 全屏图片查看器
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

@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp,
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
                    icon: androidx.compose.ui.graphics.vector.ImageVector,
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