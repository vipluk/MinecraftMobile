package com.pearium.minecraftmobile.tunnel

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class TunnelConfig(
    val serverHost: String = "34.185.160.5",
    val serverPort: Int = 7000,
    val token: String = "pearium-mc-secret-2026",
    val localPort: Int = 25565,
    val remotePort: Int = 25565,
    val isAutoStart: Boolean = true
)

class FRPClientManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tunnelJob: Job? = null
    private var frpcProcess: Process? = null

    private val _status = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow(TunnelConfig())
    val config: StateFlow<TunnelConfig> = _config.asStateFlow()

    private val recentLogs = mutableListOf<String>()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val binDir: File
        get() = File(context.filesDir, "bin").apply { mkdirs() }

    private val frpcBinary: File
        get() = File(binDir, "frpc")

    private val configFile: File
        get() = File(context.filesDir, "frpc.toml")

    init {
        saveDefaultConfigIfMissing()
        // Wstępne rozpakowanie binarki w tle po starcie
        scope.launch {
            ensureBinaryReady()
        }
    }

    fun updateConfig(newConfig: TunnelConfig) {
        _config.value = newConfig
        saveConfigToFile(newConfig)
    }

    private fun saveDefaultConfigIfMissing() {
        if (!configFile.exists()) {
            saveConfigToFile(_config.value)
        }
    }

    private fun saveConfigToFile(cfg: TunnelConfig) {
        val tomlContent = """
            serverAddr = "${cfg.serverHost}"
            serverPort = ${cfg.serverPort}
            auth.token = "${cfg.token}"

            [[proxies]]
            name = "minecraft-folia"
            type = "tcp"
            localIP = "127.0.0.1"
            localPort = ${cfg.localPort}
            remotePort = ${cfg.remotePort}
        """.trimIndent()
        configFile.writeText(tomlContent)
    }

    private fun log(message: String) {
        synchronized(recentLogs) {
            if (recentLogs.size > 150) recentLogs.removeAt(0)
            recentLogs.add(message)
            _logs.value = recentLogs.toList()
        }
    }

    suspend fun ensureBinaryReady(): Boolean = withContext(Dispatchers.IO) {
        if (frpcBinary.exists() && frpcBinary.length() > 1_000_000) {
            makeExecutable(frpcBinary)
            return@withContext true
        }

        binDir.mkdirs()

        // 1. Spróbuj rozpakować z assets wbudowanego w APK klienta frpc.gz
        try {
            log("[FRP] Przygotowywanie wbudowanego klienta tunelu z assets...")
            context.assets.open("bin/frpc.gz").use { assetIn ->
                GZIPInputStream(assetIn).use { gzIn ->
                    val tmp = File(binDir, "frpc.tmp")
                    FileOutputStream(tmp).use { out ->
                        gzIn.copyTo(out)
                    }
                    if (tmp.length() > 1_000_000) {
                        if (frpcBinary.exists()) frpcBinary.delete()
                        tmp.renameTo(frpcBinary)
                        makeExecutable(frpcBinary)
                        log("[FRP] Klient frpc pomyślnie rozpakowany (${frpcBinary.length() / (1024 * 1024)} MB)!")
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            log("[FRP] Informacja: Brak zasobu w assets (${e.message}), pobieranie...")
        }

        // 2. Jeśli brak w assets, pobierz z serwera GCP
        val downloadUrls = listOf(
            "http://${_config.value.serverHost}/frpc.gz",
            "http://34.185.160.5/frpc.gz",
            "http://${_config.value.serverHost}/frpc"
        )

        for (urlStr in downloadUrls) {
            try {
                log("[FRP] Pobieranie klienta tunelu z $urlStr...")
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val isGz = urlStr.endsWith(".gz")
                    val rawStream = conn.inputStream
                    val inStream = if (isGz) GZIPInputStream(rawStream) else rawStream
                    val tmp = File(binDir, "frpc.tmp")
                    FileOutputStream(tmp).use { out ->
                        inStream.copyTo(out)
                    }
                    inStream.close()
                    if (tmp.length() > 1_000_000) {
                        if (frpcBinary.exists()) frpcBinary.delete()
                        tmp.renameTo(frpcBinary)
                        makeExecutable(frpcBinary)
                        log("[FRP] Pomyślnie pobrano klienta (${frpcBinary.length() / (1024 * 1024)} MB)!")
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                log("[FRP] Błąd pobierania z $urlStr: ${e.message}")
            }
        }
        false
    }

    private fun makeExecutable(file: File) {
        file.setExecutable(true, false)
        file.setReadable(true, false)
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", file.absolutePath)).waitFor()
        } catch (_: Exception) {}
    }

    fun startTunnel() {
        if (_status.value == TunnelStatus.CONNECTED || _status.value == TunnelStatus.CONNECTING) return

        _status.value = TunnelStatus.CONNECTING
        tunnelJob?.cancel()

        tunnelJob = scope.launch {
            try {
                log("=== URUCHAMIANIE TUNELU SIECIOWEGO FRP ===")
                saveConfigToFile(_config.value)

                val ready = ensureBinaryReady()
                if (!ready) {
                    log("[FRP BŁĄD] Nie udało się przygotować pliku wykonywalnego frpc!")
                    _status.value = TunnelStatus.ERROR
                    return@launch
                }

                stopProcessInternal()

                log("[FRP] Łączenie z przekaźnikiem ${_config.value.serverHost}:${_config.value.serverPort}...")
                val pb = ProcessBuilder(frpcBinary.absolutePath, "-c", configFile.absolutePath)
                pb.directory(context.filesDir)
                pb.redirectErrorStream(true)

                val proc = pb.start()
                frpcProcess = proc

                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val logLine = line ?: continue
                    log("[FRP] $logLine")

                    if (logLine.contains("start proxy success") || logLine.contains("proxy added")) {
                        _status.value = TunnelStatus.CONNECTED
                    }
                }

                val exit = proc.waitFor()
                log("[FRP] Proces tunelu zakończył działanie (kod: $exit).")
                _status.value = TunnelStatus.DISCONNECTED
            } catch (e: Exception) {
                log("[FRP BŁĄD] ${e.localizedMessage}")
                _status.value = TunnelStatus.ERROR
            } finally {
                frpcProcess = null
                if (_status.value == TunnelStatus.CONNECTING) {
                    _status.value = TunnelStatus.ERROR
                }
            }
        }
    }

    private fun stopProcessInternal() {
        try {
            frpcProcess?.destroy()
            frpcProcess = null
        } catch (_: Exception) {}
    }

    fun stopTunnel() {
        tunnelJob?.cancel()
        tunnelJob = null
        stopProcessInternal()
        _status.value = TunnelStatus.DISCONNECTED
        log("[FRP] Tunel został wyłączony.")
    }
}
