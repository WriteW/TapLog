package com.roroi.taplog.daily_ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ImageRatioDialog(
    onDismiss: () -> Unit,
    onRatioSelected: (Float) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("选择图片显示比例", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                ImageRatio.entries.forEach { ratio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRatioSelected(ratio.ratio) }
                            .padding(12.dp)
                    ) {
                        Text(ratio.label)
                    }
                }
            }
        }
    }
}