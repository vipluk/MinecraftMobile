package com.pearium.minecraftmobile.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

class ServerProcessManager(
    private val context: Context,
    private val configManager: ConfigManager,
    private val javaRuntimeManager: JavaRuntimeManager,
    private val foliaDownloader: FoliaDownloader
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var process: Process? = null
    private var processWriter: BufferedWriter? = null
    private var statsJob: Job? = null

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _logFlow = MutableSharedFlow<String>(replay = 100, extraBufferCapacity = 500)
    val logFlow: SharedFlow<String> = _logFlow.asSharedFlow()

    private val recentLogs = mutableListOf<String>()

    fun setRamAllocation(ramGb: Float) {
        val clamped = ramGb.coerceIn(1.0f, 8.0f)
        _serverState.update { it.copy(allocatedRamGb = clamped) }
    }

    fun startServer(ramGb: Float = _serverState.value.allocatedRamGb) {
        if (_serverState.value.status != ServerStatus.STOPPED && _serverState.value.status != ServerStatus.ERROR) {
            return
        }

        // Zawsze upewnij się, że EULA jest zaakceptowana (eula=true)
        configManager.ensureEulaAccepted()

        val foliaJar = foliaDownloader.getFoliaJar()
        if (!foliaJar.exists()) {
            _serverState.update {
                it.copy(
                    status = ServerStatus.STARTING,
                    allocatedRamGb = ramGb,
                    errorMessage = "Pobieranie silnika Folia 26.2...",
                    uptimeSeconds = 0,
                    playersOnline = 0
                )
            }

            scope.launch {
                appendLog("=== POBIERANIE SILNIKA FOLIA (Wersja 26.2 Eksperymentalna) ===")
                appendLog("[EULA] Automatycznie zatwierdzono eula.txt (eula=true).")
                configManager.ensureEulaAccepted()

                appendLog("[PaperMC] Pobieranie informacji o Folia 26.2 z PaperMC Fill v3 API...")
                val downloadInfo = foliaDownloader.fetchLatestDownloadInfo("26.2")
                val buildName = downloadInfo?.first ?: "Folia 26.2 (Eksperymentalna)"
                val downloadUrl = downloadInfo?.second ?: "https://fill-data.papermc.io/v1/objects/128a634192261cd38bb4a5dc54075018a0f896fd6c6f529e37dca6e99e32b3b3/folia-26.2-7.jar"

                appendLog("[PaperMC] Wybrany build: $buildName")
                val success = foliaDownloader.downloadFolia(downloadUrl) { percent, msg ->
                    _serverState.update { it.copy(errorMessage = msg) }
                    if (percent % 25 == 0 || percent == 100) {
                        appendLog("[Pobieranie] $msg")
                    }
                }

                if (!success || !foliaJar.exists()) {
                    _serverState.update {
                        it.copy(
                            status = ServerStatus.ERROR,
                            errorMessage = "Błąd pobierania silnika Folia. Sprawdź połączenie z Internetem."
                        )
                    }
                    appendLog("[BŁĄD] Nie udało się pobrać pliku folia.jar.")
                    return@launch
                }

                appendLog("[PaperMC] Silnik Folia 26.2 pobrany pomyślnie (${foliaJar.length() / (1024 * 1024)} MB)!")
                configManager.ensureEulaAccepted()
                _serverState.update { it.copy(errorMessage = null) }

                launchServerProcess(ramGb, foliaJar)
            }
            return
        }

        _serverState.update {
            it.copy(
                status = ServerStatus.STARTING,
                allocatedRamGb = ramGb,
                errorMessage = null,
                uptimeSeconds = 0,
                playersOnline = 0
            )
        }

        scope.launch {
            launchServerProcess(ramGb, foliaJar)
        }
    }

    private suspend fun launchServerProcess(ramGb: Float, foliaJar: java.io.File) {
        try {
            val javaPath = javaRuntimeManager.getExecutablePath()
            val serverDir = configManager.serverDir
            val ramInt = ramGb.toInt()

            // Zoptymalizowane flagi JVM Aikar dla procesorów ARM64 i silnika Folia
            val command = arrayListOf(
                javaPath,
                "-Xms${ramInt}G",
                "-Xmx${ramInt}G",
                "-XX:+UseG1GC",
                "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+DisableExplicitGC",
                "-XX:+AlwaysPreTouch",
                "-XX:G1NewSizePercent=30",
                "-XX:G1MaxNewSizePercent=40",
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=15",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-Dusing.aikars.flags=https://mcflags.emc.gs",
                "-Daikars.new.flags=true",
                "-jar",
                foliaJar.absolutePath,
                "nogui"
            )

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(serverDir)
            processBuilder.redirectErrorStream(true)

            // Ustaw zmienne środowiskowe dla Android ARM64
            val env = processBuilder.environment()
            env["JAVA_HOME"] = javaRuntimeManager.jreDir.absolutePath
            env["PATH"] = "${javaRuntimeManager.jreDir.absolutePath}/bin:" + (env["PATH"] ?: "")
            env["LD_LIBRARY_PATH"] = "${javaRuntimeManager.jreDir.absolutePath}/lib:" + (env["LD_LIBRARY_PATH"] ?: "")

            val proc = processBuilder.start()
            process = proc
            processWriter = BufferedWriter(OutputStreamWriter(proc.outputStream))

            appendLog("=== URUCHAMIANIE SERWERA FOLIA (RAM: ${ramInt}GB) ===")

            // Uruchom pętlę statystyk i czasu działania
            startStatsLoop()

            // Czytanie wyjścia serwera
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val logLine = line ?: continue
                appendLog(logLine)

                // Sprawdź czy serwer zakończył start
                if (logLine.contains("Done (") || logLine.contains("Timings Reset")) {
                    _serverState.update { it.copy(status = ServerStatus.RUNNING) }
                }

                // Śledzenie graczy online z logów
                if (logLine.contains("logged in with entity id")) {
                    _serverState.update { it.copy(playersOnline = it.playersOnline + 1) }
                } else if (logLine.contains("lost connection:")) {
                    _serverState.update { it.copy(playersOnline = (it.playersOnline - 1).coerceAtLeast(0)) }
                }
            }

            val exitCode = proc.waitFor()
            appendLog("=== SERWER ZAKOŃCZYŁ DZIAŁANIE (Kod: $exitCode) ===")
            stopStatsLoop()
            _serverState.update { it.copy(status = ServerStatus.STOPPED, uptimeSeconds = 0, playersOnline = 0) }

        } catch (e: Exception) {
            e.printStackTrace()
            appendLog("[BŁĄD] Nie udało się wystartować procesu: ${e.localizedMessage}")
            stopStatsLoop()
            _serverState.update {
                it.copy(
                    status = ServerStatus.ERROR,
                    errorMessage = e.localizedMessage
                )
            }
        } finally {
            process = null
            processWriter = null
        }
    }

    fun stopServer() {
        if (_serverState.value.status != ServerStatus.RUNNING && _serverState.value.status != ServerStatus.STARTING) {
            return
        }

        _serverState.update { it.copy(status = ServerStatus.STOPPING) }
        appendLog("[SYSTEM] Wysłano polecenie bezpiecznego zamknięcia serwera (stop)...")

        scope.launch {
            sendCommand("stop")
            val currentProcess = process
            if (currentProcess != null) {
                // Poczekaj do 15 sekund na eleganckie zapisanie chunków i zamknięcie
                val finished = withContext(Dispatchers.IO) {
                    try {
                        currentProcess.waitFor(15, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        false
                    }
                }
                if (!finished) {
                    appendLog("[OSTRZEŻENIE] Serwer nie odpowiedział w ciągu 15s. Wymuszanie zatrzymania...")
                    currentProcess.destroyForcibly()
                }
            }
            _serverState.update { it.copy(status = ServerStatus.STOPPED, uptimeSeconds = 0, playersOnline = 0) }
            stopStatsLoop()
        }
    }

    fun sendCommand(cmd: String) {
        scope.launch {
            try {
                val cleanCmd = if (cmd.startsWith("/")) cmd.substring(1) else cmd
                processWriter?.let { writer ->
                    writer.write(cleanCmd + "\n")
                    writer.flush()
                    appendLog("> $cleanCmd")
                }
            } catch (e: Exception) {
                appendLog("[BŁĄD] Nie udało się wysłać komendy: ${e.localizedMessage}")
            }
        }
    }

    private fun appendLog(log: String) {
        synchronized(recentLogs) {
            if (recentLogs.size > 500) {
                recentLogs.removeAt(0)
            }
            recentLogs.add(log)
        }
        scope.launch {
            _logFlow.emit(log)
        }
    }

    fun getRecentLogs(): List<String> {
        synchronized(recentLogs) {
            return recentLogs.toList()
        }
    }

    private fun startStatsLoop() {
        stopStatsLoop()
        statsJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000)
                seconds++
                _serverState.update { it.copy(uptimeSeconds = seconds) }
            }
        }
    }

    private fun stopStatsLoop() {
        statsJob?.cancel()
        statsJob = null
    }
}
