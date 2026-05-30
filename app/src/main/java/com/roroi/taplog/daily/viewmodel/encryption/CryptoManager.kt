package com.roroi.taplog.daily.viewmodel.encryption

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object CryptoManager {
    const val SUFFIX = "xoroi"
    private const val HEADER_OBFUSCATION_MASK = 0x5A
    private const val BUFFER_SIZE = 65536
    private const val FAST_MOD_MASK = 65535

    private fun createMaskTable(password: String): ByteArray {
        val passBytes = password.toByteArray(Charsets.UTF_8)
        val table = ByteArray(BUFFER_SIZE)
        val passLen = passBytes.size
        if (passLen == 0) return table
        for (i in 0 until BUFFER_SIZE) {
            table[i] = passBytes[i % passLen]
        }
        return table
    }

    private class FastXorOutputStream(out: OutputStream, private val maskTable: ByteArray) :
        FilterOutputStream(out) {
        private var maskIndex = 0
        private val xorBuffer = ByteArray(BUFFER_SIZE)
        override fun write(b: Int) {
            val mask = maskTable[maskIndex].toInt()
            maskIndex = (maskIndex + 1) and FAST_MOD_MASK
            out.write(b xor mask)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            var remaining = len
            var currentOff = off
            while (remaining > 0) {
                val chunk = remaining.coerceAtMost(BUFFER_SIZE)
                for (i in 0 until chunk) {
                    xorBuffer[i] =
                        (b[currentOff + i].toInt() xor maskTable[maskIndex].toInt()).toByte()
                    maskIndex = (maskIndex + 1) and FAST_MOD_MASK
                }
                out.write(xorBuffer, 0, chunk)
                currentOff += chunk
                remaining -= chunk
            }
        }
    }

    private class FastXorInputStream(inputStream: InputStream, private val maskTable: ByteArray) :
        FilterInputStream(inputStream) {
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
                    for (child in children) stack.addLast(child)
                }
            }
        }
        return result
    }

    // [修改6] 新的逐文件加密管道
    fun lockSpacePipelineNew(sourceFolder: File, targetDir: File, password: String, encryptImages: Boolean, encryptAudio: Boolean) {
        targetDir.mkdirs()
        val allFiles = dfsCollectFiles(sourceFolder)
        val rootPath = sourceFolder.absolutePath
        val maskTable = createMaskTable(password)
        val passBytes = password.toByteArray(Charsets.UTF_8)

        for (file in allFiles) {
            if (file == sourceFolder) continue
            val relPath = file.absolutePath.removePrefix(rootPath).removePrefix("/")
            if (file.isDirectory) {
                File(targetDir, relPath).mkdirs()
                continue
            }

            // 判定是否免加密
            val isImage = file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".jpeg")
            val isAudio = file.name.endsWith(".mp3") || file.name.endsWith(".m4a") || file.name.endsWith(".wav") || file.name.endsWith(".ogg")

            val skipEncryption = (isImage && !encryptImages) || (isAudio && !encryptAudio)

            if (skipEncryption) {
                // 不加密选项打开时，媒体文件直接复制
                val targetFile = File(targetDir, relPath)
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            } else {
                // 加密文件，后缀追加 .xoroi
                val targetFile = File(targetDir, "$relPath.$SUFFIX")
                targetFile.parentFile?.mkdirs()
                BufferedOutputStream(FileOutputStream(targetFile), BUFFER_SIZE).use { bos ->
                    bos.write(passBytes.size)
                    for (byte in passBytes) bos.write(byte.toInt() xor HEADER_OBFUSCATION_MASK)
                    val xorOut = FastXorOutputStream(bos, maskTable)
                    FileInputStream(file).use { fis ->
                        val ioBuffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (fis.read(ioBuffer).also { bytesRead = it } != -1) {
                            xorOut.write(ioBuffer, 0, bytesRead)
                        }
                    }
                }
            }
        }
    }

    // [修改6] 新的逐文件解密管道
    fun unlockSpacePipelineNew(encryptedDir: File, targetFolder: File, password: String): Boolean {
        val maskTable = createMaskTable(password)
        val allFiles = dfsCollectFiles(encryptedDir)
        val rootPath = encryptedDir.absolutePath

        for (file in allFiles) {
            if (file == encryptedDir) continue
            val relPath = file.absolutePath.removePrefix(rootPath).removePrefix("/")
            if (file.isDirectory) {
                File(targetFolder, relPath).mkdirs()
                continue
            }

            if (!file.name.endsWith(SUFFIX)) {
                // 没有加密后缀，直接拷贝
                val targetFile = File(targetFolder, relPath)
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            } else {
                // 有加密后缀，解密并去除后缀
                val actualRelPath = relPath.removeSuffix(".$SUFFIX")
                val targetFile = File(targetFolder, actualRelPath)
                targetFile.parentFile?.mkdirs()
                try {
                    FileInputStream(file).use { fis ->
                        val bis = BufferedInputStream(fis, BUFFER_SIZE)
                        val passLen = bis.read()
                        if (passLen == -1) return false
                        val realPassBytes = ByteArray(passLen)
                        if (bis.read(realPassBytes) != passLen) return false
                        for (i in realPassBytes.indices) {
                            realPassBytes[i] =
                                (realPassBytes[i].toInt() xor HEADER_OBFUSCATION_MASK).toByte()
                        }
                        if (password != String(realPassBytes, Charsets.UTF_8)) return false

                        val xorIn = FastXorInputStream(bis, maskTable)
                        FileOutputStream(targetFile).use { fos ->
                            val ioBuffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (xorIn.read(ioBuffer).also { bytesRead = it } != -1) fos.write(
                                ioBuffer,
                                0,
                                bytesRead
                            )
                        }
                    }
                } catch (_: Exception) {
                    return false
                }
            }
        }
        return true
    }

    // 旧版单文件Zip兼容
    fun unlockSpacePipelineOld(encryptedFile: File, targetFolder: File, password: String): Boolean {
        try {
            val maskTable = createMaskTable(password)
            val ioBuffer = ByteArray(BUFFER_SIZE)

            FileInputStream(encryptedFile).use { fis ->
                val bis = BufferedInputStream(fis, BUFFER_SIZE)
                val passLen = bis.read()
                if (passLen == -1) return false
                val realPassBytes = ByteArray(passLen)
                if (bis.read(realPassBytes) != passLen) return false
                for (i in realPassBytes.indices) realPassBytes[i] =
                    (realPassBytes[i].toInt() xor HEADER_OBFUSCATION_MASK).toByte()
                if (password != String(realPassBytes, Charsets.UTF_8)) return false

                val xorIn = FastXorInputStream(bis, maskTable)
                ZipInputStream(xorIn).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(targetFolder, entry.name)
                        if (entry.isDirectory) newFile.mkdirs()
                        else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                var bytesRead: Int
                                while (zis.read(ioBuffer)
                                        .also { len -> bytesRead = len } != -1
                                ) fos.write(ioBuffer, 0, bytesRead)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }
}

// [修改6] 解密入口兼容新老数据
suspend fun unlockAndEnter(context: Context, inputPassword: String, spaceId: String): Boolean {
    return withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")
        val encryptedFileOld = File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}")
        val encryptedDirNew = File(baseDir, "${spaceId}_enc")
        val targetFolder = File(baseDir, spaceId)

        if (encryptedFileOld.exists()) {
            val success =
                CryptoManager.unlockSpacePipelineOld(encryptedFileOld, targetFolder, inputPassword)
            if (success) encryptedFileOld.delete() // 成功解密后删除旧版
            return@withContext success
        } else if (encryptedDirNew.exists()) {
            val success =
                CryptoManager.unlockSpacePipelineNew(encryptedDirNew, targetFolder, inputPassword)
            if (success) encryptedDirNew.deleteRecursively()
            return@withContext success
        } else {
            targetFolder.mkdirs()
            return@withContext true
        }
    }
}

// [修改6] 加密出口只生成新版格式
suspend fun lockAndExit(context: Context, userPassword: String, spaceId: String, encryptImages: Boolean = true, encryptAudio: Boolean = true) {
    withContext(Dispatchers.IO) {
        val baseDir = File(context.getExternalFilesDir(null), "daily")
        val sourceFolder = File(baseDir, spaceId)
        val finalEncryptedDir = File(baseDir, "${spaceId}_enc")

        if (!sourceFolder.exists() || sourceFolder.listFiles()?.isEmpty() == true) return@withContext
        try {
            CryptoManager.lockSpacePipelineNew(sourceFolder, finalEncryptedDir, userPassword, encryptImages, encryptAudio)
            sourceFolder.deleteRecursively()
            // 防御性清理可能残留的旧文件
            File(baseDir, "${spaceId}.${CryptoManager.SUFFIX}").delete()
        } catch (e: Exception) {
            Log.e("App", "上锁失败！", e)
        }
    }
}