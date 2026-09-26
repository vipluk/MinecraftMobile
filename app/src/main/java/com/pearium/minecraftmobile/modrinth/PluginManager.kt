package com.pearium.minecraftmobile.modrinth

import com.pearium.minecraftmobile.core.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PluginManager(
    private val configManager: ConfigManager,
    private val modrinthApiService: ModrinthApiService = ModrinthApiService()
) {

    fun getInstalledPlugins(): List<InstalledPlugin> {
        val dir = configManager.pluginsDir
        if (!dir.exists()) return emptyList()

        val files = dir.listFiles { _, name ->
            name.endsWith(".jar") || name.endsWith(".jar.disabled")
        } ?: return emptyList()

        return files.map { file ->
            val isEnabled = file.name.endsWith(".jar")
            val baseName = if (isEnabled) {
                file.name.removeSuffix(".jar")
            } else {
                file.name.removeSuffix(".jar.disabled")
            }
            InstalledPlugin(
                name = baseName,
                filename = file.name,
                isEnabled = isEnabled,
                sizeBytes = file.length()
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun togglePlugin(plugin: InstalledPlugin): Boolean {
        val dir = configManager.pluginsDir
        val currentFile = File(dir, plugin.filename)
        if (!currentFile.exists()) return false

        val targetName = if (plugin.isEnabled) {
            plugin.filename + ".disabled"
        } else {
            plugin.filename.removeSuffix(".disabled")
        }
        val targetFile = File(dir, targetName)
        return currentFile.renameTo(targetFile)
    }

    fun deletePlugin(plugin: InstalledPlugin): Boolean {
        val dir = configManager.pluginsDir
        val file = File(dir, plugin.filename)
        return file.exists() && file.delete()
    }

    suspend fun installPluginFromModrinth(
        projectIdOrSlug: String,
        onProgress: (Int, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        onProgress(10, "Wyszukiwanie najnowszej wersji...")
        val versionInfo = modrinthApiService.getLatestPluginFile(projectIdOrSlug)
        if (versionInfo == null) {
            onProgress(-1, "Nie znaleziono odpowiedniego pliku JAR dla tego pluginu.")
            return@withContext false
        }

        val (versionNumber, versionFile) = versionInfo
        onProgress(30, "Pobieranie ${versionFile.filename} ($versionNumber)...")

        val targetFile = File(configManager.pluginsDir, versionFile.filename)
        val success = modrinthApiService.downloadPluginToFolder(versionFile.url, targetFile) { percent ->
            val overall = 30 + (percent * 70 / 100)
            onProgress(overall, "Pobieranie: $percent%")
        }

        if (success) {
            onProgress(100, "Zainstalowano pomyślnie!")
            true
        } else {
            onProgress(-1, "Nie udało się pobrać pliku.")
            false
        }
    }
}
