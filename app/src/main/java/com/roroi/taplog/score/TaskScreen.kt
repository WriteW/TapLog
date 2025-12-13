package com.roroi.taplog.score

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 移除原本的固定高度常量，改用 Grid 的宽高比控制

@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    task: Task,
    onTaskClick: (Task) -> Unit,
    onLongClick: (Task) -> Unit
) {
    val themeColor = task.difficulty.getColor()

    // 构建渐变背景：从主题色到稍浅/稍深的颜色，增加质感
    val gradientBrush = remember(themeColor) {
        Brush.linearGradient(
            colors = listOf(
                themeColor,
                themeColor.copy(alpha = 0.6f) // 或者混合一点白色/黑色
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // 关键：强制 1:1 正方形
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() }, // 1. 必需的状态源
                indication = null, // 2. 设置为 null 去除波纹
                onClick = { onTaskClick(task) },
                onLongClick = { onLongClick(task) } // 触发长按
            ),
        shape = RoundedCornerShape(24.dp), // 更圆润的角，像 iOS Widget
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), // 背景由 Box 处理
        elevation = CardDefaults.cardElevation(0.dp) // 扁平化，去除阴影（或保留微弱阴影）
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 纯色渐变模式
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
            )


            // --- 层级 2: 右上角积分 ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50)) // 胶囊形状
                    .background(Color.White.copy(alpha = 0.25f)) // 磨砂玻璃感背景
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${task.income}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // --- 层级 3: 左下角内容 (标题 + 进度) ---
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                if (task.description.isNotBlank()) {
                    // 如果没有进度条，显示简短描述，或者留白保持简洁
                    Text(
                        text = task.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onTaskClick: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    topBar: @Composable () -> Unit,
) {
    // 状态：当前准备删除哪个任务？如果是 null 代表没有弹窗
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val context = LocalContext.current

    // 删除确认弹窗
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("删除任务") },
            text = { Text("确定要删除 \"${taskToDelete?.title}\" 吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { onTaskDelete(it) }
                        taskToDelete = null
                    }
                ) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("取消") }
            }
        )
    }
    Scaffold(topBar = topBar) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                // 底部留出足够空间，避免被系统手势或不存在的导航栏遮挡
                bottom = 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6F8))
        ) {
            items(GlobalV.taskList) { task ->
                TaskCard(
                    task = task,
                    onTaskClick = onTaskClick,
                    onLongClick = {
                        // 长按时，震动一下提升手感
                        performRichHaptics(context, HapticType.FAILURE) // 借用一下Heavy Click震动
                        taskToDelete = task // 弹出对话框
                    }
                )
            }
        }
    }

}