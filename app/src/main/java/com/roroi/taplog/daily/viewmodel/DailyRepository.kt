package com.roroi.taplog.daily.viewmodel

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DailyRepository(private val context: Context) {
    private val baseDir = File(context.getExternalFilesDir(null), "daily")
    private val imageDir = File(baseDir, "image")
    private val spaceDir = File(baseDir, "spaces")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        createDirs()
    }

    private fun createDirs() {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!imageDir.exists()) imageDir.mkdirs()
        if (!spaceDir.exists()) spaceDir.mkdirs()
    }
    /**
     * 移动纯文本条目到指定空间
     */
    suspend fun moveTextEntry(originalSpaceId: String? = null, targetSpaceId: String?, entry: DailyEntry) = withContext(Dispatchers.IO) {
        try {
            // 1. 在目标空间保存该条目
            // saveEntry 内部会自动调用 createDirs() 确保目标目录存在
            saveEntry(entry, targetSpaceId)

            // 2. 检查新文件是否已经成功写入
            val newEntryFile = File(getDSpaceDir(targetSpaceId), "${entry.id}.json")
            if (newEntryFile.exists()) {
                // 3. 只有确认新文件存在，才删除旧空间的 JSON 文件
                val oldEntryFile = File(getDSpaceDir(originalSpaceId), "${entry.id}.json")
                if (oldEntryFile.exists()) {
                    oldEntryFile.delete()
                }
            } else {
                throw Exception("Failed to write entry to target space")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e // 向上传递异常，方便 ViewModel 捕获并提示用户
        }
    }
    suspend fun moveImageEntry(originalSpaceId: String? = null, targetSpaceId: String?, entry: DailyEntry) = withContext(Dispatchers.IO) {
        val oldImageFile = File(getDSpaceImageDir(originalSpaceId), entry.content)
        val targetImageDir = getDSpaceImageDir(targetSpaceId)
        val targetImageFile = File(targetImageDir, entry.content)

        // 1. 确保目标目录存在
        if (!targetImageDir.exists()) targetImageDir.mkdirs()

        // 2. 移动图片文件 (建议使用带失败检测的逻辑)
        val moveSuccessful = if (oldImageFile.exists()) {
            if (!oldImageFile.renameTo(targetImageFile)) {
                // 如果 rename 失败 (跨分区等原因)，执行复制+删除
                try {
                    oldImageFile.copyTo(targetImageFile, overwrite = true)
                    oldImageFile.delete()
                } catch (_: Exception) {
                    false
                }
            } else true
        } else false

        if (moveSuccessful) {
            // 3. 只有图片移动成功，才保存新的 Entry
            saveEntry(entry, targetSpaceId)

            // 4. 正确删除旧的 JSON 文件 (注意：JSON 在 DSpaceDir，不在 ImageDir)
            val oldEntryFile = File(getDSpaceDir(originalSpaceId), "${entry.id}.json")
            if (oldEntryFile.exists()) {
                oldEntryFile.delete()
            }
        } else {
            // 可以在这里抛出异常或返回失败，通知 UI 层图片移动失败
            throw Exception("Failed to move image file")
        }
    }
    fun createSpaceDir(dspaceId: String) {
        val dspaceDir = baseDir.resolve(dspaceId)
        if (dspaceDir.exists()) return
        dspaceDir.mkdirs()
        dspaceDir.resolve("image").mkdirs()
    }

    fun saveSpace(dspace: DSpace) {
        createSpaceDir(dspace.id)
        val file = spaceDir.resolve("${dspace.id}.sp")
        file.writeText(json.encodeToString(dspace))
    }

    suspend fun loadSpace(): List<DSpace> = withContext(Dispatchers.IO) {
        val files = spaceDir.listFiles { _, name -> name.endsWith(".sp") } ?: return@withContext emptyList()
        files.mapNotNull { file ->
            try {
                val space = json.decodeFromString<DSpace>(file.readText())

                // === 【核心迁移逻辑】：旧明文密码转为 XOR 加密文件 ===
                if (space.password.isNotEmpty() && !space.isEncrypted) {
                    getDSpaceDir(space.id)
                    // 使用旧密码加密当前明文文件夹
                    com.roroi.taplog.daily.viewmodel.encryption.lockAndExit(
                        context, space.password, space.id
                    )
                    // 修改元数据：标记为已加密，彻底清空明文密码
                    val migratedSpace = space.copy(isEncrypted = true, password = "")
                    file.writeText(json.encodeToString(migratedSpace)) // 覆写保存
                    migratedSpace
                } else {
                    space
                }
                // ===============================================

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun delSpaceData(dspaceId: String) = withContext(Dispatchers.IO) {
        clearAllData(dspaceId)
        // 删除元数据文件
        val metaFile = File(spaceDir, "$dspaceId.sp")
        metaFile.delete()
    }

    fun getDSpaceDir(dspaceId: String?): File =
        dspaceId?.let { baseDir.resolve(dspaceId) } ?: baseDir

    fun getDSpaceImageDir(dspaceId: String?): File = getDSpaceDir(dspaceId).resolve("image")
    suspend fun getAllEntries(dspaceId: String? = null): List<DailyEntry> =
        withContext(Dispatchers.IO) {
            if (!baseDir.exists()) return@withContext emptyList()
            val files = getDSpaceDir(dspaceId).listFiles { _, name -> name.endsWith(".json") }
                ?: return@withContext emptyList()
            files.mapNotNull { file ->
                try {
                    json.decodeFromString<DailyEntry>(file.readText())
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }.sortedByDescending { it.timestamp }
        }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun saveEntry(entry: DailyEntry, dspaceId: String? = null) =
        withContext(Dispatchers.IO) {
            createDirs()
            val file = File(getDSpaceDir(dspaceId), "${entry.id}.json")
            file.writeText(json.encodeToString(entry))
        }

    suspend fun deleteEntry(entry: DailyEntry, dspaceId: String? = null) =
        withContext(Dispatchers.IO) {
            val jsonFile = File(getDSpaceDir(dspaceId), "${entry.id}.json")
            if (jsonFile.exists()) jsonFile.delete()

            if (entry.type == EntryType.IMAGE) {
                val imgFile = File(getDSpaceImageDir(dspaceId), entry.content)
                if (imgFile.exists()) imgFile.delete()
            }
        }

    suspend fun saveImage(uri: Uri, dspaceId: String? = null): String =
        withContext(Dispatchers.IO) {
            createDirs()
            val fileName = "${UUID.randomUUID()}.jpg"
            val destFile = File(getDSpaceImageDir(dspaceId), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            fileName
        }

    fun getImagePath(fileName: String, dspaceId: String?): File {
        return File(getDSpaceImageDir(dspaceId), fileName)
    }

    // --- 数据管理功能 ---

    suspend fun clearAllData(dspaceId: String? = null) = withContext(Dispatchers.IO) {
        getDSpaceDir(dspaceId).deleteRecursively()
        createDirs()
    }

    // 修改：导出到指定的 Uri
    suspend fun exportDataToUri(uri: Uri, dspaceId: String? = null) = withContext(Dispatchers.IO) {
        // 使用 ContentResolver 打开输出流
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                zipFolder(getDSpaceDir(dspaceId), getDSpaceDir(dspaceId), zos)
            }
        }
    }

    private fun zipFolder(rootDir: File, sourceFile: File, zos: ZipOutputStream) {
        if (sourceFile.isDirectory) {
            val files = sourceFile.listFiles() ?: return
            for (file in files) {
                zipFolder(rootDir, file, zos)
            }
        } else {
            // 排除临时文件或系统生成的 zip (如果有的话)
            if (sourceFile.name.endsWith(".zip")) return

            // 计算相对路径 (例如: image/abc.jpg 或 uuid.json)
            val relativePath = sourceFile.relativeTo(rootDir).path
            val entry = ZipEntry(relativePath)
            zos.putNextEntry(entry)
            FileInputStream(sourceFile).use { fis ->
                fis.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    suspend fun importDataPackage(uri: Uri, dspaceId: String? = null) =
        withContext(Dispatchers.IO) {
            // 先清空现有数据
            getDSpaceDir(dspaceId).deleteRecursively()
            createDirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val filePath = File(getDSpaceDir(dspaceId), entry!!.name)
                        // 防止 Zip Slip 漏洞 (检查路径是否还在 currentTargetDir 内)
                        if (!filePath.canonicalPath.startsWith(getDSpaceDir(dspaceId).canonicalPath)) {
                            throw SecurityException("Invalid zip entry: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            filePath.mkdirs()
                        } else {
                            filePath.parentFile?.mkdirs()
                            FileOutputStream(filePath).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                    }
                }
            }
        }

    suspend fun loadTimeCapsules(dspaceId: String? = null): List<TimeCapsule> = withContext(Dispatchers.IO) {
        val file = File(getDSpaceDir(dspaceId), "capsules.json")
        if (!file.exists()) return@withContext emptyList()
        try {
            json.decodeFromString(file.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTimeCapsules(capsules: List<TimeCapsule>, dspaceId: String? = null) = withContext(Dispatchers.IO) {
        createDirs()
        val file = File(getDSpaceDir(dspaceId), "capsules.json")
        file.writeText(json.encodeToString(capsules))
    }
}