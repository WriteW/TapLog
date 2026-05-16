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
     */
    private fun createMaskTable(password: String): ByteArray {
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val table = ByteArray(BUFFER_SIZE)
        val passLen = passBytes.size

        // 【防御性修复】：防止极端情况下的空密码导致 i % 0 崩溃
        if (passLen == 0) return table

        for (i in 0 until BUFFER_SIZE) {
            table[i] = passBytes[i % passLen]
        }
        return table
    }

    // ==========================================
    // 极限优化的底层流引擎
    // ==========================================
    private class FastXorOutputStream(out: OutputStream, private val maskTable: ByteArray) : FilterOutputStream(out) {
        private var maskIndex = 0

        // 【核心修复】：分配专属缓冲区。严禁原地修改调用者的数组，保护 ZipOutputStream 的内部状态！
        private val xorBuffer = ByteArray(BUFFER_SIZE)

        override fun write(b: Int) {
            val mask = maskTable[maskIndex].toInt()
            maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            out.write(b xor mask)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var remaining = len
            var currentOff = off

            // 安全分块处理：将数据搬运到 xorBuffer 中进行加密，然后再写出
            while (remaining > 0) {
                val chunk = remaining.coerceAtMost(BUFFER_SIZE)
                for (i in 0 until chunk) {
                    xorBuffer[i] = (b[currentOff + i].toInt() xor maskTable[maskIndex].toInt()).toByte()
                    maskIndex = (maskIndex + 1) and FAST_MOD_MASK
                }
                out.write(xorBuffer, 0, chunk)
                currentOff += chunk
                remaining -= chunk
            }
        }
    }

    // InputStream 保持原样，因为 read 方法本身就是用来修改传入数组的，这是合法的契约
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
                val allFiles = dfsCollectFiles(sourceFolder)
                val rootPath = sourceFolder.absolutePath

                for (file in allFiles) {
                    if (file == sourceFolder) continue // 跳过根目录本身

                    val zipFileName = file.absolutePath.removePrefix(rootPath).removePrefix("/")
                    if (zipFileName.isNotEmpty()) {
                        val entry = ZipEntry(zipFileName + if (file.isDirectory) "/" else "")
                        it.putNextEntry(entry)

                        if (file.isFile) {
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