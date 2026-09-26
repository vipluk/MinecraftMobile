package com.pearium.minecraftmobile.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class JavaRuntimeManager(private val context: Context) {

    val jreDir: File
        get() = File(context.filesDir, "jre")

    fun findJavaExecutable(): File? {
        val direct = File(jreDir, "bin/java")
        if (direct.exists()) return direct

        val usrBin = File(jreDir, "usr/bin/java")
        if (usrBin.exists()) return usrBin

        return jreDir.walkTopDown().firstOrNull { it.isFile && it.name == "java" }
    }

    fun isJavaInstalled(): Boolean {
        val exe = findJavaExecutable()
        return exe != null && exe.exists() && exe.length() > 0
    }

    fun getExecutablePath(): String {
        val exe = findJavaExecutable()
        if (exe != null && exe.exists()) {
            makeExecutable(exe)
            return exe.absolutePath
        }
        return "java"
    }

    fun getJavaHomeDir(): File {
        val exe = findJavaExecutable()
        return exe?.parentFile?.parentFile ?: jreDir
    }

    fun makeExecutable(file: File) {
        file.setExecutable(true, false)
        file.setReadable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
        } catch (ignored: Exception) {}
    }

    suspend fun installJavaRuntime(
        downloadUrl: String = DEFAULT_JRE_ARM64_URL,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()
        val tempZip = File(context.cacheDir, "jre21_arm64.zip")

        try {
            onProgress(0, "Łączenie z serwerem OpenJDK 21...")
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                onProgress(-1, "Błąd pobierania Java: HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            val mbRead = totalRead / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(percent, "Pobieranie Java 21: $mbRead MB / $mbTotal MB ($percent%)")
                        }
                    }
                }
            }

            onProgress(95, "Rozpakowywanie środowiska Java 21...")
            jreDir.mkdirs()
            unzip(tempZip, jreDir)
            tempZip.delete()

            // Nadaj uprawnienia wykonywalne dla wszystkich binariów i bibliotek
            grantExecutionPermissions(jreDir)

            onProgress(100, "Środowisko Java 21 gotowe!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd instalacji Java: ${e.localizedMessage}")
            false
        }
    }

    private fun grantExecutionPermissions(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.isDirectory) {
                file.setExecutable(true, false)
                file.setReadable(true, false)
            } else if (file.parentFile?.name == "bin" || file.name == "java" || file.name.endsWith(".so")) {
                makeExecutable(file)
            }
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        targetDirectory.mkdirs()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        val buffer = ByteArray(32 * 1024)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                    if (newFile.parentFile?.name == "bin" || newFile.name == "java" || newFile.name.endsWith(".so")) {
                        makeExecutable(newFile)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    companion object {
        // Stabilny headless OpenJDK 21 dla architektury ARM64 na Androidzie (zip)
        const val DEFAULT_JRE_ARM64_URL = "https://github.com/zryyoung/openjdk-Termux/releases/download/openjdk-21.0.1/openjdk-21.0.1-aarch64.zip"
    }
}
