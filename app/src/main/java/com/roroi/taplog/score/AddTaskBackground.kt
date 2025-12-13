package com.roroi.taplog.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private enum class CIAColor {
    RED, BLUE, YELLOW
}

private fun CIAColor.getColor(): Color {
    return when (this) {
        CIAColor.RED -> Color.Red
        CIAColor.YELLOW -> Color.Yellow
        CIAColor.BLUE -> Color.Blue
    }
}

private data class CircleInAdd(
    var xSpeed: Float,
    var ySpeed: Float,
    var size: Float, // 半径
    val color: CIAColor,
    var x: Float = -1f,
    var y: Float = -1f
) {
    // 假设质量与面积(或半径)成正比，这里简单用半径代表质量
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
 * 处理球体之间的碰撞检测与反弹
 */
private fun solveCollisions(circles: List<CircleInAdd>) {
    // 双重循环检测每一对圆 (避免重复检测：i 从 0 到 size, j 从 i+1 到 size)
    for (i in circles.indices) {
        for (j in (i + 1) until circles.size) {
            val c1 = circles[i]
            val c2 = circles[j]

            // 还没初始化的球不参与碰撞
            if (c1.x == -1f || c2.x == -1f) continue

            val dx = c1.x - c2.x
            val dy = c1.y - c2.y
            val distanceSq = dx * dx + dy * dy
            val radiusSum = c1.size + c2.size

            // 检测是否碰撞 (距离平方 < 半径和平方，用平方比较可以少开一次根号，性能更好)
            if (distanceSq < radiusSum * radiusSum && distanceSq > 0) {
                val distance = sqrt(distanceSq)

                // --- 1. 位置修正 (防止重叠粘连) ---
                // 计算重叠量
                val overlap = radiusSum - distance
                // 计算单位法向量
                val nx = dx / distance
                val ny = dy / distance

                // 将两个球沿法线方向推开 (各移动一半重叠距离)
                val moveX = nx * overlap * 0.5f
                val moveY = ny * overlap * 0.5f

                c1.x += moveX
                c1.y += moveY
                c2.x -= moveX
                c2.y -= moveY

                // --- 2. 动量守恒反弹计算 (弹性碰撞) ---
                // 使用一维弹性碰撞公式在法线方向上的投影

                // 相对速度在法向量上的投影
                // v_rel dot normal
                val dvx = c1.xSpeed - c2.xSpeed
                val dvy = c1.ySpeed - c2.ySpeed
                val vn = dvx * nx + dvy * ny

                // 如果球正在分离（速度方向相反），则不需要计算反弹（防止卡住时的抖动）
                if (vn > 0) continue

                // 弹性碰撞冲量公式
                // j = -(1 + e) * vn / (1/m1 + 1/m2)
                // 这里 e (恢复系数) 设为 1.0 (完全弹性碰撞)
                val impulse = -(2.0f * vn) / (1 / c1.mass + 1 / c2.mass)

                // 更新速度
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

private fun randomColor(): CIAColor {
    return CIAColor.values()[Random.nextInt(CIAColor.values().size)]
}

@Composable
fun AddTaskBackground(modifier: Modifier = Modifier) {
    // 1. 在 remember 中直接一次性生成所有圆
    val circles = remember {
        mutableListOf<CircleInAdd>().apply {
            // 这里设置生成的数量，例如 15 个
            repeat(15) {
                add(
                    CircleInAdd(
                        // 给每个人随机的速度
                        xSpeed = (Random.nextDouble() * 100 - 50).toFloat() / SCALE_V,
                        ySpeed = (Random.nextDouble() * 100 - 50).toFloat() / SCALE_V,
                        size = (Random.nextFloat() * 35f + 10f)  / SCALE_V,
                        color = randomColor()
                    )
                )
            }
        }
    }

    // 动画驱动器
    var timeState by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time -> timeState = time }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val now = timeState // 读取状态触发重绘
        val dt = 0.016f

        // 2. 遍历所有圆，进行绘制和更新
        circles.forEach { circle ->
            // --- 位置初始化逻辑 ---
            // 因为在 remember 里我们不知道屏幕多大，所以必须在这里初始化坐标
            if (circle.x == -1f) {
                // 简单的防溢出逻辑：让球出现在屏幕内部，不要贴边
                // 确保生成范围不会导致负数崩溃
                val safeWidth = (size.width - circle.size * 2).coerceAtLeast(1f)
                val safeHeight = (size.height - circle.size * 2).coerceAtLeast(1f)

                circle.x = Random.nextFloat() * safeWidth + circle.size
                circle.y = Random.nextFloat() * safeHeight + circle.size
            }

            // --- 物理更新 ---
            circle.update(dt, size.width, size.height)
        }

        // 3. 处理碰撞
        // 注意：因为是一次性生成，初始时球体很容易重叠。
        // 这里的碰撞逻辑会在第一帧把重叠的球用力“弹开”，
        // 视觉上会像“炸开”一样，这是正常且符合物理预期的。
        solveCollisions(circles)

        // 4. 绘制
        circles.forEach { circle ->
            drawCircle(
                color = circle.color.getColor(),
                radius = circle.size,
                center = Offset(circle.x, circle.y)
            )
        }
    }
}