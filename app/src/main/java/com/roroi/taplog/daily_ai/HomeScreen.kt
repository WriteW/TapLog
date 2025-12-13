package com.roroi.taplog.daily_ai

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 常量定义
private val TIMELINE_WIDTH = 88.dp
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

    // 侧边栏状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
        drawerContent = {
            SidebarContent(
                onExport = {
                    // 生成默认文件名：daily_backup_时间戳.zip
                    val fileName = "daily_backup_${System.currentTimeMillis()}.zip"
                    exportLauncher.launch(fileName)
                    scope.launch { drawerState.close() }
                },
                onImport = {
                    importLauncher.launch(arrayOf("application/zip"))
                    scope.launch { drawerState.close() }
                },
                onClear = {
                    viewModel.clearAllData()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        var showAddDialog by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = Color(0xFFF5F5F5),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault()).format(currentTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(currentTime),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
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
                        .width(TIMELINE_WIDTH)
                        .align(Alignment.TopStart)
                ) {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
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

@Composable
fun TimelineRow(
    group: TimelineGroup,
    viewModel: DailyViewModel,
    onEdit: (String) -> Unit,
    onImageClick: (DailyEntry) -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(group.timestamp))

    // 动态计算的颜色
    val dynamicColor = viewModel.getTimelineColor(group.timestamp)
    val dotStrokeColor = Color.White

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(TIMELINE_WIDTH),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = dynamicColor.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = CARD_TOP_OFFSET - 3.dp)
                    .fillMaxWidth()
                    .padding(end = (TIMELINE_WIDTH / 2) + 7.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(DOT_SIZE)
                    .offset(y = CARD_TOP_OFFSET)
            ) {
                drawCircle(color = dotStrokeColor, radius = size.minDimension / 2 + 2.dp.toPx())
                drawCircle(color = dynamicColor, radius = size.minDimension / 2)
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Diary Image",
                contentScale = ContentScale.Crop, // 列表页用 Crop 铺满框即可，原图已在 Selection 中“伪裁切”
                modifier = Modifier
                    .width(imgWidth)
                    .aspectRatio(entry.imageRatio) // 2*2时 ratio是1f，所以高度 = 宽度 = 240dp
            )
        }
    }
}

// 侧边栏内容
@Composable
fun SidebarContent(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit
) {
    // 清空确认逻辑
    var clearConfirmCount by remember { mutableIntStateOf(0) }

    if (clearConfirmCount > 0) {
        val (title, text) = when(clearConfirmCount) {
            1 -> "Delete All Data?" to "This will delete all your entries and photos. Are you sure?"
            2 -> "Seriously?" to "This cannot be undone. All data will be lost forever."
            3 -> "Final Warning" to "Press confirm to WIPE EVERYTHING."
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
            }
        )
    }

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerContentColor = TextColor
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Data Management",
            modifier = Modifier.padding(start = 24.dp, bottom = 24.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        NavigationDrawerItem(
            label = { Text("Export Data (ZIP)") },
            selected = false,
            onClick = onExport,
            icon = { Icon(Icons.Default.Share, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        NavigationDrawerItem(
            label = { Text("Import Data (ZIP)") },
            selected = false,
            onClick = onImport,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.rotate(90f)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        Text("Danger Zone", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray)

        NavigationDrawerItem(
            label = { Text("Clear All Data", color = Color.Red) },
            selected = false,
            onClick = { clearConfirmCount = 1 },
            icon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(
                    onClick = onDelete,
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
                fun OptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
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