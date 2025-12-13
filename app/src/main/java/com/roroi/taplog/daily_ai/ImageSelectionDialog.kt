package com.roroi.taplog.daily_ai

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ImageCropSelectionDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Float, Boolean) -> Unit // 【修改】增加 Boolean 返回值
) {
    // 状态管理
    var selectedRatio by remember { mutableFloatStateOf(ImageRatio.SQUARE.ratio) }
    var isLargeMode by remember { mutableStateOf(false) } // 【新增】是否为大图模式

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ... (标题代码保持不变) ...
                Text("Select Display Style", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Original image saved. Choose layout size.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                // --- 预览区域 ---
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                ) {
                    val density = LocalDensity.current

                    // 【关键】：根据选择的模式，动态计算预览框的大小
                    // 1x1: 宽度小，高度小
                    // 1x2: 宽度大，高度小
                    // 2x2: 宽度大，高度大
                    // 这里我们模拟 Timeline 上的显示效果 (Timeline上 1x1约120dp, 1x2约240dp)
                    val baseWidth = 120.dp

                    val frameWidthDp = if (selectedRatio > 1.5f || isLargeMode) baseWidth * 2 else baseWidth
                    val frameHeightDp = frameWidthDp / selectedRatio

                    val frameWidthPx = with(density) { frameWidthDp.toPx().roundToInt() }
                    val frameHeightPx = with(density) { frameHeightDp.toPx().roundToInt() }

                    Box(
                        modifier = Modifier
                            .size(frameWidthDp, frameHeightDp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .clipToBounds()
                    ) {
                        MoveableImage(
                            imageUri = imageUri,
                            containerSize = IntSize(frameWidthPx, frameHeightPx)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 比例选择按钮 (3个) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RatioOptionButton(
                        label = "1:1",
                        isSelected = selectedRatio == 1f && !isLargeMode,
                        onClick = {
                            selectedRatio = 1f
                            isLargeMode = false
                        }
                    )

                    RatioOptionButton(
                        label = "1:2 (Wide)",
                        isSelected = selectedRatio == 2f,
                        onClick = {
                            selectedRatio = 2f
                            isLargeMode = false
                        }
                    )

                    RatioOptionButton(
                        label = "2:2 (Large)",
                        isSelected = isLargeMode,
                        onClick = {
                            selectedRatio = 1f // 2x2 物理裁剪也是 1:1
                            isLargeMode = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 确认按钮 ---
                Button(
                    onClick = { onConfirm(selectedRatio, isLargeMode) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Photo", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}


@Composable
fun MoveableImage(
    imageUri: Uri,
    containerSize: IntSize
) {
    val context = LocalContext.current

    // 1. Coil 加载配置
    // 我们限制 max dimension 为 2048，这既保证了清晰度，又防止了 OOM。
    // 关键是 Scale.FIT，确保 Coil 加载的是完整的、未裁剪的原图缩略。
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUri)
            .size(2048)
            .scale(coil.size.Scale.FIT)
            .precision(coil.size.Precision.INEXACT)
            .build()
    )

    // 手势状态
    var userScale by remember { mutableFloatStateOf(1f) } // 用户操作产生的额外缩放
    var offset by remember { mutableStateOf(Offset.Zero) } // 用户产生的位移

    // 记录图片内容的原始比例 (用于计算)
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    // 获取图片原始尺寸
    val state = painter.state
    if (state is AsyncImagePainter.State.Success) {
        val size = state.painter.intrinsicSize
        imageIntrinsicSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    }

    // 只有当图片加载完成且容器有尺寸时才显示
    if (imageIntrinsicSize.width > 0 && containerSize.width > 0) {

        // --- 核心算法：Fit-to-Cover ---

        // 1. 计算 ContentScale.Fit 下，图片在屏幕上实际显示的宽和高
        val imageRatio = imageIntrinsicSize.width.toFloat() / imageIntrinsicSize.height
        val containerRatio = containerSize.width.toFloat() / containerSize.height

        // Fit 模式下的显示尺寸
        val fittedWidth: Float
        val fittedHeight: Float

        if (imageRatio > containerRatio) {
            // 图片更宽，Fit 模式下宽度撑满容器，高度有上下黑边
            fittedWidth = containerSize.width.toFloat()
            fittedHeight = fittedWidth / imageRatio
        } else {
            // 图片更高，Fit 模式下高度撑满容器，宽度有左右黑边
            fittedHeight = containerSize.height.toFloat()
            fittedWidth = fittedHeight * imageRatio
        }

        // 2. 计算 baseScale：为了消除黑边，需要放大的倍数
        // 如果是宽图(有上下黑边)，需要按高度拉伸到容器高度
        // 如果是高图(有左右黑边)，需要按宽度拉伸到容器宽度
        val baseScale = max(
            containerSize.width / fittedWidth,
            containerSize.height / fittedHeight
        )

        // 3. 计算最终的总缩放比例
        val currentScale = baseScale * userScale

        // 4. 计算边界限制 (Clamping)
        // 图片当前实际显示的尺寸
        val displayWidth = fittedWidth * currentScale
        val displayHeight = fittedHeight * currentScale

        // 允许偏移的最大距离 = (大图 - 容器) / 2
        val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
        val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)

        // 修正 Offset，防止划出界
        val clampedOffsetX = offset.x.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedOffsetY = offset.y.coerceIn(-maxOffsetY, maxOffsetY)

        Image(
            painter = painter,
            contentDescription = "Moveable Image",
            // 【关键】：使用 Fit，让 Compose 帮我们处理好初始的居中和比例
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds() // 确保放大后超出容器的部分被裁掉
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // 更新用户缩放：最小为 1f (即 baseScale 状态)，最大允许放大 5 倍
                        userScale = (userScale * zoom).coerceIn(1f, 5f)

                        // 更新位移：加上手势距离
                        val newOffset = offset + pan

                        // 重新计算边界限制 (因为 Scale 变了，边界也变了)
                        val currDispW = fittedWidth * (baseScale * userScale)
                        val currDispH = fittedHeight * (baseScale * userScale)
                        val currMaxX = max(0f, (currDispW - containerSize.width) / 2f)
                        val currMaxY = max(0f, (currDispH - containerSize.height) / 2f)

                        offset = Offset(
                            newOffset.x.coerceIn(-currMaxX, currMaxX),
                            newOffset.y.coerceIn(-currMaxY, currMaxY)
                        )
                    }
                }
                .graphicsLayer {
                    // 应用变换：基础放大(消除黑边) * 用户放大
                    scaleX = currentScale
                    scaleY = currentScale
                    translationX = clampedOffsetX
                    translationY = clampedOffsetY
                }
        )
    } else {
        // Loading State
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RatioOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if(isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            enabled = true,
            selected = true
        )
    )
}