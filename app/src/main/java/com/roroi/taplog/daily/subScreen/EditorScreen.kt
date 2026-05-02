package com.roroi.taplog.daily.subScreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures // 新增导入
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange // 新增导入
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue // 新增导入
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.daily.WarmYellowBg
import com.roroi.taplog.daily.getTextColor
import com.roroi.taplog.daily.viewmodel.DailyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: DailyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.editorState.collectAsState()

    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // ==========================================
    // 核心修复 1：使用本地的 TextFieldValue 来维护光标位置和输入法组合状态
    // ==========================================
    // 【优化】：增加 state.originalText 作为依赖，确保多次进出同一个日记时能正确刷新
    var textFieldValue by remember(state.sessionId) {
        mutableStateOf(
            TextFieldValue(
                text = state.editingText,
                selection = TextRange(state.editingText.length)
            )
        )
    }


    // ===== 字体缩放 =====
    val defaultFontSize = 18.sp
    var fontSize by remember { mutableStateOf(defaultFontSize) }

    val dynamicHorizontalPadding = remember(fontSize) {
        val minFont = 12f
        val maxFont = 60f
        val minPadding = 16f
        val maxPadding = 24f

        val fraction = (fontSize.value - minFont) / (maxFont - minFont)
        val currentPadding = maxPadding - (fraction * (maxPadding - minPadding))
        currentPadding.dp
    }

    // ===== 返回逻辑 =====
    val handleBack = {
        if (state.isDirty) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { handleBack() }

    // ===== 弹窗部分保持不变 =====
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("未保存的更改") },
            text = { Text("确定要放弃更改并退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onBack()
                }) {
                    Text("放弃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("取消")
                }
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
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    val backgroundColor = WarmYellowBg

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { /*...*/ },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveEditor {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { fontSize = defaultFontSize }
                    )
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.ime)
                // ==========================================
                // 核心修复 2：废弃 transformable，改用 detectTransformGestures
                // 这样既能双指缩放文字，又不会屏蔽 TextField 的上下滑动和光标选词能力
                // ==========================================
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newSize = fontSize.value * zoom
                        fontSize = newSize.coerceIn(12f, 60f).sp
                    }
                }
        ) {

            Spacer(modifier = Modifier.height(padding.calculateTopPadding()))

            TextField(
                // 使用修改后的本地 textFieldValue
                value = textFieldValue,
                onValueChange = { newValue ->
                    // 立即更新本地UI（解决光标乱跳和输入法漏字问题）
                    textFieldValue = newValue
                    // 异步同步到 ViewModel 以备保存
                    viewModel.onEditorTextChange(newValue.text)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dynamicHorizontalPadding,
                        end = dynamicHorizontalPadding
                    ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = backgroundColor,
                    unfocusedContainerColor = backgroundColor,
                    focusedIndicatorColor = backgroundColor,
                    unfocusedIndicatorColor = backgroundColor
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = getTextColor(
                        viewModel.getSpaceFromId(viewModel.selectedDSpaceId)?.isDark ?: false
                    ),
                    lineHeight = (fontSize.value * 1.5).sp,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                ),
                placeholder = {
                    Text(
                        "江河入海，终归源头...",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = fontSize
                    )
                }
            )
        }
    }
}