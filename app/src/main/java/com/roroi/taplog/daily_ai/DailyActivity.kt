package com.roroi.taplog.daily_ai

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class DailyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyTheme {
                val navController = rememberNavController()
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

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToEditor = { id ->
                                if (id == null) navController.navigate("editor")
                                else navController.navigate("editor?id=$id")
                            },
                            onNavigateToImagePicker = {
                                // 简单的权限检查逻辑可在此扩展，此处直接调用 Picker
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    }

                    composable("editor?id={id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")
                        EditorScreen(
                            entryId = id,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 纯文本编辑（新建）
                    composable("editor") {
                        EditorScreen(
                            entryId = null,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}