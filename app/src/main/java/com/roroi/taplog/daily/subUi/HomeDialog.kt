package com.roroi.taplog.daily.subUi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.request.ImageRequest
import com.roroi.taplog.daily.soBiscuitFont
import com.roroi.taplog.daily.viewmodel.DailyEntry
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

// ImageViewerDialog: 全屏图片查看器，支持缩放拖动和删除
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerDialog(
    entry: DailyEntry,
    viewModel: DailyViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    // 🌟 1. 这里现在直接拿到的是 DailyEntry 对象的列表，无需再解析！
    val (imageEntries, initialPage) = viewModel.getOrderedImagesAndIndex(entry)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageEntries.size }
    )

    val context = LocalContext.current
    val activity = remember(context) {
        context.findActivity()
    }

    var isLandscape by remember {
        mutableStateOf(false)
    }

    DisposableEffect(isLandscape) {
        activity?.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    // 🌟 用来避让面板的垂直位移
    val pagerOffsetY = remember { Animatable(0f) }

    // 🌟 新增：控制所有悬浮按钮是否显示的标志
    var isUiVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("删除图片") },
                text = { Text("确定要删除这张图片吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                        Text("删除", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                }
            )
        }

        val scope = rememberCoroutineScope()

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val sheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Hidden,
                skipHiddenState = false
            )
            val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
            val configuration = LocalConfiguration.current

            // 面板展开时，把整个 Pager 往上推
            LaunchedEffect(sheetState.targetValue) {
                if (sheetState.targetValue == SheetValue.Hidden) {
                    pagerOffsetY.animateTo(0f, tween(300))
                } else {
                    pagerOffsetY.animateTo(-600f, tween(300))
                }
            }

            // 🌟 2. 直接拿对象，再也没有 JSON 崩溃了！
            val currentImageEntry = imageEntries[pagerState.currentPage]

            LaunchedEffect(currentImageEntry.id) {
                viewModel.loadComment(currentImageEntry.id)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(WindowInsets.ime)
                ) {
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        containerColor = Color.Black,
                        sheetContainerColor = MaterialTheme.colorScheme.surface,
                        sheetPeekHeight = (configuration.screenHeightDp * 0.5f).dp,
                        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                        sheetContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.9f)
                                    .padding(bottom = 76.dp)
                            ) {
                                CommentPanel(
                                    currentImageEntry.id,
                                    viewModel,
                                    onDelete = {
                                        viewModel.delComment(it.id, currentImageEntry.id)
                                    },
                                    page = pagerState.currentPage
                                )
                            }
                        }
                    ) { _ ->

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { translationY = pagerOffsetY.value }
                            ) { page ->
                                // 🌟 3. 拿到该页的对象，并调用 ViewModel 的标准 API 获取文件路径
                                val imageEntry = imageEntries[page]
                                val imageFile = remember(imageEntry) {
                                    viewModel.getFullImagePath(imageEntry.content)
                                }

                                val zoomableState = rememberZoomableImageState()

                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ZoomableAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageFile)
                                            .size(coil.size.Size.ORIGINAL)
                                            .build(),
                                        contentDescription = null,
                                        state = zoomableState,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = {
                                            // 🌟 如果评论区开着，点击先关掉评论区
                                            if (sheetState.currentValue != SheetValue.Hidden) {
                                                scope.launch { sheetState.hide() }
                                            } else {
                                                // 🌟 否则，点击切换所有按钮的显示 / 隐藏（沉浸模式）
                                                isUiVisible = !isUiVisible
                                            }
                                        }
                                    )
                                }
                            }

                            // 返回按钮 (TopStart)
                            AnimatedVisibility(
                                visible = isUiVisible,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(16.dp).statusBarsPadding()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            // 顶部评论展开按钮 (TopEnd)
                            AnimatedVisibility(
                                visible = isUiVisible,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).statusBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalIconButton(
                                        onClick = { isLandscape = !isLandscape },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.ScreenRotation, contentDescription = "screen rotate")
                                    }
                                    FilledTonalIconButton(
                                        onClick = {
                                            scope.launch {
                                                if (sheetState.currentValue == SheetValue.Hidden) {
                                                    sheetState.partialExpand()
                                                } else {
                                                    sheetState.hide()
                                                }
                                            }
                                        },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "comment")
                                    }
                                }
                            }

                            // 底部按钮 (BottomCenter) - 删除 / 保存
                            AnimatedVisibility(
                                visible = isUiVisible,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(200)),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                                ) {
                                    val currentImageFile = remember(currentImageEntry) {
                                        viewModel.getFullImagePath(currentImageEntry.content)
                                    }

                                    FilledTonalIconButton(
                                        onClick = { showDeleteConfirm = true },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.Red)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }

                                    FilledTonalIconButton(
                                        onClick = {
                                            viewModel.saveImageToGalley(currentImageFile)
                                            viewModel.toastOut("已保存到相册")
                                        },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download")
                                    }
                                }
                            }
                        }
                    }
                }

                // 悬浮输入框
                var textThing by remember { mutableStateOf("") }

                AnimatedVisibility(
                    visible = sheetState.targetValue != SheetValue.Hidden,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = textThing,
                            onValueChange = { textThing = it },
                            placeholder = { Text("说点什么...") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = {
                                if (textThing.isNotBlank()) {
                                    viewModel.sendComment(textThing, currentImageEntry.id)
                                    textThing = ""
                                }
                            },
                            containerColor = Color.Green,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send")
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ClearConfirmationDialog(count: Int, onClear: () -> Unit, onDismiss: (Int) -> Unit) {
    val (title, text) = when (count) {
        1 -> "删除所有数据？" to "这将删除您的所有条目和照片。确定吗？"
        2 -> "确定要删除？" to "此操作不可撤销。所有数据将永久丢失。"
        3 -> "最终警告" to "点击确认将清空所有数据。"
        else -> "" to ""
    }

    AlertDialog(
        onDismissRequest = { onDismiss(0) },
        title = { Text(title, fontWeight = FontWeight.Bold, color = Color.Red) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                if (count == 3) {
                    onClear(); onDismiss(0)
                } else onDismiss(count + 1)
            }) { Text(if (count == 3) "WIPE" else "Confirm", color = Color.Red) }
        },
        dismissButton = { TextButton(onClick = { onDismiss(0) }) { Text("Cancel") } }
    )
}

// 核对密码
@Composable
fun PasswordCheckDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "进入空间",
    errorMessage: String? = null
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = soBiscuitFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "请输入访问密码",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), // 隐藏字符
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}

@Composable
fun LoadingDialog() {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = true,
                    onClick = { }  // 空点击，仅用于消费触摸事件
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (oldPass: String, newPass: String) -> Unit,
    hasOldPassword: Boolean
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // 用于控制密码是否可见的状态
    var passwordVisible by remember { mutableStateOf(false) }

    // 错误提示状态
    var oldPasswordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "修改密码", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 旧密码输入框
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        oldPasswordError = false
                    },
                    label = { Text("当前密码（无密码则留空）") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = oldPasswordError && hasOldPassword,
                    supportingText = {
                        if (oldPasswordError) {
                            Text("当前密码不能为空", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. 新密码输入框
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    label = { Text("新密码（留空则取消密码）") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. 确认新密码输入框
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmPasswordError = false
                    },
                    label = { Text("确认新密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPasswordError,
                    supportingText = {
                        if (confirmPasswordError) {
                            Text("两次输入的新密码不一致", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. 显示/隐藏密码开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it }
                    )
                    Text(text = "显示密码", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 校验逻辑
                    if (oldPassword.isBlank() && hasOldPassword) {
                        oldPasswordError = true
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        confirmPasswordError = true
                        return@Button
                    }

                    // 校验通过，调用确认回调
                    onConfirm(oldPassword, newPassword)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.Gray)
            }
        }
    )
}