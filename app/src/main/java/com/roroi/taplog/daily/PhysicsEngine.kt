package com.roroi.taplog.daily

import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

// --- 核心优化常量 (完全参考您的要求) ---
const val BALL_COUNT = 15 // 您可以在这里修改小球数量
const val SCALE_V = 1.7f
const val DOWNSAMPLE_FACTOR = 5f * SCALE_V
const val SCALE_DOWN = 1f / DOWNSAMPLE_FACTOR

/**
 * 对应参考代码中的 CircleInAdd
 */
data class ThemeBall(
    var xSpeed: Float,
    var ySpeed: Float,
    var size: Float, // 半径
    val color: Color,
    var x: Float = -1f,
    var y: Float = -1f
) {
    val mass: Float get() = size

    fun update(dt: Float, maxWidth: Float, maxHeight: Float) {
        if (x == -1f || y == -1f) return

        x += xSpeed * dt
        y += ySpeed * dt

        // 左右边界反弹
        if (x - size < 0f) {
            x = size
            xSpeed = -xSpeed
        } else if (x + size > maxWidth) {
            x = maxWidth - size
            xSpeed = -xSpeed
        }

        // 上下边界反弹
        if (y - size < 0f) {
            y = size
            ySpeed = -ySpeed
        } else if (y + size > maxHeight) {
            y = maxHeight - size
            ySpeed = -ySpeed
        }
    }
}

/**
 * 碰撞检测逻辑 (完全复刻)
 */
fun solveCollisions(circles: List<ThemeBall>) {
    for (i in circles.indices) {
        for (j in (i + 1) until circles.size) {
            val c1 = circles[i]
            val c2 = circles[j]

            if (c1.x == -1f || c2.x == -1f) continue

            val dx = c1.x - c2.x
            val dy = c1.y - c2.y
            val distanceSq = dx * dx + dy * dy
            val radiusSum = c1.size + c2.size

            if (distanceSq < radiusSum * radiusSum && distanceSq > 0) {
                val distance = sqrt(distanceSq)

                // 1. 位置修正
                val overlap = radiusSum - distance
                val nx = dx / distance
                val ny = dy / distance

                val moveX = nx * overlap * 0.5f
                val moveY = ny * overlap * 0.5f

                c1.x += moveX
                c1.y += moveY
                c2.x -= moveX
                c2.y -= moveY

                // 2. 动量守恒反弹
                val dvx = c1.xSpeed - c2.xSpeed
                val dvy = c1.ySpeed - c2.ySpeed
                val vn = dvx * nx + dvy * ny

                if (vn > 0) continue

                val impulse = -(2.0f * vn) / (1 / c1.mass + 1 / c2.mass)
                val impulseX = nx * impulse
                val impulseY = ny * impulse

                c1.xSpeed += impulseX / c1.mass
                c1.ySpeed += impulseY / c1.mass
                c2.xSpeed -= impulseX / c2.mass
                c2.ySpeed -= impulseY / c2.mass
            }
        }
    }
}