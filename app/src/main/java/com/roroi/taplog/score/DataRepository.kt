package com.roroi.taplog.score

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object DataRepository {
    private const val DIR_NAME = "score"
    private const val TASKS_FILE = "tasks.json"
    private const val GOODS_FILE = "goods.json"
    private const val SCORE_FILE = "score.json"

    // 配置 Json 实例：忽略未知键（防止版本升级崩坏），格式化输出
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private fun getDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // --- Task ---
    fun saveTasks(context: Context, tasks: List<Task>) {
        val file = File(getDir(context), TASKS_FILE)
        val jsonString = json.encodeToString(tasks)
        file.writeText(jsonString)
    }

    fun loadTasks(context: Context): List<Task> {
        val file = File(getDir(context), TASKS_FILE)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Task>>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Goods ---
    fun saveGoods(context: Context, goods: List<Goods>) {
        val file = File(getDir(context), GOODS_FILE)
        val jsonString = json.encodeToString(goods)
        file.writeText(jsonString)
    }

    fun loadGoods(context: Context): List<Goods> {
        val file = File(getDir(context), GOODS_FILE)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Goods>>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Score ---
    fun saveScore(context: Context, score: TaskScore) {
        val file = File(getDir(context), SCORE_FILE)
        val jsonString = json.encodeToString(score)
        file.writeText(jsonString)
    }

    fun loadScore(context: Context): TaskScore {
        val file = File(getDir(context), SCORE_FILE)
        if (!file.exists()) return TaskScore()
        return try {
            json.decodeFromString<TaskScore>(file.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            TaskScore()
        }
    }
}