package com.roroi.taplog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.roroi.taplog.daily.DailyActivity
import com.roroi.taplog.score.Score
import com.roroi.taplog.score_ai.ScoreAIActivity
import com.roroi.taplog.ui.theme.TapLogTheme

data class CardItem(val image: Int, val title: String, val cls: Class<*>)

// 常用列表
val mainList = listOf(
    CardItem(R.drawable.tap_cover, "Tap", Tap::class.java),
    CardItem(R.drawable.log_cover, "Log", Log::class.java),
    CardItem(R.drawable.isokay_cover, "Is it okay?", IsOkay::class.java),
    CardItem(R.drawable.daily_cover, "Daily", DailyActivity::class.java)
)

// 不常用列表 (Score 和 score_ai 移到这里)
val uncommonList = listOf(
    CardItem(R.drawable.score_cover, "Score", Score::class.java),
    CardItem(R.drawable.score_ai_cover, "score_ai", ScoreAIActivity::class.java)
)

class Select : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapLogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SelectP(innerPadding)
                }
            }
        }
    }
}

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectP(innerPadding: PaddingValues) {
    val context = LocalContext.current

    // 动漫量
    var itemAnim by remember { mutableStateOf(false) }
    val leftToRight by animateFloatAsState(
        targetValue = if (itemAnim) 0f else -(LocalConfiguration.current.screenWidthDp.toFloat()),
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "slideIn"
    )

    // 进入界面后才触发动画
    LaunchedEffect(Unit) {
        itemAnim = true
    }

    Column {
        LazyColumn(
            modifier = Modifier
                .offset(leftToRight.dp, 0.dp)
                .weight(10f)
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 第一部分：常用功能
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    mainList.forEach { item ->
                        MenuCard(item = item, context = context)
                    }
                }
            }

            // 分割线：不常用
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "不常用",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // 第二部分：不常用功能
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    uncommonList.forEach { item ->
                        MenuCard(item = item, context = context)
                    }
                }
            }
        }
    }
}

// 提取出来的卡片组件，保持代码整洁
@Composable
fun MenuCard(item: CardItem, context: Context) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .border(4.dp, MaterialTheme.colorScheme.primary)
            .clickable {
                val intent = Intent(context, item.cls)
                context.startActivity(intent)
            }
    ) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 150.dp)
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            modifier = Modifier
                .fillMaxWidth(1f)
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SpPr() {
    TapLogTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SelectP(innerPadding)
        }
    }
}