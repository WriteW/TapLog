package com.roroi.taplog.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EditPage(innerPaddingValues: PaddingValues) {
    val streamVM: StreamViewModel = viewModel()
    val state = remember { TextFieldState("") }
    LaunchedEffect(Unit) {
        streamVM.updateEpState(state)
    }
    var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    LaunchedEffect(state.text) {
        // 文本变化时执行，例如记录最后修改时间
        streamVM.checkChange()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(innerPaddingValues)
    ) {
        BasicTextField(
            state = state,
            onTextLayout = { getResult ->
                // 保存布局结果，供绘制使用
                layoutResult = getResult()
            },
            decorator = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent) // 整体背景
                        .padding(start = 8.dp, end = 8.dp)
                        // 高亮
                        .drawWithContent {
                            // 1. 绘制自定义高亮背景
                            layoutResult?.let { layout ->
                                extractOuterParenthesesWithIndex(state.text.toString()).forEach { matchResult ->
                                    val range = matchResult.range // 字符索引范围
                                    // 获取该范围所有字符的边界框（可能跨行）
                                    val start = range.first
                                    val end = range.last + 1 // 注意范围是左闭右开？取决于您的range定义
                                    for (i in start until end) {
                                        val charRect = layout.getBoundingBox(i)
                                        drawRect(
                                            color = Color.Cyan.copy(alpha = 0.5f),
                                            topLeft = charRect.topLeft,
                                            size = charRect.size,
                                        )
                                    }
                                }
                            }
                            // 2. 绘制原本的内容（文本、光标、选区等）
                            drawContent()
                        }
                ) {
                    innerTextField()
                }
            },
            textStyle = TextStyle(fontSize = 18.sp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditPagePreview() {
    EditPage(PaddingValues())
}