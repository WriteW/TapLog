package com.roroi.taplog.score

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.sin
import kotlin.math.PI

// =========================================================
// 第一部分：数据模型与工具
// =========================================================

/**
 * 动画状态：描述“飞行的方块”从哪里开始，飞到哪里去
 */
data class AnimState(
    val isPlaying: Boolean = false,      // 是否正在播放动画
    val startPos: Offset = Offset.Zero,  // 起飞点（屏幕坐标）
    val startSize: IntSize = IntSize.Zero, // 起飞时的尺寸
    val targetPos: Offset = Offset.Zero  // 降落点（屏幕坐标）
)

// =========================================================
// 第二部分：主界面 (UI 布局)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCartAnimationScreen() {
    // 1. 状态管理
    var animState by remember { mutableStateOf(AnimState()) }

    // 2. 坐标锚点 (用于记录起点和终点的位置)
    var rootCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
    var targetCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

    // 3. 根容器 (Box)：作为整个动画的坐标系基准
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoordinates = it } // 获取整个屏幕的坐标系
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("点击方块加入购物车") },
                    actions = {
                        // --- 目标：购物车图标 ---
                        IconButton(
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned {
                                // 记录购物车的坐标信息，作为终点
                                targetCoordinates = it
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                )
            }
        ) { padding ->
            // --- 内容区域 ---
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // --- 起点：卡片 ---
                // 用于记录卡片的坐标
                var cardCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

                // 这里的逻辑：如果动画正在播放，本体隐藏(alpha=0)，显示替身；否则显示本体。
                Card(
                    modifier = Modifier
                        .size(200.dp, 120.dp)
                        .alpha(if (animState.isPlaying) 0f else 1f) // 动画时隐身
                        .onGloballyPositioned { cardCoords = it }   // 记录起点坐标
                        .clickable {
                            // 点击触发逻辑
                            if (rootCoordinates != null && targetCoordinates != null && cardCoords != null) {
                                // 1. 计算起点的绝对坐标
                                val startPos = cardCoords!!.positionInRoot(rootCoordinates)

                                // 2. 计算终点(购物车)的中心坐标
                                val targetLoc = targetCoordinates!!.positionInRoot(rootCoordinates)
                                val targetCenter = targetLoc + Offset(
                                    targetCoordinates!!.size.width / 2f,
                                    targetCoordinates!!.size.height / 2f
                                )

                                // 3. 启动动画
                                animState = AnimState(
                                    isPlaying = true,
                                    startPos = startPos,
                                    startSize = cardCoords!!.size,
                                    targetPos = targetCenter
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("点我起飞", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        // =========================================================
        // 第三部分：动画图层 (Overlay)
        // 这是一个覆盖在所有 UI 之上的透明层，专门用来画“飞行的替身”
        // =========================================================
        if (animState.isPlaying) {
            GhostAnimationLayer(
                state = animState,
                onFinished = {
                    animState = animState.copy(isPlaying = false) // 动画结束，还原状态
                }
            )
        }
    }
}

// =========================================================
// 第四部分：动画核心逻辑 (老师教你画动画)
// =========================================================

@Composable
fun GhostAnimationLayer(state: AnimState, onFinished: () -> Unit) {
    // 1. 定义时间进度：0.0 (开始) -> 1.0 (结束)
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current // 用于像素和dp转换

    // 启动动画：800毫秒内完成
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    val p = progress.value // 当前进度 (0.0 ~ 1.0)

    // --- A. 尺寸变化计算 ---
    // 从原始大小(例如 200dp) 变成 目标极小值(20dp)
    val targetSizePx = with(density) { 20.dp.toPx() } // 最终变成一个小点

    // lerp (Linear Interpolation) 线性插值：根据进度 p，算出当前的宽高
    val curWidthPx = lerp(state.startSize.width.toFloat(), targetSizePx, p)
    val curHeightPx = lerp(state.startSize.height.toFloat(), targetSizePx, p)

    // 将像素转回 dp 供 Modifier 使用
    val curWidthDp = with(density) { curWidthPx.toDp() }
    val curHeightDp = with(density) { curHeightPx.toDp() }

    // --- B. 位置变化计算 (核心数学) ---
    // 目标位置需要减去自身大小的一半，才能让中心点对齐
    val targetX = state.targetPos.x - targetSizePx / 2
    val targetY = state.targetPos.y - targetSizePx / 2

    // 1. X轴：简单的线性移动
    val curX = lerp(state.startPos.x, targetX, p)

    // 2. Y轴：线性移动 + 抛物线偏移
    // sin(p * PI) 会在 p=0 和 p=1 时为0，在 p=0.5 时达到最大值1
    // 乘以 -300f 表示向上拱起 300 像素
    val parabolaOffset = -300f * sin(p * PI).toFloat()
    val curY = lerp(state.startPos.y, targetY, p) + parabolaOffset

    // --- C. 形状与颜色 ---
    // 形状：从圆角矩形(12dp) 变成 纯圆形(半径=宽度的一半)
    val curRadius = lerp(with(density) { 12.dp.toPx() }, curWidthPx / 2, p)
    // 颜色：从 原色 变成 红色
    val curColor = lerp(MaterialTheme.colorScheme.primaryContainer, Color.Red, p)

    // --- D. 绘制替身 ---
    Box(
        modifier = Modifier
            // 1. 移动到计算出的坐标
            .offset { IntOffset(curX.toInt(), curY.toInt()) }
            // 2. 设置当前计算出的动态大小
            .size(width = curWidthDp, height = curHeightDp)
            // 3. 旋转与裁剪
            .graphicsLayer {
                shape = RoundedCornerShape(curRadius)
                clip = true
                rotationZ = p * 720f // 旋转两圈 (360 * 2)
            }
            .background(curColor)
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAddToCartAnimationScreen() {
    AddToCartAnimationScreen()
}