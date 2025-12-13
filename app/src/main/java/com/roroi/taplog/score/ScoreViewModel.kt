package com.roroi.taplog.score

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 改为继承 AndroidViewModel 以获取 Application Context
class ScoreViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val _taskScore = MutableStateFlow(TaskScore())
    val taskScore: StateFlow<TaskScore> = _taskScore.asStateFlow()

    init {
        // 初始化时加载积分
        loadScore()
    }

    private fun loadScore() {
        viewModelScope.launch {
            val savedScore = DataRepository.loadScore(context)
            _taskScore.value = savedScore
        }
    }

    private fun saveScore() {
        // 每次变动后保存
        // 注意：高频写入可能影响性能，生产环境建议使用 Debounce 或在 onPause 保存
        // 这里为了实时性直接保存
        viewModelScope.launch {
            DataRepository.saveScore(context, _taskScore.value)
        }
    }

    fun addIncome(income: Int) {
        _taskScore.update { current ->
            current.copy(dScore = current.dScore + income)
        }
        saveScore()
    }

    fun integrateScore(hardP: () -> Unit) {
        if (_taskScore.value.dScore == 0) return
        if (_taskScore.value.dScore < 0) {
            hardP()
            return
        }
        hardP()
        _taskScore.update { current ->
            current.copy(
                score = current.score + current.dScore,
                dScore = 0
            )
        }
        saveScore()
    }

    fun deleteTask(task: Task) {
        GlobalV.removeTask(task)
    }

    fun deleteGoods(goods: Goods) {
        GlobalV.removeGoods(goods)
    }

    fun resetScore() {
        // 将 score 和 dScore 都归零
        _taskScore.update {
            TaskScore(score = 0, dScore = 0)
        }
        saveScore() // 保存到本地
    }

    fun purchase(cost: Int): Boolean {
        val current = _taskScore.value
        if (current.score >= cost) {
            _taskScore.update {
                it.copy(score = it.score - cost)
            }
            saveScore()
            return true
        } else {
            addIncome(-cost)
            return false
        }
    }
}