package com.roroi.taplog.stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roroi.taplog.ui.theme.TapLogTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapLogTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun MainScreen() {
    val streamVM: StreamViewModel = viewModel()
    streamVM.updateContext(LocalContext.current)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hazeState = remember { HazeState() }
    val scope = rememberCoroutineScope()

    // 用于删除确认对话框的状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var streamToDelete by remember { mutableStateOf<Uuid?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Color.Transparent,
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    drawerContainerColor = Color.Cyan.copy(alpha = 0.2f),
                    drawerContentColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .width(150.dp)
                        .hazeChild(state = hazeState)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        items(
                            items = streamVM.streamList,
                            key = { it.uuid }
                        ) { stream ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            streamVM.setCurrentStream(stream.uuid)
                                            scope.launch { drawerState.close() }
                                        },
                                        onLongClick = {
                                            streamToDelete = stream.uuid
                                            showDeleteDialog = true
                                        }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stream.originName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) {
            @Suppress("DEPRECATION")
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Stream") },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } }
                            ) {
                                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { streamVM.saveFile() }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SaveAs,
                                    contentDescription = "Save"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState, backgroundColor = MaterialTheme.colorScheme.background)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) { paddingValues ->
                EditPage(paddingValues)

                // 删除确认对话框
                if (showDeleteDialog && streamToDelete != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            streamToDelete = null
                        },
                        title = { Text("确认删除") },
                        text = { Text("确定要删除这条记录吗？此操作不可撤销。") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    streamVM.deleteStream(streamToDelete!!)
                                    showDeleteDialog = false
                                    streamToDelete = null
                                }
                            ) {
                                Text("删除")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    streamToDelete = null
                                }
                            ) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }
}