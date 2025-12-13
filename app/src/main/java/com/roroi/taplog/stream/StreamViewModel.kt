package com.roroi.taplog.stream

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Stream @OptIn(ExperimentalUuidApi::class) constructor(
    val text: String,
    val uuid: Uuid = Uuid.random(),
    val originName: String // 显示用的名称，允许重名
)

const val PATH = "Stream"
const val STAMP_PATTERN = "yyMMdd_HHmmss"

class StreamViewModel : ViewModel() {
    private var roiCharacter = false // 是否是主人格

    // 当前编辑的文本状态（属性 setter 私有，外部通过 updateEpState 修改）
    var epState by mutableStateOf<TextFieldState?>(null)
        private set

    // 当前正在编辑的流的 uuid（为空表示新建）
    @OptIn(ExperimentalUuidApi::class)
    var currentStreamUuid by mutableStateOf<Uuid?>(null)
        private set

    // 所有流的列表（用于 UI 展示）
    val streamList = mutableStateListOf<Stream>()

    // Context 引用（属性 setter 私有，外部通过 updateContext 设置）
    private var context by mutableStateOf<Context?>(null)

    // 用于避免重复加载的标志
    private var isLoaded = false

    // 外部通过此函数设置 epState，避免与属性 setter 冲突
    fun updateEpState(value: TextFieldState) {
        epState = value
        if (!isLoaded) {
            loadAllStreams()
            isLoaded = true
        }
    }

    // 外部通过此函数设置 context，并自动加载列表（只加载一次）
    fun updateContext(context: Context) {
        this.context = context
    }

    // 设置当前编辑的流（由外部调用，例如点击列表项时）
    @OptIn(ExperimentalUuidApi::class)
    fun setCurrentStream(uuid: Uuid?) {
        currentStreamUuid = uuid
        if (uuid != null) {
            // 从 streamList 中找到对应的流，加载其文本到 epState
            val stream = streamList.find { it.uuid == uuid }
            stream?.let {
                epState?.setTextAndPlaceCursorAtEnd(it.text)
            }
        } else {
            // 新建流：清空编辑器
            epState?.setTextAndPlaceCursorAtEnd("")
        }
    }

    // 加载所有流文件到 streamList
    @OptIn(ExperimentalUuidApi::class)
    fun loadAllStreams() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val appPath = context?.getExternalFilesDir(null) ?: return@withContext
                val streamDir = File(appPath, PATH)
                if (!streamDir.exists() || !streamDir.isDirectory) return@withContext

                val files = streamDir.listFiles { file ->
                    file.isFile && file.extension.equals("stm", ignoreCase = true)
                }?.toList() ?: emptyList()

                val loadedStreams = files.mapNotNull { file ->
                    try {
                        Json.decodeFromString<Stream>(file.readText())
                    } catch (_: Exception) {
                        // 解析失败则跳过（可记录日志）
                        null
                    }
                }.sortedByDescending { it.originName } // 按名称倒序，或按文件修改时间

                withContext(Dispatchers.Main) {
                    streamList.clear()
                    streamList.addAll(loadedStreams)
                }
            }
        }
    }

    // 保存当前编辑的内容
    @OptIn(ExperimentalUuidApi::class)
    fun saveFile() {
        val ctx = context ?: return
        val state = epState ?: return
        val text = state.text.toString()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val appPath = ctx.getExternalFilesDir(null) ?: return@withContext
                    val streamDir = File(appPath, PATH)
                    streamDir.mkdirs() // 确保目录存在

                    // 确定要保存的 uuid 和文件名
                    val uuidToSave = currentStreamUuid ?: Uuid.random()
                    val file = File(streamDir, "$uuidToSave.stm")

                    // 确定 originName：如果是已有流，保留原名；否则生成时间戳
                    val originName = if (currentStreamUuid != null) {
                        streamList.find { it.uuid == uuidToSave }?.originName
                            ?: getCTimeStamp() // 理论上不会为空
                    } else {
                        getCTimeStamp() // 新建流使用时间戳作为默认名
                    }

                    val stream = Stream(text, uuid = uuidToSave, originName = originName)
                    val json = Json.encodeToString(stream)
                    file.writeText(json)

                    // 更新内存中的列表
                    withContext(Dispatchers.Main) {
                        val existingIndex = streamList.indexOfFirst { it.uuid == uuidToSave }
                        if (existingIndex >= 0) {
                            // 覆盖原有项
                            streamList[existingIndex] = stream
                        } else {
                            // 新增项
                            streamList.add(stream)
                            // 按需排序（例如按修改时间）
                            streamList.sortByDescending { it.originName }
                        }
                        // 更新当前 uuid
                        currentStreamUuid = uuidToSave
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    // 可以在这里显示错误提示，但简化起见只打印日志
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteStream(uuid: Uuid) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val ctx = context ?: return@withContext
                val appPath = ctx.getExternalFilesDir(null) ?: return@withContext
                val streamDir = File(appPath, PATH)
                val file = File(streamDir, "$uuid.stm")
                val deleted = file.delete() // 返回是否成功删除

                if (deleted) {
                    withContext(Dispatchers.Main) {
                        // 从列表中移除
                        streamList.removeAll { it.uuid == uuid }
                        // 如果当前编辑的就是这个流，重置
                        if (currentStreamUuid == uuid) {
                            currentStreamUuid = null
                            epState?.setTextAndPlaceCursorAtEnd("")
                        }
                    }
                } else {
                    // 文件不存在或删除失败，可记录日志
                    // 可以考虑从列表中强制移除，但文件可能已不存在
                    // 这里简单打印警告
                    println("Delete failed or file not found: $file")
                }
            }
        }
    }

    // 以下为原有功能，未修改
    fun changeC() {
        roiCharacter = !roiCharacter
        if (roiCharacter) {
            changeToL()
        } else {
            changeToR()
        }
    }

    fun changeToL() {
        epState?.setTextAndPlaceCursorAtEnd(epState?.text.toString() + "\n()")
        epState?.edit {
            selection = TextRange(epState!!.text.length - 1, epState!!.text.length - 1)
        }
    }

    fun changeToR() {
        epState?.setTextAndPlaceCursorAtEnd(epState!!.text.toString() + "\n")
    }

    fun checkChange() {
        epState?.let { state ->
            val text = epState!!.text.toString()
            val lastIndex = text.lastIndexOf("。。。")  // 找到最后一个三个句号的位置
            if (lastIndex != -1) {
                state.edit {
                    delete(lastIndex, lastIndex + 3)  // 删除这三个句号
                }
                changeC()
            }
        }
    }
}

// 以下为原有的工具函数，保持不变
data class ParenthesesMatch(
    val range: IntRange,
    val content: String
)

fun extractOuterParenthesesWithIndex(text: String): List<ParenthesesMatch> {
    val result = mutableListOf<ParenthesesMatch>()
    var depth = 0
    var start = -1
    for ((index, char) in text.withIndex()) {
        when (char) {
            '(' -> {
                if (depth == 0) start = index
                depth++
            }
            ')' -> {
                depth--
                if (depth == 0 && start != -1) {
                    result.add(ParenthesesMatch(start..index, text.substring(start..index)))
                    start = -1
                }
            }
        }
    }
    return result
}

fun getCurrentTimeSeconds(): Long = System.currentTimeMillis() / 1000

@SuppressLint("SimpleDateFormat")
fun getCTimeStamp(): String {
    val date = Date(getCurrentTimeSeconds() * 1000)
    return SimpleDateFormat(STAMP_PATTERN).format(date)
}