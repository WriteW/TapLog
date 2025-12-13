package com.roroi.taplog.score

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates

// 使用 @SuppressLint 忽略静态 Context 警告
// 在实际生产中，建议通过 Hilt 注入或 Application 类管理，这里为了保持项目结构简单使用静态引用
@SuppressLint("StaticFieldLeak")
object GlobalV {
    // 列表依然使用 SnapshotStateList 以支持 Compose 响应式
    val taskList = mutableStateListOf<Task>()
    val goodsList = mutableStateListOf<Goods>()

    // UI 坐标相关

    private var appContext: Context? = null

    // 初始化：在 Activity onCreate 时调用
    fun init(context: Context) {
        appContext = context.applicationContext
        loadAll()
    }

    private fun loadAll() {
        val context = appContext ?: return

        // 加载任务
        taskList.clear()
        taskList.addAll(DataRepository.loadTasks(context))

        // 加载商品
        goodsList.clear()
        goodsList.addAll(DataRepository.loadGoods(context))
    }

    // --- 操作封装 (自动保存) ---

    fun addTask(task: Task) {
        taskList.add(task)
        saveTasks()
    }

    fun addGoods(goods: Goods) {
        goodsList.add(goods)
        saveGoods()
    }

    fun removeTask(task: Task) {
        taskList.remove(task)
        saveTasks() // 记得保存更改到文件
    }

    fun removeGoods(goods: Goods) {
        goodsList.remove(goods)
        saveGoods() // 记得保存更改到文件
    }

    private fun saveTasks() {
        appContext?.let { DataRepository.saveTasks(it, taskList) }
    }

    private fun saveGoods() {
        appContext?.let { DataRepository.saveGoods(it, goodsList) }
    }
}