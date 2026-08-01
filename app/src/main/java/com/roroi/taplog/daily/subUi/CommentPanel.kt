package com.roroi.taplog.daily.subUi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.daily.viewmodel.EntryComment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CommentPanel(
    entryId: String,
    viewModel: DailyViewModel,
    onDelete: (EntryComment) -> Unit,
    page: Int
) {
    LaunchedEffect(page) {
        viewModel.loadComment(entryId)
    }
    val comments = viewModel.currentComments
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(
                items = comments,
                key = {
                    it.id
                }
            ) { comment ->

                CommentPart(
                    modifier = Modifier.fillMaxWidth(),
                    comment = comment,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun CommentPart(
    modifier: Modifier = Modifier,
    comment: EntryComment,
    onDelete: (EntryComment) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }


    Box(
        modifier = modifier
            .padding(vertical = 8.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color.Cyan)
                )


                Spacer(
                    Modifier.width(8.dp)
                )


                Column {
                    Text(
                        comment.who.name,
                        fontSize = 16.sp
                    )

                    Text(
                        formatTimestamp(comment.timestamp),
                        color = Color.Gray,
                        fontSize = 8.sp
                    )
                }
            }


            Text(
                comment.content,
                modifier = Modifier.padding(
                    start = 40.dp,
                    top = 4.dp
                )
            )
        }

        Box(modifier = Modifier.align(
            Alignment.TopEnd
        )) {
            IconButton(
                onClick = {
                    expanded = true
                }
            ) {

                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("删除")
                    },
                    onClick = {
                        expanded = false
                        // 删除操作
                        onDelete(comment)
                    }
                )
            }
        }
    }
}

fun formatTimestamp(
    timestampMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    // 1. 定义所需的时间格式：年-月-日 时:分
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

    // 2. 将毫秒级 Long 时间戳转为 Instant，并结合设备的默认时区转为 ZonedDateTime 格式化
    return Instant.ofEpochMilli(timestampMs)
        .atZone(zoneId)
        .format(formatter)
}