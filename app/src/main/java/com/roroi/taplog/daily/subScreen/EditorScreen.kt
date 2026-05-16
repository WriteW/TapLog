package com.roroi.taplog.daily.subScreen

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: DailyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.editorState.collectAsState()

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var titleValue by remember(state.sessionId) {
        mutableStateOf(TextFieldValue(text = state.editingTitle, selection = TextRange(state.editingTitle.length)))
    }

    // 【修改】：判断是否为十分钟前的旧日记，是的话光标移到最前
    var textValue by remember(state.sessionId) {
        val isOld = System.currentTimeMillis() - state.timestamp > 10 * 60 * 1000 // 十分钟
        val initialSelection = if (isOld && !state.isNew) TextRange.Zero else TextRange(state.editingText.length)
        mutableStateOf(TextFieldValue(text = state.editingText, selection = initialSelection))
    }

    val defaultFontSize = 18.sp
    var fontSize by remember { mutableStateOf(defaultFontSize) }
    var showMoreMenu by remember { mutableStateOf(false) } // 【新增】更多菜单状态
    val charCount = textValue.text.length

    val handleBack = {
        if (state.isDirty) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { handleBack() }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("未保存的更改") },
            text = { Text("确定要放弃更改并退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除日记", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要彻底删除这条日记吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEditor {
                        showDeleteConfirmDialog = false
                        onBack()
                    }
                }) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    val isDark = viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.isDark ?: false
    // 采用极致的极简色彩，暗色时使用深灰，亮色时使用纯白
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val iconTint = if (isDark) Color.White else Color(0xFF333333)
    val textColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF111111)
    val titleHintColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFD4D4D4)
    val metaColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF999999)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { fontSize = defaultFontSize })
                },
                title = {},
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = iconTint)
                    }
                },
                actions = {
                    // 右侧动作栏对齐设计：无边框轻量按钮
                    if (!state.isNew) {
                        // 【修改】：将垃圾桶移入更多菜单
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More", tint = iconTint)
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("删除日记", color = Color.Red) },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteConfirmDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        viewModel.saveEditor { onBack() }
                    }) {
                        Icon(Icons.Default.Check, "Save", tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.ime)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newSize = fontSize.value * zoom
                        fontSize = newSize.coerceIn(12f, 60f).sp
                    }
                }
        ) {
            // 大字号加粗标题输入框
            BasicTextField(
                value = titleValue,
                onValueChange = {
                    titleValue = it
                    viewModel.onEditorTitleChange(it.text)
                },
                textStyle = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    fontSize = 26.sp
                ),
                cursorBrush = SolidColor(iconTint),
                decorationBox = { innerTextField ->
                    if (titleValue.text.isEmpty()) {
                        Text(
                            "Title",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = titleHintColor,
                                fontSize = 26.sp
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 精美的元数据时间统计栏
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val dateStr = remember(state.timestamp) {
                    SimpleDateFormat("MMM dd  HH:mm", Locale.ENGLISH).format(Date(state.timestamp))
                }
                Text(text = dateStr, color = metaColor, fontSize = 13.sp, fontFamily = FontFamily.Default)

                Text(text = "  |  ", color = metaColor.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = FontFamily.Default)

                Text(text = "$charCount ${if (charCount != 1) "characters" else "character"}", color = metaColor, fontSize = 13.sp, fontFamily = FontFamily.Default)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 正文长文本输入框
            BasicTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    viewModel.onEditorTextChange(it.text)
                },
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.5).sp
                ),
                cursorBrush = SolidColor(iconTint),
                decorationBox = { innerTextField ->
                    if (textValue.text.isEmpty()) {
                        Text(
                            "All returns to its source.",
                            style = TextStyle(
                                color = titleHintColor,
                                fontSize = fontSize
                            )
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}