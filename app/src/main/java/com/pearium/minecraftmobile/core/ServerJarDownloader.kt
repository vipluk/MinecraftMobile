package com.pearium.minecraftmobile.core

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ServerJarDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    val serverDir: File
        get() = File(context.filesDir, "minecraft_server").apply {
            if (!exists()) mkdirs()
        }

    fun getServerJar(engine: ServerEngine, version: String): File {
        val specificJar = File(serverDir, "server_${engine.id}_$version.jar")
        if (specificJar.exists()) return specificJar

        // Kompatybilność wsteczna dla istniejącego pliku folia.jar
        if (engine == ServerEngine.FOLIA) {
            val legacy = File(serverDir, "folia.jar")
            if (legacy.exists()) return legacy
        }
        return specificJar
    }

    fun isEngineVersionInstalled(engine: ServerEngine, version: String): Boolean {
        val jar = getServerJar(engine, version)
        return jar.exists() && jar.length() > 2 * 1024 * 1024 // min 2 MB
    }

    // Metody kompatybilności wstecznej dla starszych odwołań Folia
    fun getFoliaJar(): File = getServerJar(ServerEngine.FOLIA, "26.2")

    fun isFoliaInstalled(): Boolean = isEngineVersionInstalled(ServerEngine.FOLIA, "26.2")

    fun getInstalledVersion(): String? {
        val file = File(serverDir, "folia_version.txt")
        if (file.exists() && getFoliaJar().exists()) {
            val v = file.readText().trim()
            if (v.isNotEmpty()) return v
        }
        return if (getFoliaJar().exists()) "26.2" else null
    }

    fun setInstalledVersion(version: String) {
        val file = File(serverDir, "folia_version.txt")
        file.parentFile?.mkdirs()
        file.writeText(version)
    }

    fun getCachedVersions(engine: ServerEngine): List<String> {
        return when (engine) {
            ServerEngine.FOLIA -> listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.20.6", "1.20.4", "1.19.4")
            ServerEngine.PURPUR -> listOf("26.3", "26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.1", "1.20.6", "1.20.4", "1.20.2", "1.19.4")
            ServerEngine.FABRIC -> listOf("1.21.4", "1.21.3", "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.16.5")
        }
    }

    suspend fun fetchAvailableVersions(engine: ServerEngine): List<String> = withContext(Dispatchers.IO) {
        try {
            when (engine) {
                ServerEngine.FOLIA -> fetchFoliaVersions()
                ServerEngine.PURPUR -> fetchPurpurVersions()
                ServerEngine.FABRIC -> fetchFabricVersions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getCachedVersions(engine)
        }
    }

    private fun fetchFoliaVersions(): List<String> {
        try {
            val url = "https://fill.papermc.io/v3/projects/folia"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return getCachedVersions(ServerEngine.FOLIA)
                val json = gson.fromJson(body, JsonObject::class.java)
                val versionsObj = json.getAsJsonObject("versions")
                val list = mutableListOf<String>()
                for (entry in versionsObj.entrySet()) {
                    val arr = entry.value.asJsonArray
                    for (v in arr) {
                        list.add(v.asString)
                    }
                }
                if (list.isNotEmpty()) {
                    return list.distinct()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCachedVersions(ServerEngine.FOLIA)
    }

    private fun fetchPurpurVersions(): List<String> {
        try {
            val url = "https://api.purpurmc.org/v2/purpur"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return getCachedVersions(ServerEngine.PURPUR)
                val json = gson.fromJson(body, JsonObject::class.java)
                val arr = json.getAsJsonArray("versions")
                val list = mutableListOf<String>()
                for (element in arr) {
                    list.add(element.asString)
                }
                if (list.isNotEmpty()) {
                    // Odwracamy kolejność, aby najnowsze wersje były na początku listy
                    return list.reversed()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCachedVersions(ServerEngine.PURPUR)
    }

    private fun fetchFabricVersions(): List<String> {
        try {
            val url = "https://meta.fabricmc.net/v2/versions/game"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return getCachedVersions(ServerEngine.FABRIC)
                val arr = gson.fromJson(body, JsonArray::class.java)
                val list = mutableListOf<String>()
                for (element in arr) {
                    if (element.isJsonObject) {
                        val obj = element.asJsonObject
                        val stable = obj.get("stable")?.asBoolean ?: false
                        if (stable) {
                            val ver = obj.get("version")?.asString
                            if (!ver.isNullOrBlank()) {
                                list.add(ver)
                            }
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    // Zwróć maksymalnie 25 najnowszych stabilnych wersji gry
                    return list.take(25)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getCachedVersions(ServerEngine.FABRIC)
    }

    suspend fun fetchLatestDownloadInfo(engine: ServerEngine, targetVersion: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            when (engine) {
                ServerEngine.FOLIA -> fetchFoliaDownloadInfo(targetVersion)
                ServerEngine.PURPUR -> fetchPurpurDownloadInfo(targetVersion)
                ServerEngine.FABRIC -> fetchFabricDownloadInfo(targetVersion)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("${engine.displayName} $targetVersion", getFallbackDownloadUrl(engine, targetVersion))
        }
    }

    private fun fetchFoliaDownloadInfo(targetVersion: String): Pair<String, String> {
        try {
            val url = "https://fill.papermc.io/v3/projects/folia/versions/$targetVersion/builds"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MinecraftMobile/1.0 (pearium.com)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val jsonArray = gson.fromJson(body, JsonArray::class.java)
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

                    if (latestBuildObj != null) {
                        val downloads = latestBuildObj.getAsJsonObject("downloads")
                        val serverDefault = downloads.getAsJsonObject("server:default")
                        val downloadUrl = serverDefault.get("url").asString
                        return Pair("Folia $targetVersion (build #$maxId)", downloadUrl)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair("Folia $targetVersion", getFallbackDownloadUrl(ServerEngine.FOLIA, targetVersion))
    }

    private fun fetchPurpurDownloadInfo(targetVersion: String): Pair<String, String> {
        val downloadUrl = "https://api.purpurmc.org/v2/purpur/$targetVersion/latest/download"
        return Pair("Purpur $targetVersion (Najnowszy)", downloadUrl)
    }

    private fun fetchFabricDownloadInfo(targetVersion: String): Pair<String, String> {
        var loaderVersion = "0.19.5"
        var installerVersion = "1.1.2"

        try {
            val loaderUrl = "https://meta.fabricmc.net/v2/versions/loader"
            val loaderReq = Request.Builder().url(loaderUrl).header("User-Agent", "MinecraftMobile/1.0").build()
            val loaderResp = client.newCall(loaderReq).execute()
            if (loaderResp.isSuccessful) {
                val body = loaderResp.body?.string()
                if (body != null) {
                    val arr = gson.fromJson(body, JsonArray::class.java)
                    for (el in arr) {
                        val obj = el.asJsonObject
                        if (obj.get("stable")?.asBoolean == true) {
                            loaderVersion = obj.get("version").asString
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val instUrl = "https://meta.fabricmc.net/v2/versions/installer"
            val instReq = Request.Builder().url(instUrl).header("User-Agent", "MinecraftMobile/1.0").build()
            val instResp = client.newCall(instReq).execute()
            if (instResp.isSuccessful) {
                val body = instResp.body?.string()
                if (body != null) {
                    val arr = gson.fromJson(body, JsonArray::class.java)
                    if (arr.size() > 0) {
                        installerVersion = arr[0].asJsonObject.get("version").asString
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val downloadUrl = "https://meta.fabricmc.net/v2/versions/loader/$targetVersion/$loaderVersion/$installerVersion/server/jar"
        return Pair("Fabric $targetVersion (Loader $loaderVersion)", downloadUrl)
    }

    fun getFallbackDownloadUrl(engine: ServerEngine, targetVersion: String): String {
        return when (engine) {
            ServerEngine.FOLIA -> "https://fill-data.papermc.io/v1/objects/128a634192261cd38bb4a5dc54075018a0f896fd6c6f529e37dca6e99e32b3b3/folia-26.2-7.jar"
            ServerEngine.PURPUR -> "https://api.purpurmc.org/v2/purpur/$targetVersion/latest/download"
            ServerEngine.FABRIC -> "https://meta.fabricmc.net/v2/versions/loader/$targetVersion/0.19.5/1.1.2/server/jar"
        }
    }

    suspend fun downloadServerJar(
        engine: ServerEngine,
        targetVersion: String,
        downloadUrl: String,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(0, "Łączenie z serwerem ${engine.displayName}...")
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
            val destinationFile = File(serverDir, "server_${engine.id}_$targetVersion.jar")
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
                            onProgress(percent, "Pobieranie ${engine.displayName} $targetVersion: $mbRead MB / $mbTotal MB ($percent%)")
                        } else {
                            val mbRead = totalRead / (1024 * 1024)
                            onProgress(50, "Pobieranie ${engine.displayName} $targetVersion: $mbRead MB...")
                        }
                    }
                }
            }

            // Jeśli to Folia, skopiuj/zlinkuj do folia.jar dla kompatybilności wstecznej
            if (engine == ServerEngine.FOLIA) {
                val legacyJar = File(serverDir, "folia.jar")
                try {
                    destinationFile.copyTo(legacyJar, overwrite = true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setInstalledVersion(targetVersion)
            }

            onProgress(100, "Silnik ${engine.displayName} $targetVersion pobrany pomyślnie!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(-1, "Błąd pobierania silnika ${engine.displayName}: ${e.localizedMessage}")
            false
        }
    }

    // Metody kompatybilności wstecznej z FoliaDownloader:
    suspend fun fetchAvailableVersions(): List<String> = fetchAvailableVersions(ServerEngine.FOLIA)
    suspend fun fetchLatestDownloadInfo(targetVersion: String = "26.2"): Pair<String, String>? = fetchLatestDownloadInfo(ServerEngine.FOLIA, targetVersion)
    suspend fun downloadFolia(targetVersion: String, downloadUrl: String, onProgress: (Int, String) -> Unit): Boolean = downloadServerJar(ServerEngine.FOLIA, targetVersion, downloadUrl, onProgress)
}
