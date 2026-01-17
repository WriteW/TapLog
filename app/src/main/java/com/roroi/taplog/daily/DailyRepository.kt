package com.roroi.taplog.daily

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DailyRepository(private val context: Context) {

    private val baseDir = File(context.getExternalFilesDir(null), "daily")
    private val imageDir = File(baseDir, "image")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        createDirs()
    }

    private fun createDirs() {
        if (!baseDir.exists()) baseDir.mkdirs()
        if (!imageDir.exists()) imageDir.mkdirs()
    }

    suspend fun getAllEntries(): List<DailyEntry> = withContext(Dispatchers.IO) {
        if (!baseDir.exists()) return@withContext emptyList()
        val files = baseDir.listFiles { _, name -> name.endsWith(".json") } ?: return@withContext emptyList()
        files.mapNotNull { file ->
            try {
                json.decodeFromString<DailyEntry>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    suspend fun saveEntry(entry: DailyEntry) = withContext(Dispatchers.IO) {
        createDirs()
        val file = File(baseDir, "${entry.id}.json")
        file.writeText(json.encodeToString(entry))
    }

    suspend fun deleteEntry(entry: DailyEntry) = withContext(Dispatchers.IO) {
        val jsonFile = File(baseDir, "${entry.id}.json")
        if (jsonFile.exists()) jsonFile.delete()

        if (entry.type == EntryType.IMAGE) {
            val imgFile = File(imageDir, entry.content)
            if (imgFile.exists()) imgFile.delete()
        }
    }

    suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
        createDirs()
        val fileName = "${java.util.UUID.randomUUID()}.jpg"
        val destFile = File(imageDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        fileName
    }

    fun getImagePath(fileName: String): File {
        return File(imageDir, fileName)
    }

    // --- 数据管理功能 ---

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        baseDir.deleteRecursively()
        createDirs()
    }

    // 修改：导出到指定的 Uri
    suspend fun exportDataToUri(uri: Uri) = withContext(Dispatchers.IO) {
        // 使用 ContentResolver 打开输出流
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                zipFolder(baseDir, baseDir, zos)
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

    suspend fun importDataPackage(uri: Uri) = withContext(Dispatchers.IO) {
        // 先清空现有数据
        baseDir.deleteRecursively()
        createDirs()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val filePath = File(baseDir, entry!!.name)
                    // 防止 Zip Slip 漏洞 (检查路径是否还在 baseDir 内)
                    if (!filePath.canonicalPath.startsWith(baseDir.canonicalPath)) {
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
}