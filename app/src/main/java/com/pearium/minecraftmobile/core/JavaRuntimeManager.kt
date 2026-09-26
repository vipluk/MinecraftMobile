package com.pearium.minecraftmobile.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class JavaRuntimeManager(private val context: Context) {

    val jreDir: File
        get() = File(context.filesDir, "jre")

    val javaExecutable: File
        get() = File(jreDir, "bin/java")

    fun isJavaInstalled(): Boolean {
        return javaExecutable.exists() && javaExecutable.canExecute()
    }

    /**
     * Zwraca ścieżkę do wykonywalnego pliku java.
     * W razie potrzeby nadaje uprawnienia wykonywania (chmod 755).
     */
    fun getExecutablePath(): String {
        if (javaExecutable.exists()) {
            javaExecutable.setExecutable(true, false)
            return javaExecutable.absolutePath
        }
        // Fallback: jeśli w systemie jest dostępna komenda java
        return "java"
    }

    /**
     * Pobiera i instaluje headless OpenJDK 21 dla architektury ARM64.
     */
    suspend fun installJavaRuntime(
        downloadUrl: String = DEFAULT_JRE_ARM64_URL,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val tempZip = File(context.cacheDir, "jre21_arm64.zip")

        try {
            onProgress(0, "Pobieranie środowiska OpenJDK 21 (ARM64)...")
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                onProgress(-1, "Błąd pobierania JRE: HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempZip).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            onProgress(percent, "Pobrano JRE: ${totalRead / (1024 * 1024)} MB (${percent}%)")
                        }
                    }
                }
            }

            onProgress(90, "Rozpakowywanie środowiska Java 21...")
            unzip(tempZip, jreDir)
            tempZip.delete()

            // Nadaj uprawnienia wykonywalne dla wszystkich binariów w bin/
            val binDir = File(jreDir, "bin")
            binDir.listFiles()?.forEach { bin ->
                bin.setExecutable(true, false)
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "755", bin.absolutePath)).waitFor()
                } catch (ignored: Exception) {}
            }

            onProgress(100, "Środowisko Java 21 gotowe!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd instalacji JRE: ${e.localizedMessage}")
            false
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
                        val buffer = ByteArray(8 * 1024)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }

    companion object {
        // Domyślny stabilny mirror OpenJDK 21 Headless dla Android ARM64
        const val DEFAULT_JRE_ARM64_URL = "https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch/releases/download/jre21-20240409/jre21-arm64.tar.xz"
    }
}
