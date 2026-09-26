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

    suspend fun fetchLatestDownloadInfo(targetVersion: String = "26.2"): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // PaperMC Fill v3 API dla Folia
            val url = "https://fill.papermc.io/v3/projects/folia/versions/$targetVersion/builds"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext getFallbackDownloadInfo(targetVersion)

            val responseBody = response.body?.string() ?: return@withContext getFallbackDownloadInfo(targetVersion)
            val jsonArray = gson.fromJson(responseBody, com.google.gson.JsonArray::class.java)

            if (jsonArray.size() == 0) return@withContext getFallbackDownloadInfo(targetVersion)

            // Pobierz build o najwyższym ID
            var latestBuildObj: JsonObject? = null
            var maxId = -1

            for (element in jsonArray) {
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val id = obj.get("id")?.asInt ?: -1
                    if (id > maxId) {
                        maxId = id
                        latestBuildObj = obj
                    }
                }
            }

            if (latestBuildObj == null) return@withContext getFallbackDownloadInfo(targetVersion)

            val downloads = latestBuildObj.getAsJsonObject("downloads")
            val serverDefault = downloads.getAsJsonObject("server:default")
            val downloadUrl = serverDefault.get("url").asString

            return@withContext Pair("Folia $targetVersion (build #$maxId - Eksperymentalna)", downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getFallbackDownloadInfo(targetVersion)
        }
    }

    private fun getFallbackDownloadInfo(targetVersion: String): Pair<String, String> {
        return if (targetVersion == "26.2") {
            Pair(
                "Folia 26.2 build #7 (Eksperymentalna)",
                "https://fill-data.papermc.io/v1/objects/128a634192261cd38bb4a5dc54075018a0f896fd6c6f529e37dca6e99e32b3b3/folia-26.2-7.jar"
            )
        } else {
            Pair(
                "Folia 26.2 (Stable)",
                "https://fill-data.papermc.io/v1/objects/128a634192261cd38bb4a5dc54075018a0f896fd6c6f529e37dca6e99e32b3b3/folia-26.2-7.jar"
            )
        }
    }

    suspend fun downloadFolia(
        downloadUrl: String,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "Łączenie z serwerem PaperMC...")
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
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
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            val mbRead = totalRead / (1024 * 1024)
                            val mbTotal = totalBytes / (1024 * 1024)
                            onProgress(percent, "Pobieranie Folia 26.2: $mbRead MB / $mbTotal MB ($percent%)")
                        }
                    }
                }
            }
            onProgress(100, "Silnik Folia 26.2 pobrany pomyślnie!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd pobierania: ${e.localizedMessage}")
            false
        }
    }
}
