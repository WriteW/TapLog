package com.roroi.taplog.daily

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
import com.roroi.taplog.daily.viewmodel.BALL_COUNT
import com.roroi.taplog.daily.viewmodel.DOWNSAMPLE_FACTOR
import com.roroi.taplog.daily.viewmodel.SCALE_DOWN
import com.roroi.taplog.daily.viewmodel.SCALE_V
import com.roroi.taplog.daily.viewmodel.ThemeBall
import com.roroi.taplog.daily.viewmodel.solveCollisions
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
    val circles = remember(theme) {
        mutableStateListOf<ThemeBall>().apply {
            val baseSize = 35f
            val sizeFactor = sqrt(15f / BALL_COUNT)
            repeat(BALL_COUNT) {
                val rawSize = (Random.nextFloat() * baseSize + 10f) * sizeFactor
                add(
                    ThemeBall(
                        xSpeed = (Random.nextDouble() * 100 - 50).toFloat() / (SCALE_V * 2.2f),
                        ySpeed = (Random.nextDouble() * 100 - 50).toFloat() / (SCALE_V * 2.2f),
                        size = rawSize / SCALE_V,
                        color = theme.ballColors.random()
                    )
                )
            }
        }
    }

    // 强制重绘的触发器
    var redrawTrigger by remember { mutableIntStateOf(0) }

    // 动画循环
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { } // 等待下一帧
            redrawTrigger++      // 触发重组
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 读取 redrawTrigger 只是为了建立依赖，实际值不重要
        redrawTrigger

        val dt = 0.016f

        // 初始化位置
        circles.forEach { circle ->
            if (circle.x == -1f) {
                val safeWidth = (size.width - circle.size * 2).coerceAtLeast(1f)
                val safeHeight = (size.height - circle.size * 2).coerceAtLeast(1f)
                circle.x = Random.nextFloat() * safeWidth + circle.size
                circle.y = Random.nextFloat() * safeHeight + circle.size
            }
        }

        // 物理更新
        circles.forEach { circle ->
            circle.update(dt, size.width, size.height)
        }
        solveCollisions(circles)

        // 绘制
        circles.forEach { circle ->
            drawCircle(
                color = circle.color,
                radius = circle.size,
                center = Offset(circle.x, circle.y)
            )
        }
    }
}