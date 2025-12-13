package com.roroi.taplog.score

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roroi.taplog.R

// ==========================================
// Market UI 组件
// ==========================================

@Composable
fun GoodsCard(
    goods: Goods,
    onBuyClick: (Goods) -> Unit,
    onLongClick: (Goods) -> Unit
) {
    val themeColor = goods.getColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // 条状高度
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onBuyClick(goods) },
                onLongClick = { onLongClick(goods) }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：色块装饰 (代替图片)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(themeColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 中间：信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goods.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goods.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 右侧：价格按钮
            Surface(
                color = themeColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.roi_coin),
                        contentDescription = "Price Coin",
                        // Image 组件不需要 tint
                        modifier = Modifier.size(16.dp),
                        contentScale = ContentScale.Fit // 确保图片能完整显示
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${goods.price}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }
        }
    }
}

@Composable
fun MarketScreen(
    onBuyClick: (Goods) -> Unit,
    onGoodsDelete: (Goods) -> Unit,
    topBar: @Composable () -> Unit
) {
    var goodsToDelete by remember { mutableStateOf<Goods?>(null) }
    val context = LocalContext.current

    // 商品删除弹窗
    if (goodsToDelete != null) {
        AlertDialog(
            onDismissRequest = { goodsToDelete = null },
            title = { Text("下架商品") },
            text = { Text("确定要删除 \"${goodsToDelete?.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        goodsToDelete?.let { onGoodsDelete(it) }
                        goodsToDelete = null
                    }
                ) { Text("删除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { goodsToDelete = null }) { Text("取消") }
            }
        )
    }
    Scaffold(topBar = topBar) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = 80.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6F8))
        ) {
            items(GlobalV.goodsList) { goods ->
                GoodsCard(
                    goods = goods,
                    onBuyClick = onBuyClick,
                    onLongClick = {
                        performRichHaptics(context, HapticType.FAILURE)
                        goodsToDelete = goods
                    }
                )
            }

            // 如果列表为空，显示提示
            if (GlobalV.goodsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无商品，快去添加吧！", color = Color.Gray)
                    }
                }
            }
        }
    }
}