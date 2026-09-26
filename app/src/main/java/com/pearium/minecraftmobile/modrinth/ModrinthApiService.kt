package com.pearium.minecraftmobile.modrinth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ModrinthApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Wyszukuje pluginy w serwisie Modrinth dopasowane do silników serwerowych (Paper/Folia).
     */
    suspend fun searchPlugins(query: String, limit: Int = 20): List<ModrinthProjectHit> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = "https://api.modrinth.com/v2/search".toHttpUrlOrNull()?.newBuilder() ?: return@withContext emptyList()
            urlBuilder.addQueryParameter("query", query)
            urlBuilder.addQueryParameter("limit", limit.toString())
            // Filtrujemy tylko projekty typu plugin
            urlBuilder.addQueryParameter("facets", "[[\"project_type:plugin\"]]")

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "MinecraftMobile-App/1.0.0 (contact: vipluk@pearium.com)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val json = response.body?.string() ?: return@withContext emptyList()
            val searchResponse = gson.fromJson(json, ModrinthSearchResponse::class.java)
            searchResponse.hits
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Pobiera wersje danego projektu i znajduje podstawowy plik .jar do pobrania.
     */
    suspend fun getLatestPluginFile(projectIdOrSlug: String): Pair<String, ModrinthVersionFile>? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.modrinth.com/v2/project/$projectIdOrSlug/version"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile-App/1.0.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = response.body?.string() ?: return@withContext null
            val type = object : TypeToken<List<ModrinthVersion>>() {}.type
            val versions: List<ModrinthVersion> = gson.fromJson(json, type)

            if (versions.isEmpty()) return@withContext null

            val latestVersion = versions.first()
            val primaryFile = latestVersion.files.firstOrNull { it.primary } ?: latestVersion.files.firstOrNull()

            if (primaryFile != null) {
                Pair(latestVersion.versionNumber, primaryFile)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Pobiera wtyczkę 1-kliknięciem bezpośrednio do folderu plugins/
     */
    suspend fun downloadPluginToFolder(
        fileUrl: String,
        targetFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(fileUrl)
                .header("User-Agent", "MinecraftMobile-App/1.0.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val totalBytes = body.contentLength()
            targetFile.parentFile?.mkdirs()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = ((totalRead * 100) / totalBytes).toInt()
                            onProgress(percent)
                        }
                    }
                }
            }
            onProgress(100)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
