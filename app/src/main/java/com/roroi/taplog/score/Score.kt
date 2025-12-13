package com.roroi.taplog.score

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roroi.taplog.R
import com.roroi.taplog.ui.theme.TapLogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

// --- 工具函数区域 ---

fun performRichHaptics(context: Context, type: HapticType) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator == null) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val effect = when (type) {
            HapticType.SUCCESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticType.FAILURE -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(if (type == HapticType.SUCCESS) 50L else 150L)
    }
}

enum class HapticType { SUCCESS, FAILURE }

private var currentToast: Toast? = null
fun showNativeToast(context: Context, message: String) {
    // 1. 关键步骤：如果有正在显示的 Toast，立即取消它！
    // 这样就切断了排队，直接让上一条消失
    currentToast?.cancel()

    // 2. 创建新的 Toast
    currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)

    // 3. 显示新的
    currentToast?.show()
}

// 扩展函数：计算坐标
fun LayoutCoordinates.positionInRoot(root: LayoutCoordinates?): Offset {
    if (root == null) return Offset.Zero
    val boundsInWindow = this.positionInWindow()
    return root.windowToLocal(boundsInWindow)
}

// --- Activity ---

class Score : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化全局数据
        GlobalV.init(this)

        setContent {
            TapLogTheme {
                TapLogApp()
            }
        }
    }
}

// --- 主界面 Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapLogApp(
    viewModel: ScoreViewModel = viewModel()
) {
    val taskScore by viewModel.taskScore.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }



    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置积分") },
            text = { Text("确定要清空所有积分吗？此操作不可撤销！😱") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetScore() // 执行清空
                        showResetDialog = false
                        // 震动反馈
                        performRichHaptics(context, HapticType.FAILURE)
                        showNativeToast(context, "积分已清空 💸")
                    }
                ) {
                    Text("清空", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    @Composable
    fun HomeTopBar() {
        CenterAlignedTopAppBar(
            title = {
                Text("TapLog", fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                // 左侧：分数显示
                // 包裹一层 Box 增加左侧间距
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    ScoreDisplay(
                        score = taskScore.score,
                        dScore = taskScore.dScore,
                        onIntegrateScore = {
                            viewModel.integrateScore {
                                performRichHaptics(
                                    context,
                                    HapticType.SUCCESS
                                )
                            }
                        },
                        onLongClick = {
                            performRichHaptics(context, HapticType.SUCCESS)
                            showResetDialog = true
                        }
                    )
                }
            },
            actions = {
                // 右侧：购物车图标 (点击切换去商店/回首页)
                IconButton(onClick = {
                    if (currentRoute == "home") {
                        navController.navigate("market") {
                            // 避免堆叠过深
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        // 如果已经在商店，点击则返回首页
                        navController.popBackStack()
                    }
                }) {
                    Icon(
                        // 如果在首页显示购物车，如果在商店显示首页图标（或者返回箭头）
                        imageVector = if (currentRoute == "home") Icons.Default.ShoppingCart else Icons.Default.Home,
                        contentDescription = "Switch View",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
        )
    }
    Scaffold(
        floatingActionButton = {
            if (currentRoute == "home" || currentRoute == "market") FloatingActionButton(
                    onClick = {
                        if (currentRoute == "market") {
                            navController.navigate("add_goods")
                        } else {
                            navController.navigate("add_task")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
        }
    ) { innerPadding ->
        innerPadding
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.background(Color(0xFFF5F6F8)),
        ) {
            composable("home") {
                HomeScreen(
                    onTaskClick = { task ->
                        showNativeToast(context, "SUCCESS✅")
                        performRichHaptics(context, HapticType.SUCCESS)
                        viewModel.addIncome(task.income)
                    },
                    onTaskDelete = { task -> viewModel.deleteTask(task) },
                    topBar = { HomeTopBar() },
                    onEditTask = { task ->
                        // 1. 序列化 Task -> JSON
                        val taskJson = Json.encodeToString(task)
                        // 2. URL 编码 (防止 JSON 中的 {}, "" 等字符破坏路由格式)
                        val encodedJson = Uri.encode(taskJson)
                        // 3. 导航并传递参数
                        navController.navigate("add_task?task=$encodedJson")
                    }
                )
            }
            composable("market") {
                MarketScreen(
                    onBuyClick = { goods ->
                        val success = viewModel.purchase(goods.price)
                        if (success) {
                            showNativeToast(context, "SUCCESS✅")
                            performRichHaptics(context, HapticType.SUCCESS)
                        } else {
                            performRichHaptics(context, HapticType.SUCCESS)
                        }
                    },
                    onGoodsDelete = { goods ->
                        viewModel.deleteGoods(goods)
                    },
                    topBar = { HomeTopBar() },
                    onEditGoods = { goods ->
                        val goodsJson = Json.encodeToString(goods)
                        val encodedJson = Uri.encode(goodsJson)
                        navController.navigate("add_goods?goods=$encodedJson")
                    },
                )

            }
            // --- 修改：添加任务页面 (接收参数) ---
            composable(
                route = "add_task?task={task}", // 定义路由结构
                arguments = listOf(
                    navArgument("task") {
                        type = NavType.StringType
                        nullable = true // 允许为空（新增模式）
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                // 获取参数
                val taskJson = backStackEntry.arguments?.getString("task")

                AddTaskApp(
                    onBack = { navController.popBackStack() },
                    initialTaskJson = taskJson // 将 JSON 传给页面
                )
            }

            // --- 修改：添加商品页面 (接收参数) ---
            composable(
                route = "add_goods?goods={goods}",
                arguments = listOf(
                    navArgument("goods") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val goodsJson = backStackEntry.arguments?.getString("goods")

                AddGoodsApp(
                    onBack = { navController.popBackStack() },
                    initialGoodsJson = goodsJson // 将 JSON 传给页面
                )
            }
        }
    }
}

@Composable
fun ScoreDisplay(
    score: Int,
    dScore: Int,
    onIntegrateScore: () -> Unit,
    onLongClick: () -> Unit
) {
    val animatedDScore by animateIntAsState(
        targetValue = dScore,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "DScoreAnimation"
    )
    // 1. 定义颤抖动画的位移状态 (X轴)
    val shakeOffset = remember { Animatable(0f) }

    // 2. 监听 dScore 的变化，启动颤抖循环
    LaunchedEffect(dScore) {
        if (dScore < 0) {
            // 只有负数才颤抖
            while (isActive) {
                delay(2000) // "过一会" (这里设为2秒)
                // 开始颤抖序列：左 -> 右 -> 回正
                shakeOffset.animateTo(-5f, spring(stiffness = 2000f)) // 快速左移
                shakeOffset.animateTo(5f, spring(stiffness = 2000f))  // 快速右移
                shakeOffset.animateTo(-3f, spring(stiffness = 2000f))
                shakeOffset.animateTo(3f, spring(stiffness = 2000f))
                shakeOffset.animateTo(0f, spring(stiffness = 1000f))  // 回正
            }
        } else {
            // 如果变回正数或0，立即停止颤抖
            shakeOffset.snapTo(0f)
        }
    }
    // 1. 定义动画状态
    // 当外部传入的 score 发生变化（比如从 100 变 110），animatedScore 会在 500ms 内慢慢变过去
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(
            durationMillis = 500, // 0.5秒
            easing = LinearOutSlowInEasing // 先快后慢，比较自然
        ),
        label = "ScoreAnimation"
    )

    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // 同样去除波纹，或者去掉这行保留波纹反馈
                onClick = onIntegrateScore,
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.roi_coin),
            contentDescription = "Coin",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 2. 使用 animatedScore 而不是 score
            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (animatedDScore != 0) {
                Text(
                    // [修改] 使用动画值显示
                    text = if (animatedDScore > 0) "+$animatedDScore" else "$animatedDScore",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    // [修改] 颜色基于动画值：负数过程保持红色，正数过程保持主题色
                    color = if (animatedDScore < 0) Color.Red else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = shakeOffset.value.dp)
                )
            }
        }
    }
}
