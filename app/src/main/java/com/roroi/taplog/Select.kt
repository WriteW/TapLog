package com.roroi.taplog

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.roroi.taplog.ui.theme.TapLogTheme
import kotlinx.coroutines.delay

data class CardItem(val image: Int, val title: String, val cls: Class<*>)

val itemList = listOf(
    CardItem(R.drawable.tap_cover, "Tap", Tap::class.java),
    CardItem(R.drawable.log_cover, "Log", Log::class.java),
    CardItem(R.drawable.isokay_cover, "Is it okay?", IsOkay::class.java),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectP(innerPadding: PaddingValues) {
    val context = LocalContext.current

    // 动画量
    var textAnim by remember { mutableStateOf(false) }
    val displayOffset by animateFloatAsState(
        targetValue = if (textAnim) 200f else 0f,
        animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing)
    )
    var itemAnim by remember { mutableStateOf(false) }
    val leftToRight by animateFloatAsState(targetValue = if (itemAnim) 0f else -(LocalConfiguration.current.screenWidthDp.toFloat()), animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing))

    // 进入界面后才触发动画
    LaunchedEffect(Unit) {
        delay(600)
        textAnim = true
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
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    itemList.forEach { item ->
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
                                    .size(width = 110.dp, height = 150.dp)   // 设置长宽
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
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "这个倒是我画的",
                textAlign = TextAlign.Center,
                color = godColor,
                fontSize = 20.sp,
                modifier = Modifier.offset(0.dp, displayOffset.dp)
            )
        }
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