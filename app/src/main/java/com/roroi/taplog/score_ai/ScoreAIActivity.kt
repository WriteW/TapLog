package com.roroi.taplog.score_ai // TODO: 修改为你的包名

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

// ==========================================
// 1. Data Models
// ==========================================

@Serializable
data class AppData(
    val totalScore: Int = 0,
    val tasks: List<TaskItem> = emptyList(),
    val rewards: List<RewardItem> = emptyList()
)

@Serializable
data class TaskItem(
    val id: String,
    val name: String,
    val scoreGain: Int,
    val colorStartHex: Long,
    val colorEndHex: Long
)

@Serializable
data class RewardItem(
    val id: String,
    val name: String,
    val cost: Int,
    // 修改点：商品现在也有颜色属性
    val colorStartHex: Long,
    val colorEndHex: Long
)

// ==========================================
// 2. Repository
// ==========================================

class DataRepository(private val filesDir: File) {
    private val directoryName = "score_ai"
    private val fileName = "data.json"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private fun getFile(): File {
        val dir = File(filesDir, directoryName)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    suspend fun saveData(data: AppData) = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(data)
            getFile().writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadData(): AppData = withContext(Dispatchers.IO) {
        try {
            val file = getFile()
            if (!file.exists()) return@withContext AppData()
            json.decodeFromString<AppData>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            AppData()
        }
    }
}

// ==========================================
// 3. ViewModel (Updated)
// ==========================================

class ScoreViewModel(private val repository: DataRepository) : ViewModel() {
    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _uiState = MutableStateFlow(AppData())
    val uiState: StateFlow<AppData> = _uiState.asStateFlow()

    init {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

    private fun loadData() {
        scope.launch {
            _uiState.value = repository.loadData()
        }
    }

    private fun persist() {
        scope.launch {
            repository.saveData(_uiState.value)
        }
    }

    fun addTask(name: String, score: Int, colorStart: Long, colorEnd: Long) {
        _uiState.update { current ->
            current.copy(
                tasks = current.tasks + TaskItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    scoreGain = score,
                    colorStartHex = colorStart,
                    colorEndHex = colorEnd
                )
            )
        }
        persist()
    }

    // 修改点：添加商品也接收颜色参数
    fun addReward(name: String, cost: Int, colorStart: Long, colorEnd: Long) {
        _uiState.update { current ->
            current.copy(
                rewards = current.rewards + RewardItem(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    cost = cost,
                    colorStartHex = colorStart,
                    colorEndHex = colorEnd
                )
            )
        }
        persist()
    }

    fun completeTask(task: TaskItem) {
        _uiState.update {
            it.copy(totalScore = it.totalScore + task.scoreGain)
        }
        persist()
    }

    fun redeemReward(reward: RewardItem, onError: () -> Unit, onSuccess: () -> Unit) {
        if (_uiState.value.totalScore >= reward.cost) {
            _uiState.update {
                it.copy(totalScore = it.totalScore - reward.cost)
            }
            persist()
            onSuccess()
        } else {
            onError()
        }
    }
}

class ScoreViewModelFactory(private val filesDir: File) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScoreViewModel(DataRepository(filesDir)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// 4. Activity & UI Entry Point
// ==========================================

class ScoreAIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModelFactory = ScoreViewModelFactory(filesDir)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF333333),
                    secondary = Color(0xFF4CAF50),
                    background = Color(0xFFF5F5F5),
                    surface = Color.White
                )
            ) {
                ScoreApp(viewModelFactory)
            }
        }
    }
}

@Composable
fun ScoreApp(factory: ScoreViewModelFactory) {
    val navController = rememberNavController()
    val viewModel: ScoreViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    // 移除了外层的 Scaffold，将 Scaffold 和 TopBar 下放到每个 Screen 内部
    // 这样做的目的是为了让 TopBar 上的按钮能方便地访问 Screen 内部的输入状态
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            TaskListScreen(
                tasks = uiState.tasks,
                currentScore = uiState.totalScore,
                navController = navController,
                onTaskClick = { task -> viewModel.completeTask(task) }
            )
        }
        composable("shop") {
            RewardListScreen(
                rewards = uiState.rewards,
                currentScore = uiState.totalScore,
                navController = navController,
                onRewardClick = { reward ->
                    viewModel.redeemReward(reward,
                        onError = { /* logic in screen */ },
                        onSuccess = { /* logic in screen */ }
                    )
                }
            )
        }
        composable("add_task") {
            AddTaskScreen(
                navController = navController,
                onAdd = { name, score, c1, c2 ->
                    viewModel.addTask(name, score, c1, c2)
                }
            )
        }
        composable("add_reward") {
            AddRewardScreen(
                navController = navController,
                onAdd = { name, cost, c1, c2 ->
                    viewModel.addReward(name, cost, c1, c2)
                }
            )
        }
    }
}

// ==========================================
// 5. Shared UI Components
// ==========================================

// 通用的 TopBar，支持动态内容
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color.Black
        )
    )
}

@Composable
fun ScoreBadge(score: Int) {
    Surface(
        color = Color(0xFFFFD700),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$score", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// 颜色选择器组件，复用
@Composable
fun ColorPickerRow(
    selectedGradientIndex: Int,
    onSelect: (Int) -> Unit
) {
    val gradients = listOf(
        0xFF42A5F5 to 0xFF1E88E5, // Blue
        0xFF66BB6A to 0xFF43A047, // Green
        0xFFFFA726 to 0xFFFB8C00, // Orange
        0xFFEF5350 to 0xFFE53935, // Red
        0xFFAB47BC to 0xFF8E24AA, // Purple
        0xFF78909C to 0xFF455A64  // Blue Grey
    )

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        gradients.forEachIndexed { index, (c1, c2) ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(c1), Color(c2))))
                    .border(
                        width = if (selectedGradientIndex == index) 3.dp else 0.dp,
                        color = if (selectedGradientIndex == index) Color.Black else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelect(index) }
            )
        }
    }
}

fun getGradient(index: Int): Pair<Long, Long> {
    val gradients = listOf(
        0xFF42A5F5 to 0xFF1E88E5,
        0xFF66BB6A to 0xFF43A047,
        0xFFFFA726 to 0xFFFB8C00,
        0xFFEF5350 to 0xFFE53935,
        0xFFAB47BC to 0xFF8E24AA,
        0xFF78909C to 0xFF455A64
    )
    return if (index in gradients.indices) gradients[index] else gradients[0]
}

// ==========================================
// 6. Screens
// ==========================================

@Composable
fun TaskListScreen(
    tasks: List<TaskItem>,
    currentScore: Int,
    navController: NavController,
    onTaskClick: (TaskItem) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "我的任务",
                actions = {
                    ScoreBadge(currentScore)
                    IconButton(onClick = { navController.navigate("shop") }) {
                        Icon(Icons.Default.ShoppingCart, "Shop", tint = Color.Black)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_task") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(tasks) { task ->
                TaskCard(task, onClick = { onTaskClick(task) })
            }
        }
    }
}

@Composable
fun RewardListScreen(
    rewards: List<RewardItem>,
    currentScore: Int,
    navController: NavController,
    onRewardClick: (RewardItem) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppTopBar(
                title = "兑换商店",
                onBack = { navController.popBackStack() },
                actions = {
                    ScoreBadge(currentScore)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_reward") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reward")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(rewards) { reward ->
                RewardCard(
                    reward = reward,
                    onClick = {
                        onRewardClick(reward)
                        Toast.makeText(context, "兑换: ${reward.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ------------------------------------------
// Add Screens with TopBar Confirmation
// ------------------------------------------

@Composable
fun AddTaskScreen(
    navController: NavController,
    onAdd: (String, Int, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("10") }
    var selectedGradientIndex by remember { mutableIntStateOf(0) }

    // 确认添加逻辑
    fun submit() {
        if (name.isNotEmpty() && score.isNotEmpty()) {
            val (c1, c2) = getGradient(selectedGradientIndex)
            onAdd(name, score.toIntOrNull() ?: 0, c1, c2)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "新建任务",
                onBack = { navController.popBackStack() },
                actions = {
                    // 修改点：TopBar 右侧的确认按钮，直接调用 submit()
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Default.Check, "Confirm", tint = Color.Black)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("任务名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = score,
                onValueChange = { if (it.all { char -> char.isDigit() }) score = it },
                label = { Text("完成加分") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("选择任务卡片颜色", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickerRow(selectedGradientIndex) { selectedGradientIndex = it }
        }
    }
}

@Composable
fun AddRewardScreen(
    navController: NavController,
    onAdd: (String, Int, Long, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("50") }
    // 修改点：添加商品也需要颜色
    var selectedGradientIndex by remember { mutableIntStateOf(3) } // Default red-ish

    fun submit() {
        if (name.isNotEmpty() && cost.isNotEmpty()) {
            val (c1, c2) = getGradient(selectedGradientIndex)
            onAdd(name, cost.toIntOrNull() ?: 0, c1, c2)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "添加商品",
                onBack = { navController.popBackStack() },
                actions = {
                    // 修改点：TopBar 右侧的确认按钮
                    IconButton(onClick = { submit() }) {
                        Icon(Icons.Default.Check, "Confirm", tint = Color.Black)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("商品名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = cost,
                onValueChange = { if (it.all { char -> char.isDigit() }) cost = it },
                label = { Text("兑换消耗") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            // 修改点：添加颜色选择UI
            Text("选择商品外观颜色", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickerRow(selectedGradientIndex) { selectedGradientIndex = it }
        }
    }
}

// ==========================================
// 7. Cards
// ==========================================

@Composable
fun TaskCard(task: TaskItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(task.colorStartHex), Color(task.colorEndHex))
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = task.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            Text(
                text = "+${task.scoreGain}",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun RewardCard(reward: RewardItem, onClick: () -> Unit) {
    // 游戏化横向UI，利用颜色
    // 为了和 Task 区分，我们把颜色应用在边框或特殊区域，而不是整个背景
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左侧颜色条，展示商品稀有度/颜色
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(reward.colorStartHex), Color(reward.colorEndHex))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = reward.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "ITEM",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        letterSpacing = 2.sp
                    )
                }

                // 价格标签，使用该商品的颜色作为背景
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(reward.colorStartHex), Color(reward.colorEndHex))
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "-${reward.cost}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}