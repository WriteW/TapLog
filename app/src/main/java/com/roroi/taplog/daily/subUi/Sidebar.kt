package com.roroi.taplog.daily.subUi

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.roroi.taplog.Tap
import com.roroi.taplog.daily.CroppedDisplayImage
import com.roroi.taplog.daily.DailyTimeTheme
import com.roroi.taplog.daily.darken
import com.roroi.taplog.daily.getTextColor
import com.roroi.taplog.daily.soBiscuitFont
import com.roroi.taplog.daily.subScreen.NameSpace
import com.roroi.taplog.daily.subScreen.ThemeEditorCircle
import com.roroi.taplog.daily.viewmodel.CropParams
import com.roroi.taplog.daily.viewmodel.DailyEntry
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.daily.viewmodel.EntryType
import com.roroi.taplog.daily.viewmodel.TimelineGroup
import com.roroi.taplog.daily.viewmodel.getDotColor
import com.roroi.taplog.score.Score
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close

data class NavModuleItem(
    val name: String,
    val cls: Class<*>
)

val otherAppModules = listOf(
    NavModuleItem("Tap", Tap::class.java),
    NavModuleItem("Log", com.roroi.taplog.Log::class.java),
    NavModuleItem("Stream", com.roroi.taplog.stream.MainActivity::class.java),
    NavModuleItem("Score", Score::class.java) // 假设你的 Score 叫这个名字，按需修改
)

@Composable
fun TimeCapsuleItem(viewModel: DailyViewModel?, onCloseSidebar: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val theme = viewModel?.getThemeBySpace() ?: DailyTimeTheme.getCurrent()
    val hasUnlocked =
        viewModel?.timeCapsules?.any { it.openAt <= System.currentTimeMillis() && !it.isViewed } == true

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 28.dp, end = 24.dp, bottom = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Time Capsule", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (hasUnlocked && !expanded) { // 主标题红点
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = theme.onSurfaceColor
            )
        }

        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column {
                NavigationDrawerItem(
                    label = { Text("Add", color = theme.onSurfaceColor) },
                    onClick = {
                        onCloseSidebar()
                        viewModel?.navigateToAddCapsule()
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    selected = false, modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View", color = theme.onSurfaceColor)
                            if (hasUnlocked) { // 展开后 View 上的红点
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                            }
                        }
                    },
                    onClick = {
                        onCloseSidebar()
                        viewModel?.navigateToViewCapsules()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ViewList, null) },
                    selected = false, modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

// ============== 修改：带展开功能的 Danger Zone ==============
@Composable
fun LeftSidebarDangerZone(viewModel: DailyViewModel?, onClear: () -> Unit) {
    // 控制是否展开状态，默认 false 收起，防止误触
    var expanded by remember { mutableStateOf(false) }
    val theme = viewModel?.getThemeBySpace() ?: DailyTimeTheme.getCurrent()

    Column {
        // 点击整行都可以切换展开状态
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 28.dp, end = 24.dp, bottom = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Danger Zone",
                color = Color.Red.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand Danger Zone",
                tint = Color.Red.copy(alpha = 0.8f)
            )
        }

        // 动画展开/收起具体内容
        AnimatedVisibility(visible = expanded) {
            NavigationDrawerItem(
                label = {
                    Text("Clear All Data", color = theme.onSurfaceColor)
                },
                selected = false,
                onClick = onClear,
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = theme.onSurfaceColor
                    )
                },
                modifier = Modifier.padding(horizontal = 12.dp) // 让子项往里缩一点更好看
            )
        }
    }
}

// ============== 新增：带展开功能的 Others 模块跳转区 ==============
@Composable
fun LeftSidebarOthers(theme: DailyTimeTheme) {
    // 默认收起或展开取决于你的需求，这里默认收起
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 28.dp, end = 24.dp, bottom = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Others",
                color = theme.onSurfaceColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand Others",
                tint = theme.onSurfaceColor
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                // 遍历刚才定义的 List 自动生成按钮
                otherAppModules.forEach { item ->
                    NavigationDrawerItem(
                        label = {
                            Text(item.name, color = theme.onSurfaceColor)
                        },
                        selected = false,
                        onClick = {
                            // 触发跳转
                            val intent = Intent(context, item.cls)
                            context.startActivity(intent)
                        },
                        icon = {
                            // 使用统一的退出小图标
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = theme.onSurfaceColor
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

// 左侧栏预览
@Preview
@Composable
fun LSPre() {
    LeftSidebarContent(DailyTimeTheme.AFTERNOON, null, {}, {}, {}, {}, null)
}

// 左侧边栏的分裂
// 左侧边栏
@Composable
fun LeftSidebarContent(
    theme: DailyTimeTheme,
    hazeState: HazeState?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    onCloseSidebar: () -> Unit,
    viewModel: DailyViewModel?
) {
    var clearConfirmCount by remember { mutableIntStateOf(0) }

    if (clearConfirmCount > 0) {
        ClearConfirmationDialog(clearConfirmCount, onClear) { clearConfirmCount = it }
    }

    var hazeModifier: Modifier = Modifier
    hazeModifier = if (hazeState != null) {
        hazeModifier.hazeChild(state = hazeState)
    } else {
        hazeModifier.background(theme.backgroundColor)
    }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = Color.Transparent,
        drawerContentColor = theme.onSurfaceColor,
        modifier = hazeModifier
            .width(300.dp)
    ) {

        LeftSidebarHeader(theme)
        LeftSidebarActions(viewModel, onExport, onImport)
        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp, horizontal = 28.dp))

        if (viewModel == null || viewModel.getSpaceFromId(viewModel.selectedDSpaceId) != null) {
            LeftSideSpaceEditor(viewModel)
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp, horizontal = 28.dp))
        }

        // 时间胶囊
        TimeCapsuleItem(viewModel = viewModel, onCloseSidebar = onCloseSidebar)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 28.dp))
        // 挂载 Others 栏
        LeftSidebarOthers(theme = theme)
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 28.dp))

        // 挂载 Danger Zone 栏
        LeftSidebarDangerZone(viewModel = viewModel) { clearConfirmCount = 1 }
    }
}

@Composable
fun LeftSideSpaceEditor(viewModel: DailyViewModel?) {
    val currentSpace = viewModel?.getSpaceFromId(viewModel.selectedDSpaceId)

    NavigationDrawerItem(
        label = {
            Text(
                "Change Password",
                color = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
        selected = false,
        onClick = {
            viewModel?.showChangePassword = true
        },
        icon = {
            Icon(
                Icons.Default.Password,
                "change password",
                tint = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .height(56.dp)
            .padding(start = 16.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val height = 40.dp
        ThemeEditorCircle(
            space = viewModel?.getSpaceFromId(viewModel.selectedDSpaceId),
            height = height,
            modifier = Modifier.zIndex(1f)
        ) { newTheme ->
            currentSpace?.let {
                viewModel.changeSpaceP(
                    it.copy(
                        colorBgArgb = newTheme.backgroundColor.toArgb(),
                        colorBallArgb = newTheme.primaryColor.toArgb(),
                        isDark = newTheme.isDark
                    )
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        NameSpace(currentSpace?.name ?: "new Space", height = height) { newName ->
            currentSpace?.let {
                viewModel.changeSpaceP(
                    it.copy(
                        name = newName
                    )
                )
            }
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}

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
        color = theme.primaryColor,
        fontFamily = soBiscuitFont
    )
}

@Composable
fun LeftSidebarActions(viewModel: DailyViewModel?, onExport: () -> Unit, onImport: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Text(
                "Export Data (ZIP)",
                color = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
        selected = false,
        onClick = onExport,
        icon = {
            Icon(
                Icons.Default.Output,
                null,
                tint = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
    )
    NavigationDrawerItem(
        label = {
            Text(
                "Import Data (ZIP)",
                color = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
        selected = false,
        onClick = onImport,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.Input,
                null,
                tint = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        })
    NavigationDrawerItem(
        label = {
            Text(
                "Batch Manage",
                color = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
        selected = false,
        onClick = {
            viewModel?.startBatchSelecting()
        },
        icon = {
            Icon(
                Icons.Default.Menu,
                "batch manage",
                tint = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        }
    )
    if (viewModel != null) {
        val theme = viewModel.getThemeBySpace()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.onSurfaceColor.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = theme.onSurfaceColor.copy(alpha = 0.6f)
            )
            BasicTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textStyle = TextStyle(color = theme.onSurfaceColor, fontSize = 16.sp)
            )
            if (viewModel.searchQuery.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = theme.onSurfaceColor.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { viewModel.updateSearch("") }
                )
            }
        }
    }
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
    val startIndices = remember(groups) {
        val list = mutableListOf<Int>()
        var acc = 0
        for (g in groups) {
            list.add(acc)
            acc += g.items.size
        }
        list
    }
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
                    fontFamily = soBiscuitFont,
                    color = theme.primaryColor,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 这里的点击不要波纹，因为是背景线
                        ) {
                            scope.launch {
                                if (groups.isNotEmpty()) {
                                    // 滚动到最后一项
                                    listState.scrollToItem(groups.size - 1)
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
                    Log.d(
                        "the dog is a lie",
                        "index: $index ;isPreSizeEven: ${groups.first() != group && groups[index - 1].items.size % 2 == 0}"
                    )
                    RightSidebarGroupItem(
                        startIndex = startIndices[index],
                        index = index, // 传入索引
                        group = group,
                        viewModel = viewModel,
                        theme = theme,
                        onTimeClick = { onJumpToGroup(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun RightSidebarGroupItem(
    startIndex: Int,
    index: Int,
    group: TimelineGroup,
    viewModel: DailyViewModel,
    theme: DailyTimeTheme,
    onTimeClick: () -> Unit
) {
    val dateObj = Date(group.timestamp)
    val timeStr = remember(group.timestamp) { com.roroi.taplog.daily.TimeFormat.format(dateObj) }
    val dateStr = remember(group.timestamp) { com.roroi.taplog.daily.DateFormat.format(dateObj) }

    val dotColor = viewModel.getTimelineColor(group.timestamp)
    val entries = group.items

    // 偶数行(0,2,4) -> 内容在右，时间在左
    // 奇数行(1,3,5) -> 内容在左，时间在右
    val isFirstEntryRight = startIndex % 2 == 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0xFFE0E0E0),
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }) {

        // --- 首行排版 ---
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            if (isFirstEntryRight) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    TimeLineTime(timeStr, dateStr, theme, Alignment.End, onTimeClick)
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (entries.isNotEmpty()) CompactDiaryCard(entries[0], viewModel)
                }
            }
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(
                        group.getDotColor(),
                        size.minDimension / 2 + 2.dp.toPx()
                    ); drawCircle(dotColor, size.minDimension / 2)
                }
            }
            if (isFirstEntryRight) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (entries.isNotEmpty()) CompactDiaryCard(entries[0], viewModel)
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    TimeLineTime(timeStr, dateStr, theme, Alignment.Start, onTimeClick)
                }
            }
        }

        // --- 组内后续元素排版 ---
        if (entries.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            entries.drop(1).forEachIndexed { subIndex, entry ->
                // 计算当前这条在整个大盘里的绝对索引，偶数放右边，奇数放左边
                val currentFlatIndex = startIndex + subIndex + 1
                val isCurrentSubItemRight = currentFlatIndex % 2 == 0

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)) {
                    if (!isCurrentSubItemRight) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) { CompactDiaryCard(entry, viewModel) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) { CompactDiaryCard(entry, viewModel) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TimeLineTime(
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

// Entries in Timeline
@Composable
fun CompactDiaryCard(
    entry: DailyEntry,
    viewModel: DailyViewModel
) {
    // 侧边栏的多态路由控制
    when (entry.type) {
        EntryType.TEXT -> CompactTextCard(entry, viewModel)
        EntryType.IMAGE -> CompactImageCard(entry, viewModel)
    }
}

@Composable
private fun CompactTextCard(entry: DailyEntry, viewModel: DailyViewModel) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { viewModel.navigateToEditor(entry.id) }
    ) {
        com.roroi.taplog.daily.GlassmorphismBackground(modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.padding(12.dp)) {
            // 标题渲染
            if (!entry.title.isNullOrBlank()) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = getTextColor(false),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = if (!entry.title.isNullOrBlank()) 3 else 4,
                overflow = TextOverflow.Ellipsis,
                color = getTextColor(false).copy(alpha = if (entry.title.isNullOrBlank()) 1f else 0.8f),
                fontWeight = if (entry.title.isNullOrBlank()) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CompactImageCard(entry: DailyEntry, viewModel: DailyViewModel) {
    val file = viewModel.getFullImagePath(entry.content)
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        color = Color.White,
        modifier = Modifier.clickable { viewModel.showImage(entry) }
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(entry.imageRatio)
                .clipToBounds()
        ) {
            CroppedDisplayImage(
                file = file,
                scaleAdjustment = 0.5f,
                cropParams = entry.cropParams ?: CropParams()
            )
        }
    }
}