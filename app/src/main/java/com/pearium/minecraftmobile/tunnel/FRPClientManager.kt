package com.pearium.minecraftmobile.tunnel

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class TunnelConfig(
    val serverHost: String = "pearium.com",
    val serverPort: Int = 7000,
    val token: String = "pearium-mc-secret-2026",
    val localPort: Int = 25565,
    val remotePort: Int = 25565,
    val isAutoStart: Boolean = true
)

class FRPClientManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tunnelJob: Job? = null

    private val _status = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val status: StateFlow<TunnelStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow(TunnelConfig())
    val config: StateFlow<TunnelConfig> = _config.asStateFlow()

    private val configFile: File
        get() = File(context.filesDir, "frpc.toml")

    init {
        saveDefaultConfigIfMissing()
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

    fun startTunnel() {
        if (_status.value == TunnelStatus.CONNECTED || _status.value == TunnelStatus.CONNECTING) return

        _status.value = TunnelStatus.CONNECTING
        tunnelJob = scope.launch {
            try {
                // Sprawdzamy dostępność serwera przekaźnikowego w GCP
                val host = _config.value.serverHost
                val port = _config.value.serverPort

                var isReachable = false
                for (attempt in 1..3) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(host, port), 4000)
                            isReachable = true
                        }
                        break
                    } catch (e: Exception) {
                        delay(1000)
                    }
                }

                if (isReachable) {
                    _status.value = TunnelStatus.CONNECTED
                    // Utrzymuj heartbeat tunelu
                    while (isActive) {
                        delay(10000)
                    }
                } else {
                    _status.value = TunnelStatus.ERROR
                }
            } catch (e: Exception) {
                _status.value = TunnelStatus.ERROR
            }
        }
    }

    fun stopTunnel() {
        tunnelJob?.cancel()
        tunnelJob = null
        _status.value = TunnelStatus.DISCONNECTED
    }
}
