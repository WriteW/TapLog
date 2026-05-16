package com.roroi.taplog.daily

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.zIndex
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
import com.roroi.taplog.daily.viewmodel.canDisplayInline
import com.roroi.taplog.daily.viewmodel.getDotColor
import com.roroi.taplog.daily.viewmodel.supportPortal
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

@SuppressLint("ConstantLocale")
val TimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@SuppressLint("ConstantLocale")
val DateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

@SuppressLint("ConstantLocale")
val YearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

@SuppressLint("ConstantLocale")
val FullDateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())

@Composable
fun GlassmorphismBackground(
    modifier: Modifier = Modifier,
    alpha: Float = cardTransparentScale,
    blurRadius: Float = 80f
) {
    val isAndroidS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val textBg = if (isAndroidS) Color.White.copy(alpha = alpha) else Color.White
    Box(
        modifier = modifier
            .background(textBg)
            .graphicsLayer {
                if (isAndroidS) {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR)
                        .asComposeRenderEffect()
                }
            }
    )
}

fun Modifier.diaryGestures(
    entryId: String,
    viewModel: DailyViewModel,
    haptic: HapticFeedback,
    onClick: () -> Unit
): Modifier = this.pointerInput(entryId) {
    awaitEachGesture {
        awaitFirstDown()
        try {
            withTimeout(longClickMs) {
                val up = waitForUpOrCancellation()
                if (up != null) {
                    up.consume()
                    if (viewModel.isBatchManaging) {
                        if (viewModel.batchEntries.contains(entryId)) {
                            viewModel.batchEntries.remove(entryId)
                        } else {
                            viewModel.batchEntries.add(entryId)
                        }
                    } else {
                        onClick()
                    }
                }
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (viewModel.selectedEntryId != entryId) {
                viewModel.selectEntry(entryId)
            } else {
                viewModel.unFocusEntry()
            }
            var event: PointerEvent
            do {
                event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

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
                viewModel.verifyAndEnterSpace(inputPassword) // 走新逻辑
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
                viewModel.changeSpacePassword(newPass)
                viewModel.showChangePassword = false
            },
            hasOldPassword = currentSpace.isEncrypted // 使用 isEncrypted 状态
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
                onCloseSidebar = { scope.launch { leftDrawerState.close() } },
                viewModel = viewModel
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
                        BackHandler(enabled = true) {
                            if (viewModel.viewingCapsuleId != null) {
                                viewModel.exitCapsuleSpace() // 退出胶囊虚拟空间
                            } else if (leftDrawerState.isOpen) {
                                scope.launch { leftDrawerState.close() }
                            } else if (rightDrawerState.isOpen) {
                                scope.launch { rightDrawerState.close() }
                            } else if (isAddFABExpand.value) {
                                isAddFABExpand.value = false
                            } else if (viewModel.selectedDSpaceId?.isNotBlank() == true) {
                                viewModel.exitToMainSpace() // 内部已经包含了加密逻辑
                            } else if (!viewModel.showPasswordCheck) {
                                // 彻底退出应用前确保锁门
                                scope.launch {
                                    viewModel.showLoadingDialog = true
                                    viewModel.lockCurrentSpaceIfNeeded()
                                    (context as? Activity)?.finish()
                                }
                            }
                        }


                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                val spaceColor =
                                    viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.colorBgArgb
                                val finalColor =
                                    spaceColor?.let { Color(it).copy(alpha = 0.5f) }
                                        ?: Color.White.copy(alpha = 0.5f)
                                AnimatedContent(
                                    targetState = viewModel.viewingCapsuleId != null,
                                    label = "CapsuleSpaceTransition"
                                ) { inCapsule ->
                                    if (inCapsule) {
                                        val capsule =
                                            viewModel.timeCapsules.find { it.id == viewModel.viewingCapsuleId }
                                        val creationDate =
                                            capsule?.createdAt?.let { Date(it) } ?: currentTime
                                        // 胶囊空间专属 TopBar
                                        CenterAlignedTopAppBar(
                                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor = finalColor
                                            ),
                                            title = {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "From: ${
                                                            FullDateFormat.format(
                                                                creationDate
                                                            )
                                                        }",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        ),
                                                        color = currentTheme.onSurfaceColor.copy(
                                                            alpha = 0.7f
                                                        ),
                                                        fontFamily = dymonFont
                                                    )
                                                    Text(
                                                        text = TimeFormat.format(
                                                            creationDate
                                                        ),
                                                        style = MaterialTheme.typography.displaySmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 24.sp,
                                                            letterSpacing = 1.sp
                                                        ),
                                                        color = currentTheme.primaryColor,
                                                        fontFamily = dymonFont
                                                    )
                                                }
                                            },
                                            navigationIcon = {
                                                IconButton({ viewModel.exitCapsuleSpace() }) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Exit Capsule"
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = viewModel.isBatchManaging,
                                            label = "TopBarTransition"
                                        ) { isBatch ->
                                            if (isBatch) {
                                                // 【新增】：封存胶囊模式
                                                if (viewModel.pendingCapsule != null) {
                                                    CenterAlignedTopAppBar(
                                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                            containerColor = finalColor
                                                        ),
                                                        title = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .clickable { viewModel.confirmCapsuleCreation() }
                                                                    .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "Confirm ${viewModel.batchEntries.size} entries into Capsule",
                                                                    color = currentTheme.primaryColor,
                                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                )
                                                            }
                                                        },
                                                        navigationIcon = {
                                                            IconButton({ viewModel.stopBatchSelecting() }) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    null
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                                // 【新增】：判断是否处于合并模式 (Binding Mode)
                                                else if (viewModel.bindingTargetId != null) {
                                                    CenterAlignedTopAppBar(
                                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                            containerColor = finalColor
                                                        ),
                                                        title = {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .clickable { viewModel.executeBinding() } // 点击执行合并
                                                                    .padding(
                                                                        horizontal = 8.dp,
                                                                        vertical = 8.dp
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "Click me after selecting the entries to combine.",
                                                                    color = currentTheme.primaryColor,
                                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        lineHeight = 18.sp
                                                                    ),
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 2 // 防止小屏幕手机文字过长挤爆
                                                                )
                                                            }
                                                        },
                                                        navigationIcon = {
                                                            // 左侧提供退出按钮，点击后恢复原状
                                                            IconButton({ viewModel.stopBatchSelecting() }) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Cancel Binding"
                                                                )
                                                            }
                                                        }
                                                    )
                                                } else {
                                                    // 原本的普通批量管理 TopBar
                                                    CenterAlignedTopAppBar(
                                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                            containerColor = Color.Transparent
                                                        ),
                                                        title = {
                                                            Text(
                                                                "批量操作",
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                        },
                                                        navigationIcon = {
                                                            IconButton({ viewModel.stopBatchSelecting() }) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Close Batch"
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            } else {
                                                NormalTopBar(
                                                    viewModel,
                                                    scope,
                                                    currentTheme,
                                                    leftDrawerState,
                                                    listState,
                                                    currentTime
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            floatingActionButton = {
                                if (viewModel.viewingCapsuleId == null) {
                                    AddFAB(currentTheme, viewModel, isAddFABExpand)
                                }
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
        if (viewModel.radialMenuEntryId != null) {
            RadialMenuOverlay(
                viewModel = viewModel,
                entryId = viewModel.radialMenuEntryId!!,
                theme = currentTheme
            )
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
        TimeFormat.format(dateObj)
    }
    val dateStr = remember(group.timestamp) {
        // 你可以改成 "MM月dd日" 或者 "MM/dd"
        DateFormat.format(dateObj)
    }
    val yearStr = remember(group.timestamp) {
        YearFormat.format(dateObj)
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

                    if (current.canDisplayInline() && next?.canDisplayInline() == true) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.animateContentSize(
                                animationSpec = tween(
                                    basicAnimLong
                                )
                            )
                        ) {
                            EntryWithButtons(current, viewModel)
                            EntryWithButtons(next, viewModel)
                        }
                        i += 2
                    } else {
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
                if (entry.supportPortal() && !viewModel.isBatchManaging) {
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
                    MoreButton(entry, viewModel)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box {
                    DiaryCard(entry, viewModel)

                    // 【修复区域】：加上 if (isSelected) 判断
                    if (viewModel.isBatchManaging && viewModel.batchEntries.contains(entry.id)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 6.dp)
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Blue, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "selected",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
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

@Composable
fun MoreButton(entry: DailyEntry, viewModel: DailyViewModel) {
    EntryActionButton(
        entry = entry,
        viewModel = viewModel,
        icon = Icons.Default.MoreVert,
        contentDescription = "more",
        offsetYCorrection = -44, // 替代之前的 MoveButton 位置
        animDuration = basicAnimLong + 100,
        onClick = { viewModel.openRadialMenu(entry.id) } // 唤醒全屏环形菜单
    )
}

@Composable
fun RadialMenuOverlay(
    viewModel: DailyViewModel,
    entryId: String,
    theme: DailyTimeTheme
) {
    // 关闭动作
    val dismiss = { viewModel.closeRadialMenu() }

    // 拦截系统的物理返回键
    BackHandler(onBack = dismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // 确保在绝对顶层
            .background(Color.Black.copy(alpha = 0.5f)) // 背景变暗
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismiss // 点击暗处关闭
            ),
        contentAlignment = Alignment.Center
    ) {
        // 毛玻璃中空圆盘背景
        val radiusDp = 100.dp
        val radiusPx = with(LocalDensity.current) { radiusDp.toPx() }

        Box(
            modifier = Modifier
                .size(radiusDp * 2.5f), // 💡 修复：移除了 .clip(CircleShape)
            contentAlignment = Alignment.Center
        ) {
            // 你也可以在这里加入 Canvas 画圆环描边，强化视觉感受
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 60f)
                )
            }

            // 定义想要放在菜单上的按钮列表
            val actions = listOf(
                Triple(Icons.Filled.SubdirectoryArrowRight, "Move") {
                    dismiss()
                    viewModel.selectEntry(entryId)
                    viewModel.showSelectSpaceM = true
                },
                // 【新增】：合并绑定按钮
                Triple(Icons.Filled.Link, "Bind") {
                    dismiss() // 收起圆盘
                    viewModel.startBindingMode(entryId) // 进入合并模式
                },
                Triple(Icons.Filled.LinkOff, "Unbind") {
                    dismiss()
                    viewModel.unbindEntryFromGroup(entryId)
                }
            )

            // 利用三角函数，把按钮均匀撒在圆周上
            val angleStep = (2 * Math.PI) / actions.size.coerceAtLeast(1)
            val startAngle = -Math.PI / 2

            actions.forEachIndexed { index, action ->
                val (icon, label, onClick) = action
                val angle = startAngle + index * angleStep

                val offsetX = (radiusPx * kotlin.math.cos(angle)).toFloat()
                val offsetY = (radiusPx * kotlin.math.sin(angle)).toFloat()

                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .offset(
                            x = with(LocalDensity.current) { offsetX.toDp() },
                            y = with(LocalDensity.current) { offsetY.toDp() }
                        )
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = theme.backgroundColor
                    )
                }
            }
        }
    }
}

// DiaryCard: 日记卡片显示文本或图片
@Composable
fun DiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel
) {
    val haptic = LocalHapticFeedback.current
    val cardModifier = Modifier.clip(RoundedCornerShape(16.dp))

    // 未来添加音频视频，只需在这里加 EntryType.AUDIO -> AudioDiaryCard(...)
    when (entry.type) {
        EntryType.TEXT -> {
            TextDiaryCard(entry, viewModel, haptic, cardModifier)
        }

        EntryType.IMAGE -> {
            ImageDiaryCard(entry, viewModel, haptic, cardModifier)
        }
    }
}

// 抽取后的纯文本组件
@Composable
private fun TextDiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    haptic: HapticFeedback,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 220.dp)
            .diaryGestures(entry.id, viewModel, haptic) {
                viewModel.navigateToEditor(entry.id)
            }
    ) {
        GlassmorphismBackground(modifier = Modifier.matchParentSize()) // 引入复用组件
        Text(
            text = entry.content,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
                color = getTextColor(false)
            ),
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold
        )
    }
}

// 抽取后的图片组件
@Composable
private fun ImageDiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    haptic: HapticFeedback,
    modifier: Modifier
) {
    val file = viewModel.getFullImagePath(entry.content)
    val imgWidth = if (entry.imageRatio > 1.5f || entry.isLarge) 240.dp else 120.dp

    Surface(
        modifier = modifier
            .diaryGestures(entry.id, viewModel, haptic) {
                if (viewModel.hasSpace(entry.id)) {
                    val spaceT = viewModel.spaces.find { it.entryId == entry.id }
                    viewModel.setSDestination(spaceT?.id)
                    if (spaceT?.isEncrypted == true) viewModel.showPasswordCheck =
                        true else viewModel.changeSpace()
                } else {
                    viewModel.showImage(entry)
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
                .clipToBounds()
        ) {
            CroppedDisplayImage(
                file = file,
                scaleAdjustment = if (imgWidth < 200.dp) 0.5f else 1f,
                cropParams = entry.cropParams ?: CropParams()
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopBar(
    viewModel: DailyViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    currentTheme: DailyTimeTheme,
    leftDrawerState: androidx.compose.material3.DrawerState,
    listState: LazyListState,
    currentTime: Date
) {
    val spaceColor = viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.colorBgArgb
    val finalColor =
        spaceColor?.let { Color(it).copy(alpha = 0.5f) } ?: Color.White.copy(alpha = 0.5f)

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = finalColor),
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
                        onClick = { scope.launch { listState.scrollToItem(0) } }
                    )
            ) {
                Text(
                    text = FullDateFormat.format(currentTime),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = currentTheme.onSurfaceColor.copy(alpha = 0.7f),
                    fontFamily = dymonFont
                )
                Text(
                    text = TimeFormat.format(currentTime),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    ),
                    color = currentTheme.primaryColor,
                    fontFamily = dymonFont
                )
            }
        }
    )
}
