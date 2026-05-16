package com.roroi.taplog.daily.viewmodel.encryption

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object CryptoManager {
    const val SUFFIX = "xoroi"
    private const val HEADER_OBFUSCATION_MASK = 0x5A

    // 64KB 物理闪存页对齐 (2的16次方)
    private const val BUFFER_SIZE = 65536
    // 用于位运算快速求余的掩码 (65536 - 1)
    private const val FAST_MOD_MASK = 65535

    /**
     * 生成 64KB 预填充密码表
     * 空间换时间的终极杀器：把短密码循环平铺到 64KB 的内存中。
     * 这样在读写大数据时，就不需要每次去计算 password 的 index 了。
     */
    private fun createMaskTable(password: String): ByteArray {
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val table = ByteArray(BUFFER_SIZE)
        val passLen = passBytes.size
        for (i in 0 until BUFFER_SIZE) {
            table[i] = passBytes[i % passLen]
        }
        return table
    }

    // ==========================================
    // 极限优化的底层流引擎 (裸写字节流，零多余封装)
    // ==========================================
    private class FastXorOutputStream(out: OutputStream, private val maskTable: ByteArray) : FilterOutputStream(out) {
        private var maskIndex = 0

        override fun write(b: Int) {
            val mask = maskTable[maskIndex].toInt()
            // 利用位运算替代 if 判定或 % 运算，速度提升几十倍
            maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            out.write(b xor mask)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            // 直接在原数组上做异或，不分配新数组
            for (i in 0 until len) {
                b[off + i] = (b[off + i].toInt() xor maskTable[maskIndex].toInt()).toByte()
                maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            }
            // 整块扔给底层的 BufferedOutputStream
            out.write(b, off, len)
        }
    }

    private class FastXorInputStream(inputStream: InputStream, private val maskTable: ByteArray) : FilterInputStream(inputStream) {
        private var maskIndex = 0

        override fun read(): Int {
            val b = `in`.read()
            if (b == -1) return -1
            val mask = maskTable[maskIndex].toInt()
            maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            return b xor mask
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val bytesRead = `in`.read(b, off, len)
            if (bytesRead == -1) return -1
            for (i in 0 until bytesRead) {
                b[off + i] = (b[off + i].toInt() xor maskTable[maskIndex].toInt()).toByte()
                maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            }
            return bytesRead
        }
    }

    // ==========================================
    // 手写无 GC 开销的自定义 DFS 遍历
    // ==========================================
    private fun dfsCollectFiles(root: File): List<File> {
        val result = ArrayList<File>()
        val stack = ArrayDeque<File>()
        stack.addLast(root)

        while (stack.isNotEmpty()) {
            val curr = stack.removeLast()
            result.add(curr)
            if (curr.isDirectory) {
                val children = curr.listFiles()
                if (children != null) {
                    for (child in children) {
                        stack.addLast(child)
                    }
                }
            }
        }
        return result
    }

    fun lockSpacePipeline(sourceFolder: File, encryptedFile: File, password: String) {
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val maskTable = createMaskTable(password)

        // 分配物理对齐的公用缓冲区
        val ioBuffer = ByteArray(BUFFER_SIZE)

        BufferedOutputStream(FileOutputStream(encryptedFile), BUFFER_SIZE).use { bos ->
            // 写入密码头
            bos.write(passBytes.size)
            for (byte in passBytes) bos.write(byte.toInt() xor HEADER_OBFUSCATION_MASK)

            val xorOut = FastXorOutputStream(bos, maskTable)
            val zos = ZipOutputStream(xorOut).apply { setLevel(0) }

            zos.use {
                // 启用自定义 DFS，避免 walkTopDown 的过度对象分配
                val allFiles = dfsCollectFiles(sourceFolder)
                val rootPath = sourceFolder.absolutePath

                for (file in allFiles) {
                    if (file == sourceFolder) continue // 跳过根目录本身

                    val zipFileName = file.absolutePath.removePrefix(rootPath).removePrefix("/")
                    if (zipFileName.isNotEmpty()) {
                        val entry = ZipEntry(zipFileName + if (file.isDirectory) "/" else "")
                        it.putNextEntry(entry)

                        if (file.isFile) {
                            // 抛弃系统的 copyTo，手写极速通道
                            FileInputStream(file).use { fis ->
                                var bytesRead: Int
                                while (fis.read(ioBuffer).also { len -> bytesRead = len } != -1) {
                                    it.write(ioBuffer, 0, bytesRead)
                                }
                            }
                        }
                        it.closeEntry()
                    }
                }
            }
        }
    }

    fun unlockSpacePipeline(encryptedFile: File, targetFolder: File, password: String): Boolean {
        try {
            val maskTable = createMaskTable(password)
            val ioBuffer = ByteArray(BUFFER_SIZE)

            FileInputStream(encryptedFile).use { fis ->
                val bis = BufferedInputStream(fis, BUFFER_SIZE)
                val passLen = bis.read()
                if (passLen == -1) return false
                val realPassBytes = ByteArray(passLen)
                if (bis.read(realPassBytes) != passLen) return false
                for (i in realPassBytes.indices) {
                    realPassBytes[i] = (realPassBytes[i].toInt() xor HEADER_OBFUSCATION_MASK).toByte()
                }

                val realPassword = String(realPassBytes, Charsets.UTF_8)
                if (password != realPassword) return false

                val xorIn = FastXorInputStream(bis, maskTable)
                val zis = ZipInputStream(xorIn)

                zis.use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val newFile = File(targetFolder, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            // 手写高速写出
                            FileOutputStream(newFile).use { fos ->
                                var bytesRead: Int
                                while (zipIn.read(ioBuffer).also { len -> bytesRead = len } != -1) {
                                    fos.write(ioBuffer, 0, bytesRead)
                                }
                            }
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("CryptoManager", "解密失败或文件损坏", e)
            return false
        }
    }
}

suspend fun unlockAndEnter(context: Context, inputPassword: String, spaceId: String): Boolean {
    // 保持原有逻辑不变
    return withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")
        val encryptedFile = File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}")
        val targetFolder = File(baseDir, spaceId)

        if (!encryptedFile.exists()) {
            targetFolder.mkdirs()
            return@withContext true
        }

        val success = CryptoManager.unlockSpacePipeline(encryptedFile, targetFolder, inputPassword)
        if (success) {
            if (encryptedFile.exists()) encryptedFile.delete()
        }
        return@withContext success
    }
}

suspend fun lockAndExit(context: Context, userPassword: String, spaceId: String) {
    // 保持原有逻辑不变
    withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")
        val sourceFolder = File(baseDir, spaceId)
        val finalEncryptedFile = File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}")

        if (!sourceFolder.exists() || sourceFolder.listFiles()?.isEmpty() == true) return@withContext

        try {
            CryptoManager.lockSpacePipeline(sourceFolder, finalEncryptedFile, userPassword)
            sourceFolder.deleteRecursively()
        } catch (e: Exception) {
            Log.e("App", "上锁失败！", e)
        }
    }
}