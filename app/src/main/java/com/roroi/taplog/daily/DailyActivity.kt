package com.roroi.taplog.daily

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roroi.taplog.daily.subScreen.EditorScreen
import com.roroi.taplog.daily.subScreen.PortalEditor
import com.roroi.taplog.daily.viewmodel.DailyViewModel
import com.roroi.taplog.ui.theme.TapLogTheme

@Suppress("DEPRECATION")
class DailyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TapLogTheme {
                val  navController = rememberNavController()
                val viewModel: DailyViewModel = viewModel()

                // 图片选择器状态
                var showRatioDialog by remember { mutableStateOf(false) }
                var tempImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val imagePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        tempImageUri = uri
                        showRatioDialog = true
                    }
                }

                // 比例选择弹窗
                if (showRatioDialog && tempImageUri != null) {
                    ImageCropSelectionDialog(
                        imageUri = tempImageUri!!,
                        onDismiss = { showRatioDialog = false },
                        // 【修改】：接收 ratio 和 isLarge
                        onConfirm = { ratio, isLarge, cropParams ->
                            viewModel.addImageEntry(tempImageUri!!, ratio, isLarge, cropParams)
                            showRatioDialog = false
                            tempImageUri = null
                        }
                    )
                }

                // 切换页面监听
                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { pair ->
                        when (pair.first) {
                            "editor" -> {
                                navController.navigate(pair.second)
                            }

                            "imagePicker" -> {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }

                            "pop" -> {
                                navController.popBackStack()
                                Log.d("snake is cute", ".2.")
                            }

                            "portal" -> {
                                navController.navigate(pair.second)
                            }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel
                        )
                    }

                    // 纯文本编辑（新建）
                    composable("editor") {
                        Log.d("DailyActivity__", "editor")
                        EditorScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 纯文本编辑（修改）
                    composable("editor?id={id}") { backStackEntry ->
                        EditorScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("portal?id={id}") { backStackEntry ->
                        val fatherId = backStackEntry.arguments?.getString("id")
                        fatherId?.let { PortalEditor(viewModel = viewModel, it) }
                    }
                }
            }
        }
    }
}