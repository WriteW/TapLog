package com.roroi.taplog.daily_ai

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun DailyDynamicBackground(theme: DailyTimeTheme) {
    // 1. 最底层：主题背景色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        // 2. 优化渲染层 (完全参考提供的 OptimizedBackground 逻辑)
        Box(
            modifier = Modifier
                // 关键：将画布尺寸缩小到 1/DOWNSAMPLE_FACTOR
                .fillMaxSize(SCALE_DOWN)
                .graphicsLayer {
                    // 关键：将内容放大回全屏
                    scaleX = DOWNSAMPLE_FACTOR
                    scaleY = DOWNSAMPLE_FACTOR
                    transformOrigin = TransformOrigin(0f, 0f) // 从左上角放大

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // 动态计算模糊半径
                        val optimizedRadius = max(15f, 100f / DOWNSAMPLE_FACTOR)
                        renderEffect = RenderEffect
                            .createBlurEffect(optimizedRadius, optimizedRadius, Shader.TileMode.MIRROR)
                            .asComposeRenderEffect()
                    }
                }
        ) {
            // 3. 实际绘制层 (在缩小的画布上绘制)
            PhysicsBallsCanvas(theme)
        }
    }
}

@Composable
private fun PhysicsBallsCanvas(theme: DailyTimeTheme) {
    // 初始化小球
    val circles = remember(theme) {
        mutableListOf<ThemeBall>().apply {
            // 自动计算大小：基准大小 / sqrt(数量比)
            // 数量越多，球越小，保证总密度一致
            val baseSize = 35f
            val sizeFactor = sqrt(15f / BALL_COUNT) // 15是参考基准数量

            repeat(BALL_COUNT) {
                // 随机大小范围
                val rawSize = (Random.nextFloat() * baseSize + 10f) * sizeFactor

                add(
                    ThemeBall(
                        // 速度除以 SCALE_V (适配缩小后的世界)
                        xSpeed = (Random.nextDouble() * 100 - 50).toFloat() / (SCALE_V * 2.2f),
                        ySpeed = (Random.nextDouble() * 100 - 50).toFloat() / (SCALE_V * 2.2f),
                        // 大小除以 SCALE_V
                        size = rawSize / SCALE_V,
                        // 颜色：从主题中随机
                        color = theme.ballColors.random()
                    )
                )
            }
        }
    }

    // 动画驱动
    var timeState by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time -> timeState = time }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val now = timeState
        val dt = 0.016f

        // 因为外层用了 fillMaxSize(SCALE_DOWN)，这里的 size 已经是极小的微观尺寸了
        // 比如屏幕 1080px，这里可能只有 120px
        // 物理更新直接基于这个微观尺寸进行，完全不需要手动转换坐标

        circles.forEach { circle ->
            // --- 位置初始化 ---
            if (circle.x == -1f) {
                // 在微观世界中初始化坐标
                val safeWidth = (size.width - circle.size * 2).coerceAtLeast(1f)
                val safeHeight = (size.height - circle.size * 2).coerceAtLeast(1f)

                circle.x = Random.nextFloat() * safeWidth + circle.size
                circle.y = Random.nextFloat() * safeHeight + circle.size
            }

            // --- 物理更新 ---
            circle.update(dt, size.width, size.height)
        }

        // --- 碰撞处理 ---
        solveCollisions(circles)

        // --- 绘制 ---
        circles.forEach { circle ->
            drawCircle(
                color = circle.color,
                radius = circle.size,
                center = Offset(circle.x, circle.y)
            )
        }
    }
}