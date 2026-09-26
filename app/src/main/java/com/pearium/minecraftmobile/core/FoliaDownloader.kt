package com.pearium.minecraftmobile.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class FoliaDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun getFoliaJar(): File {
        return File(File(context.filesDir, "minecraft_server"), "folia.jar")
    }

    fun isFoliaInstalled(): Boolean {
        val jar = getFoliaJar()
        return jar.exists() && jar.length() > 10 * 1024 * 1024 // min 10 MB
    }

    suspend fun fetchLatestDownloadInfo(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // 1. Pobierz listę wersji projektu Folia
            val projectUrl = "https://api.papermc.io/v2/projects/folia"
            val projectReq = Request.Builder().url(projectUrl).build()
            val projectResp = client.newCall(projectReq).execute()
            if (!projectResp.isSuccessful) return@withContext null

            val projectJson = gson.fromJson(projectResp.body?.string(), JsonObject::class.java)
            val versions = projectJson.getAsJsonArray("versions")
            val latestVersion = versions[versions.size() - 1].asString

            // 2. Pobierz najnowszy build danej wersji
            val versionUrl = "https://api.papermc.io/v2/projects/folia/versions/$latestVersion"
            val versionReq = Request.Builder().url(versionUrl).build()
            val versionResp = client.newCall(versionReq).execute()
            if (!versionResp.isSuccessful) return@withContext null

            val versionJson = gson.fromJson(versionResp.body?.string(), JsonObject::class.java)
            val builds = versionJson.getAsJsonArray("builds")
            val latestBuild = builds[builds.size() - 1].asString

            // 3. Zbuduj URL do pobrania
            val downloadUrl = "https://api.papermc.io/v2/projects/folia/versions/$latestVersion/builds/$latestBuild/downloads/folia-$latestVersion-$latestBuild.jar"
            return@withContext Pair("Folia $latestVersion (build #$latestBuild)", downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback na stabilny mirror / wersję 1.20.4
            return@withContext Pair("Folia 1.20.4 (Stable)", "https://api.papermc.io/v2/projects/folia/versions/1.20.4/builds/30/downloads/folia-1.20.4-30.jar")
        }
    }

    suspend fun downloadFolia(
        downloadUrl: String,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "Łączenie z PaperMC API...")
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                onProgress(-1, "Błąd HTTP: ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val totalBytes = body.contentLength()
            val destinationFile = getFoliaJar()
            destinationFile.parentFile?.mkdirs()

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            val mbRead = totalRead / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(percent, "Pobieranie Folia: $mbRead MB / $mbTotal MB ($percent%)")
                        }
                    }
                }
            }
            onProgress(100, "Silnik Folia pobrany pomyślnie!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd pobierania: ${e.localizedMessage}")
            false
        }
    }
}
