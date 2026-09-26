package com.pearium.minecraftmobile.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class ConfigManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("minecraft_mobile_prefs", Context.MODE_PRIVATE)

    fun getSelectedEngine(): ServerEngine {
        val id = prefs.getString("selected_engine", "folia") ?: "folia"
        return ServerEngine.fromId(id)
    }

    fun setSelectedEngine(engine: ServerEngine) {
        prefs.edit().putString("selected_engine", engine.id).apply()
    }

    fun getSelectedVersion(engine: ServerEngine = getSelectedEngine()): String {
        val key = "version_${engine.id}"
        val defaultVer = when (engine) {
            ServerEngine.FOLIA -> prefs.getString("folia_version", "26.2") ?: "26.2"
            ServerEngine.PURPUR -> "1.21.4"
            ServerEngine.FABRIC -> "1.21.4"
        }
        return prefs.getString(key, defaultVer) ?: defaultVer
    }

    fun setSelectedVersion(engine: ServerEngine, version: String) {
        val edit = prefs.edit().putString("version_${engine.id}", version)
        if (engine == ServerEngine.FOLIA) {
            edit.putString("folia_version", version)
        }
        edit.apply()
    }

    fun getSelectedFoliaVersion(): String {
        return getSelectedVersion(ServerEngine.FOLIA)
    }

    fun setSelectedFoliaVersion(version: String) {
        setSelectedVersion(ServerEngine.FOLIA, version)
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

    val modsDir: File
        get() = File(serverDir, "mods").apply {
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

    fun getMotd(): String {
        return readProperties()["motd"] ?: "§aMinecraft Mobile Server §7(Xiaomi 11T Pro)"
    }

    fun setMotd(motd: String) {
        updateProperties(mapOf("motd" to motd))
    }

    fun ensureDefaultIcon() {
        val iconFile = getServerIconFile()
        if (!iconFile.exists()) {
            try {
                context.assets.open("server-icon.png").use { input ->
                    FileOutputStream(iconFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                saveServerIcon(generatePresetIcon("grass"))
            }
        }
    }

    fun getServerIconFile(): File = File(serverDir, "server-icon.png")

    fun getServerIconBitmap(): Bitmap? {
        val file = getServerIconFile()
        return if (file.exists() && file.length() > 0) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun saveServerIcon(bitmap: Bitmap): Boolean {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            FileOutputStream(getServerIconFile()).use { out ->
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generatePresetIcon(type: String): Bitmap {
        if (type.lowercase() == "pearium") {
            try {
                context.assets.open("server-icon.png").use { stream ->
                    val loaded = BitmapFactory.decodeStream(stream)
                    if (loaded != null) return loaded
                }
            } catch (_: Exception) {}
        }

        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()

        when (type.lowercase()) {
            "creeper" -> {
                paint.color = Color.parseColor("#44A836")
                canvas.drawRect(0f, 0f, 64f, 64f, paint)
                paint.color = Color.parseColor("#2F8325")
                canvas.drawRect(0f, 0f, 32f, 32f, paint)
                canvas.drawRect(48f, 48f, 64f, 64f, paint)
                paint.color = Color.parseColor("#1B1B1B")
                canvas.drawRect(12f, 16f, 24f, 28f, paint)
                canvas.drawRect(40f, 16f, 52f, 28f, paint)
                canvas.drawRect(24f, 28f, 40f, 44f, paint)
                canvas.drawRect(16f, 36f, 24f, 52f, paint)
                canvas.drawRect(40f, 36f, 48f, 52f, paint)
            }
            "diamond" -> {
                paint.color = Color.parseColor("#1E5F74")
                canvas.drawRect(0f, 0f, 64f, 64f, paint)
                paint.color = Color.parseColor("#4DEEEA")
                canvas.drawRect(16f, 16f, 48f, 48f, paint)
                paint.color = Color.parseColor("#E0FFFF")
                canvas.drawRect(20f, 20f, 32f, 32f, paint)
                paint.color = Color.parseColor("#00B4D8")
                canvas.drawRect(32f, 32f, 44f, 44f, paint)
            }
            "netherite" -> {
                paint.color = Color.parseColor("#1C1A1D")
                canvas.drawRect(0f, 0f, 64f, 64f, paint)
                paint.color = Color.parseColor("#3B363C")
                canvas.drawRect(8f, 8f, 56f, 56f, paint)
                paint.color = Color.parseColor("#645967")
                canvas.drawRect(16f, 16f, 48f, 48f, paint)
                paint.color = Color.parseColor("#FFD166")
                canvas.drawRect(28f, 28f, 36f, 36f, paint)
            }
            else -> { // "grass"
                paint.color = Color.parseColor("#5A8F35")
                canvas.drawRect(0f, 0f, 64f, 24f, paint)
                paint.color = Color.parseColor("#866043")
                canvas.drawRect(0f, 24f, 64f, 64f, paint)
                paint.color = Color.parseColor("#4C782C")
                canvas.drawRect(8f, 24f, 16f, 32f, paint)
                canvas.drawRect(24f, 24f, 36f, 32f, paint)
                canvas.drawRect(48f, 24f, 56f, 30f, paint)
                paint.color = Color.parseColor("#694C35")
                canvas.drawRect(12f, 40f, 20f, 48f, paint)
                canvas.drawRect(40f, 46f, 50f, 56f, paint)
            }
        }
        return bmp
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
            "online-mode" to "false",
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
