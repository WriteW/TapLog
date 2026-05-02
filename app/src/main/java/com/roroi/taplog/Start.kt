package com.roroi.taplog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.ui.theme.TapLogTheme

class Start : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapLogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartP(innerPadding)
                }
            }
        }
    }
}

@Composable
fun StartP(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    // 根据 startAnimation 来决定动画目标值
    val displayOffset by animateFloatAsState(
        targetValue = if (startAnimation) 200f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
    )

    // 进入界面后才触发动画
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1F))
        Box(
            modifier = Modifier
                .size(270.dp)
                .drawBehind {
                    val outerRadius = size.minDimension
                    val innerRadius = 90.dp.toPx()

                    drawCircle(
                        brush = glowRadialGradient(
                            outerRadius = outerRadius,
                            innerRadius = innerRadius,
                            glowColor = Color(95, 205, 228)
                        ),
                        radius = outerRadius,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon2),
                    contentDescription = "图标",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
        val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

        // 1. 外层 Box：专门用来画光晕（尺寸比 64dp 大，留出光晕扩散的空间）
        Box(
            modifier = Modifier
                .size(120.dp) // 给光晕足够的展示空间
                .drawBehind {
                    val outerRadius = size.minDimension / 1.5f // 外发光边界：60dp
                    val innerRadius = 32.dp.toPx() // 内发光边界：正好贴合按钮的边缘 (64 / 2)

                    drawCircle(
                        brush = glowRadialGradient(
                            outerRadius = outerRadius,
                            innerRadius = innerRadius,
                            glowColor = primaryContainerColor
                        ),
                        radius = outerRadius,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 2. 内层 Box：按钮本体
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape) // 只裁剪按钮本身
                    .background(primaryContainerColor) // 按钮的纯色背景
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val intent = Intent(context, Select::class.java)
                        context.startActivity(intent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "enter",
                    // 建议给 Icon 加个对比色，防止它和背景色混在一起看不清
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.weight(1F))
        Text(
            text = "made by roRoi",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "软件图标chatGPT画的",
            fontSize = 20.sp,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .offset(0.dp, displayOffset.dp),
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = godColor
        )
    }
}

fun glowRadialGradient(
    outerRadius: Float,
    innerRadius: Float,
    glowColor: Color
): Brush {

    val stop = innerRadius / outerRadius

    return Brush.radialGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            stop to Color.Transparent,
            stop to glowColor,
            1f to glowColor.copy(alpha = 0f)
        ),
        radius = outerRadius
    )
}

@Preview(showBackground = true)
@Composable
fun SPr() {
    TapLogTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            StartP(innerPadding)
        }
    }
}