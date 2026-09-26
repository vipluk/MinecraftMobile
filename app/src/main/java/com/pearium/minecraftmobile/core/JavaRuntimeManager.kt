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
        if (exe == null || !exe.exists() || exe.length() == 0L) return false
        val versionFile = File(jreDir, "version.txt")
        if (!versionFile.exists()) return false
        return versionFile.readText().trim() == "25"
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
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val urlsToTry = listOf(PRIMARY_JRE_ARM64_URL, FALLBACK_JRE_ARM64_URL)
        val tempZip = File(context.cacheDir, "jre25_arm64.zip")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build()

        var downloaded = false
        for ((index, url) in urlsToTry.withIndex() ) {
            try {
                val mirrorName = if (index == 0) "GitHub CDN" else "Serwer Relay GCP"
                onProgress(0, "Łączenie z serwerem OpenJDK 25 ($mirrorName)...")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                    .build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    continue
                }

                val body = response.body ?: continue
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
                                onProgress(percent, "Pobieranie Java 25 ($mirrorName): $mbRead MB / $mbTotal MB ($percent%)")
                            }
                        }
                    }
                }

                if (tempZip.exists() && tempZip.length() > 10 * 1024 * 1024) {
                    downloaded = true
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!downloaded || !tempZip.exists()) {
            onProgress(-1, "Błąd pobierania OpenJDK 25 ze wszystkich serwerów lustrzanych.")
            return@withContext false
        }

        try {
            onProgress(92, "Usuwanie starej wersji i rozpakowywanie OpenJDK 25...")
            if (jreDir.exists()) {
                jreDir.deleteRecursively()
            }
            jreDir.mkdirs()

            unzip(tempZip, jreDir)
            tempZip.delete()

            // Zapisz znacznik wersji
            File(jreDir, "version.txt").writeText("25")

            onProgress(98, "Konfigurowanie uprawnień systemowych...")
            grantExecutionPermissions(jreDir)

            onProgress(100, "Środowisko OpenJDK 25 gotowe!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd instalacji OpenJDK 25: ${e.localizedMessage}")
            false
        }
    }

    private fun grantExecutionPermissions(dir: File) {
        dir.walkTopDown().forEach { file ->
            if (file.isDirectory) {
                file.setExecutable(true, false)
                file.setReadable(true, false)
            } else if (file.parentFile?.name == "bin" || file.name == "java" || file.name.endsWith(".so") || file.name == "jspawnhelper" || file.name == "jexec") {
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
                    if (newFile.parentFile?.name == "bin" || newFile.name == "java" || newFile.name.endsWith(".so") || newFile.name == "jspawnhelper" || newFile.name == "jexec") {
                        makeExecutable(newFile)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    companion object {
        const val PRIMARY_JRE_ARM64_URL = "https://github.com/vipluk/MinecraftMobile/releases/download/v1.0.0-assets/openjdk-25-aarch64.zip"
        const val FALLBACK_JRE_ARM64_URL = "http://34.185.160.5/openjdk-25-aarch64.zip"
    }
}
