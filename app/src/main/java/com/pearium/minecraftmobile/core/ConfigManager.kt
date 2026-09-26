package com.pearium.minecraftmobile.core

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class ConfigManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("minecraft_mobile_prefs", Context.MODE_PRIVATE)

    fun getSelectedFoliaVersion(): String {
        return prefs.getString("folia_version", "26.2") ?: "26.2"
    }

    fun setSelectedFoliaVersion(version: String) {
        prefs.edit().putString("folia_version", version).apply()
    }

    fun getAllocatedCores(): Int {
        val maxAvailable = Runtime.getRuntime().availableProcessors()
        val defaultCores = (maxAvailable / 2).coerceIn(2, 6)
        return prefs.getInt("allocated_cores", defaultCores)
    }

    fun setAllocatedCores(cores: Int) {
        prefs.edit().putInt("allocated_cores", cores).apply()
    }

    fun getAllocatedRam(): Float {
        return prefs.getFloat("allocated_ram", 4.0f)
    }

    fun setAllocatedRam(ram: Float) {
        prefs.edit().putFloat("allocated_ram", ram).apply()
    }

    val serverDir: File
        get() = File(context.filesDir, "minecraft_server").apply {
            if (!exists()) mkdirs()
        }

    val pluginsDir: File
        get() = File(serverDir, "plugins").apply {
            if (!exists()) mkdirs()
        }

    fun ensureEulaAccepted() {
        val eulaFile = File(serverDir, "eula.txt")
        eulaFile.writeText("# By changing the setting below to TRUE you are indicating your agreement to our EULA\neula=true\n")
    }

    fun readProperties(): Map<String, String> {
        val file = File(serverDir, "server.properties")
        if (!file.exists()) {
            return defaultProperties()
        }
        val properties = Properties()
        FileInputStream(file).use { properties.load(it) }
        return properties.entries.associate { it.key.toString() to it.value.toString() }
    }

    fun updateProperties(newProps: Map<String, String>) {
        val file = File(serverDir, "server.properties")
        val properties = Properties()
        if (file.exists()) {
            FileInputStream(file).use { properties.load(it) }
        } else {
            defaultProperties().forEach { (k, v) -> properties.setProperty(k, v) }
        }
        newProps.forEach { (k, v) -> properties.setProperty(k, v) }
        FileOutputStream(file).use {
            properties.store(it, "MinecraftMobile Auto-Generated Server Configuration")
        }
    }

    fun readRawFile(filename: String): String {
        val file = File(serverDir, filename)
        return if (file.exists()) file.readText() else ""
    }

    fun saveRawFile(filename: String, content: String) {
        val file = File(serverDir, filename)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    private fun defaultProperties(): Map<String, String> {
        return mapOf(
            "server-port" to "25565",
            "motd" to "§aMinecraft Mobile Server §7(Xiaomi 11T Pro)",
            "difficulty" to "normal",
            "gamemode" to "survival",
            "max-players" to "20",
            "online-mode" to "false", // Domyślnie pozwala na wejście bez problemów z autoryzacją
            "pvp" to "true",
            "view-distance" to "8",
            "simulation-distance" to "6",
            "enable-rcon" to "false",
            "white-list" to "false",
            "level-name" to "world",
            "spawn-protection" to "16"
        )
    }
}
