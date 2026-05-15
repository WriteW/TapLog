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
    const val SUFFIX  = "xoroi"
    private const val HEADER_OBFUSCATION_MASK = 0x5A

    // 性能优化核心：全局复用的内存块，彻底消除 GC 垃圾回收卡顿
    private class XorOutputStream(out: OutputStream, private val passBytes: ByteArray) : FilterOutputStream(out) {
        private var passIndex = 0
        private val chunkBuffer = ByteArray(8192) // 复用缓冲区，拒绝反复 New 对象

        override fun write(b: Int) {
            val mask = passBytes[passIndex % passBytes.size].toInt()
            passIndex++
            super.out.write(b xor mask)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var remaining = len
            var currentOff = off
            while (remaining > 0) {
                val chunk = Math.min(remaining, chunkBuffer.size)
                for (i in 0 until chunk) {
                    val mask = passBytes[passIndex % passBytes.size].toInt()
                    passIndex++
                    chunkBuffer[i] = (b[currentOff + i].toInt() xor mask).toByte()
                }
                super.out.write(chunkBuffer, 0, chunk)
                currentOff += chunk
                remaining -= chunk
            }
        }
    }

    private class XorInputStream(inputStream: InputStream, private val passBytes: ByteArray) : FilterInputStream(inputStream) {
        private var passIndex = 0
        override fun read(): Int {
            val b = super.`in`.read()
            if (b == -1) return -1
            val mask = passBytes[passIndex % passBytes.size].toInt()
            passIndex++
            return b xor mask
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val bytesRead = super.`in`.read(b, off, len)
            if (bytesRead == -1) return -1
            // 直接在原数组上做异或修改，极速且零内存分配
            for (i in 0 until bytesRead) {
                val mask = passBytes[passIndex % passBytes.size].toInt()
                passIndex++
                b[off + i] = (b[off + i].toInt() xor mask).toByte()
            }
            return bytesRead
        }
    }

    fun lockSpacePipeline(sourceFolder: File, encryptedFile: File, password: String) {
        val passBytes = password.toByteArray(Charsets.UTF_8)
        BufferedOutputStream(FileOutputStream(encryptedFile)).use { bos ->
            bos.write(passBytes.size)
            for (byte in passBytes) bos.write(byte.toInt() xor HEADER_OBFUSCATION_MASK)

            val xorOut = XorOutputStream(bos, passBytes)
            val zos = ZipOutputStream(xorOut).apply { setLevel(0) } // setLevel(0) 彻底关闭压缩，速度起飞

            zos.use {
                sourceFolder.walkTopDown().forEach { file ->
                    val zipFileName = file.absolutePath.removePrefix(sourceFolder.absolutePath).removePrefix("/")
                    if (zipFileName.isNotEmpty()) {
                        val entry = ZipEntry(zipFileName + if (file.isDirectory) "/" else "")
                        it.putNextEntry(entry)
                        if (file.isFile) {
                            file.inputStream().use { fis -> fis.copyTo(it) }
                        }
                        it.closeEntry()
                    }
                }
            }
        }
    }

    fun unlockSpacePipeline(encryptedFile: File, targetFolder: File, password: String): Boolean {
        try {
            FileInputStream(encryptedFile).use { fis ->
                val bos = BufferedInputStream(fis)
                val passLen = bos.read()
                if (passLen == -1) return false
                val realPassBytes = ByteArray(passLen)
                if (bos.read(realPassBytes) != passLen) return false
                for (i in realPassBytes.indices) {
                    realPassBytes[i] = (realPassBytes[i].toInt() xor HEADER_OBFUSCATION_MASK).toByte()
                }
                val realPassword = String(realPassBytes, Charsets.UTF_8)
                if (password != realPassword) return false

                val passBytes = realPassword.toByteArray(Charsets.UTF_8)
                val xorIn = XorInputStream(bos, passBytes)
                val zis = ZipInputStream(xorIn)

                zis.use {
                    var entry: ZipEntry? = it.nextEntry
                    while (entry != null) {
                        val newFile = File(targetFolder, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos -> it.copyTo(fos) }
                        }
                        entry = it.nextEntry
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("CryptoManager", "解密损坏", e)
            return false
        }
    }
}

suspend fun unlockAndEnter(context: Context, inputPassword: String, spaceId: String): Boolean {
    return withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")
        val encryptedFile = File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}")

        // 【关键修复】：解密目标文件夹必须是真实的 spaceId，不能是写死的字符
        val targetFolder = File(baseDir, spaceId)

        if (!encryptedFile.exists()) {
            targetFolder.mkdirs()
            return@withContext true
        }

        val success = CryptoManager.unlockSpacePipeline(encryptedFile, targetFolder, inputPassword)
        if (success) {
            if (encryptedFile.exists()) encryptedFile.delete() // 阅后即焚
            Log.d("App", "解锁成功！")
        } else {
            Log.w("App", "密码错误！")
        }
        return@withContext success
    }
}

suspend fun lockAndExit(context: Context, userPassword: String, spaceId: String) {
    withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")

        // 【关键修复】：加密源文件夹必须是真实的 spaceId，不能是写死的字符
        val sourceFolder = File(baseDir, spaceId)
        val finalEncryptedFile = File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}")

        if (!sourceFolder.exists() || sourceFolder.listFiles()?.isEmpty() == true) {
            return@withContext
        }

        try {
            CryptoManager.lockSpacePipeline(sourceFolder, finalEncryptedFile, userPassword)
            // 加密完成后彻底清空明文目录
            sourceFolder.deleteRecursively()
            Log.d("App", "上锁完成！明文数据已擦除。")
        } catch (e: Exception) {
            Log.e("App", "上锁失败！", e)
        }
    }
}