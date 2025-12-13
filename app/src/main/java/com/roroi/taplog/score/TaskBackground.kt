package com.roroi.taplog.score

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlin.math.PI

val Float.rad: Float
    get() = this * (PI.toFloat() / 180f)



@Composable
fun PlanetBackground(modifier: Modifier = Modifier) {
    // 记录上一帧的时间
    var lastTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val currentTimeNanos = awaitFrame()

            // 如果是第一帧，先初始化 lastTime
            if (lastTimeNanos == 0L) {
                lastTimeNanos = currentTimeNanos
            }

            // 计算时间差 (单位：秒)
            val dt = (currentTimeNanos - lastTimeNanos) / 1_000_000_000f
            lastTimeNanos = currentTimeNanos
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        lastTimeNanos
    }
}