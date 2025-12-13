package com.roroi.taplog.score

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roroi.taplog.ui.theme.TapLogTheme

// 商品表单状态
class AddGoodsState {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var price by mutableIntStateOf(30)

    fun toGoods(): Goods {
        return Goods(
            title = title.ifBlank { "title" },
            description = description.ifBlank { "description" },
            price = price
        )
    }
}

@Composable
fun rememberAddGoodsState() = remember { AddGoodsState() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoodsApp(onBack: () -> Unit) {
    val state = rememberAddGoodsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAppVisible by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppVisible = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = {
        AddGoodsTopBar(onBack = onBack, state = state)
    }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景层 (复用 AddTask 的模糊背景逻辑，为了代码简洁这里简化写，
            // 实际项目中可以把 OptimizedBackground 提取到公共文件)
            if (isAppVisible) {
                OptimizedBackground()
            }

            // 遮罩
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.6f else 0.9f))
            )

            // 内容
            AddGoodsContent(
                state = state,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun AddGoodsContent(state: AddGoodsState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 预览卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.65f))
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    // 实时预览效果
                    GoodsCard(goods = state.toGoods(), onBuyClick = {}, onLongClick = {})
                }
            }
        }

        // 文本输入
        item {
            TransparentTextField(
                value = state.title,
                onValueChange = { state.title = it },
                label = "商品名称",
                placeholder = "例如：一杯咖啡"
            )
        }
        item {
            TransparentTextField(
                value = state.description,
                onValueChange = { state.description = it },
                label = "商品描述",
                placeholder = "例如：一杯经典的咖啡，带有浓郁的咖啡香气..."
            )
        }

        // 价格设置
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.weight(3.5f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "价格", modifier = Modifier.padding(end = 8.dp))
                    NumberStepper(
                        value = state.price,
                        onValueChange = { state.price = it },
                        range = 0..100000
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // 提示信息
        item {
            Text(
                text = "颜色将根据价格自动生成：\n<10: 绿色, <20: 蓝色, <60: 紫色, >120: 金色",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoodsTopBar(onBack: () -> Unit, state: AddGoodsState) {
    CenterAlignedTopAppBar(
        title = { Text("Add Goods", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(0.85f)),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Exit")
            }
        },
        actions = {
            IconButton(onClick = {
                GlobalV.addGoods(state.toGoods())
                onBack()
            }) {
                Icon(Icons.Rounded.Check, contentDescription = "Save")
            }
        }
    )
}

@Preview
@Composable
fun PreviewAddGoods() {
    TapLogTheme { AddGoodsApp(onBack = {}) }
}