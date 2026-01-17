package com.roroi.taplog.daily

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

@Composable
fun ImageCropSelectionDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (Float, Boolean, CropParams) -> Unit // 【修改】返回 CropParams
) {
    var selectedRatio by remember { mutableFloatStateOf(1f) }
    var isLargeMode by remember { mutableStateOf(false) }

    // 临时存储当前的裁剪状态，准备传出
    var currentCropParams by remember { mutableStateOf(CropParams()) }

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
                Text("Adjust Image", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pan and zoom to crop.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                // --- 预览区域 ---
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                ) {
                    val density = LocalDensity.current

                    // 【关键】：统一基准宽度为 240.dp (对应大卡片宽度)
                    // 无论选什么比例，都基于这个宽度计算，保存的 offset 也就有了统一标准
                    val frameWidthDp = 240.dp

                    // 根据比例计算高度
                    // 1:1 -> 240dp
                    // 2:1 -> 120dp
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
                        // 【关键】：添加 key，当 frame 大小改变时强制重组 MoveableImage
                        // 这样可以重新计算 Fit/Cover 逻辑，解决“点击后预览不变”的问题
                        key(selectedRatio, isLargeMode) {
                            MoveableImage(
                                imageUri = imageUri,
                                containerSize = IntSize(frameWidthPx, frameHeightPx),
                                onCropChanged = { params -> currentCropParams = params }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RatioOptionButton("1:1", selectedRatio == 1f && !isLargeMode) { selectedRatio = 1f; isLargeMode = false }
                    RatioOptionButton("1:2", selectedRatio == 2f) { selectedRatio = 2f; isLargeMode = false }
                    RatioOptionButton("2:2", isLargeMode) { selectedRatio = 1f; isLargeMode = true }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onConfirm(selectedRatio, isLargeMode, currentCropParams) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}



@Composable
fun MoveableImage(
    imageUri: Uri,
    containerSize: IntSize,
    onCropChanged: (CropParams) -> Unit
) {
    val context = LocalContext.current

    // 加载 2048px 限制的原图
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUri)
            .size(2048)
            .scale(coil.size.Scale.FIT)
            .build()
    )

    var userScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageIntrinsicSize by remember { mutableStateOf(IntSize.Zero) }

    val state = painter.state
    if (state is AsyncImagePainter.State.Success) {
        val size = state.painter.intrinsicSize
        imageIntrinsicSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    }

    if (imageIntrinsicSize.width > 0 && containerSize.width > 0) {
        // Fit-to-Cover 算法
        val imageRatio = imageIntrinsicSize.width.toFloat() / imageIntrinsicSize.height
        val containerRatio = containerSize.width.toFloat() / containerSize.height

        val fittedWidth: Float
        val fittedHeight: Float

        if (imageRatio > containerRatio) {
            fittedWidth = containerSize.width.toFloat()
            fittedHeight = fittedWidth / imageRatio
        } else {
            fittedHeight = containerSize.height.toFloat()
            fittedWidth = fittedHeight * imageRatio
        }

        val baseScale = max(
            containerSize.width / fittedWidth,
            containerSize.height / fittedHeight
        )

        // 实时回传参数
        LaunchedEffect(userScale, offset) {
            onCropChanged(CropParams(userScale, offset.x, offset.y))
        }

        val currentScale = baseScale * userScale
        val displayWidth = fittedWidth * currentScale
        val displayHeight = fittedHeight * currentScale

        val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
        val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)

        val clampedOffsetX = offset.x.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedOffsetY = offset.y.coerceIn(-maxOffsetY, maxOffsetY)

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit, // 基础铺垫
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        userScale = (userScale * zoom).coerceIn(1f, 5f)
                        val newOffset = offset + pan

                        // 重新计算动态边界
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
                    // 应用所有变换
                    scaleX = currentScale
                    scaleY = currentScale
                    translationX = clampedOffsetX
                    translationY = clampedOffsetY
                }
        )
    } else {
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