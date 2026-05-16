package com.roroi.taplog.daily.subUi

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.roroi.taplog.daily.CroppedDisplayImage
import com.roroi.taplog.daily.DailyTimeTheme
import com.roroi.taplog.daily.cardTransparentScale
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 左侧栏预览
@Preview
@Composable
fun LSPre() {
    LeftSidebarContent(DailyTimeTheme.AFTERNOON, null, {}, {}, {}, null)
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
}

@Composable
fun LeftSidebarDangerZone(viewModel: DailyViewModel?, onClear: () -> Unit) {
    Text("Danger Zone", modifier = Modifier.padding(start = 28.dp, bottom = 12.dp))
    NavigationDrawerItem(
        label = {
            Text(
                text = "Clear All Data",
                color = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        },
        selected = false,
        onClick = onClear,
        icon = {
            Icon(
                Icons.Default.Delete,
                null,
                tint = viewModel?.getThemeBySpace()?.onSurfaceColor ?: Color.Gray
            )
        })
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
                var isPSECount = 0
                itemsIndexed(groups) { index, group ->
                    val isPreSizeEven = groups.first() != group && groups[index - 1].items.size % 2 == 0
                    if (isPreSizeEven) isPSECount += 1

                    Log.d("the dog is a lie", "index: $index ;isPreSizeEven: ${groups.first() != group && groups[index - 1].items.size % 2 == 0}")
                    RightSidebarGroupItem(
                        index = index, // 传入索引
                        isReverseSide = isPSECount % 2 != 0, // 如果为奇数
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
    index: Int,
    isReverseSide: Boolean,// 上一个entries的size是否为偶数，如果是则反转初始isRight
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
    val isFirstEntryRight = (index % 2 == 0) xor isReverseSide

    Column(modifier = Modifier.fillMaxWidth().drawBehind {
        // 画贯穿整个组的中轴线
        drawLine(
            color = Color(0xFFE0E0E0),
            start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }) {

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
                    TimeLineTime(
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
                        .background(Color.Transparent)
                        .padding(end = 12.dp), contentAlignment = Alignment.TopEnd
                ) {
                    if (entries.isNotEmpty()) CompactDiaryCard(
                        entries[0],
                        viewModel
                    )
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
                        color = group.getDotColor(),
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
                    if (entries.isNotEmpty()) CompactDiaryCard(
                        entries[0],
                        viewModel
                    )
                }
            } else {
                // B情况：时间在右 (靠左对齐，贴近中线)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp), contentAlignment = Alignment.TopStart
                ) {
                    TimeLineTime(
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
        com.roroi.taplog.daily.GlassmorphismBackground(modifier = Modifier.matchParentSize()) // 引入复用组件
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
        Box(modifier = Modifier.width(100.dp).aspectRatio(entry.imageRatio).clipToBounds()) {
            CroppedDisplayImage(
                file = file,
                scaleAdjustment = 0.5f,
                cropParams = entry.cropParams ?: CropParams()
            )
        }
    }
}