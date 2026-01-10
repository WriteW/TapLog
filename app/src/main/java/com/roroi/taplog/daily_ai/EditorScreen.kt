package com.roroi.taplog.daily_ai

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    entryId: String?,
    viewModel: DailyViewModel,
    onBack: () -> Unit
) {
    // 查找现有条目
    val existingEntry = remember(entryId) {
        viewModel.groupedEntries.value.flatMap { it.items }.find { it.id == entryId }
    }

    var textContent by remember { mutableStateOf(existingEntry?.content ?: "") }

    // 弹窗状态
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 检查是否有修改
    val isDirty = if (existingEntry != null) {
        textContent != existingEntry.content
    } else {
        textContent.isNotEmpty()
    }

    // 处理返回逻辑
    val handleBack = {
        if (isDirty) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { handleBack() }

    // 1. 未保存更改的确认弹窗
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

    // 2. 删除确认弹窗
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除日记", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("确定要彻底删除这条日记吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    if (existingEntry != null) {
                        viewModel.deleteEntry(existingEntry.id) // 传递 ID
                        showDeleteConfirmDialog = false
                        onBack()
                    }
                }) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        containerColor = WarmYellowBg,
        topBar = {
            CenterAlignedTopAppBar( // 改用 CenterAligned 以便让 Title 居中
                title = {
                    // 需求：删除键放中间
                    if (existingEntry != null) {
                        FilledTonalIconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.5f),
                                contentColor = Color.Red.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 保存按钮在右侧
                    IconButton(onClick = {
                        if (textContent.isNotBlank()) {
                            if (existingEntry != null) {
                                viewModel.updateEntry(existingEntry.copy(content = textContent))
                            } else {
                                viewModel.addTextEntry(textContent)
                            }
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = WarmYellowBg, scrolledContainerColor = WarmYellowBg )
            )
        }
    ) { padding ->
        // 暖黄色背景编辑区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.ime) // 核心：确保 Box 的 padding 包含输入法高度
        ) {
            Spacer(modifier = Modifier.height(padding.calculateTopPadding() / 1.5f + 4.dp) )
            TextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextColor,
                    lineHeight = 28.sp, // 增加行高，提升阅读体验
                    fontSize = 18.sp
                ),
                placeholder = {
                    Text(
                        "江河入海，终归源头...",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 18.sp
                    )
                }
            )
        }
    }
}