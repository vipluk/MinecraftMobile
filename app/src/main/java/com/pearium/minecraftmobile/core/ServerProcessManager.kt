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
            // 1. Sprawdź czy środowisko Java 25 jest pobrane
            if (!javaRuntimeManager.isJavaInstalled()) {
                appendLog("=== POBIERANIE ŚRODOWISKA JAVA 25 (ARM64) ===")
                appendLog("[Java] Brak zainstalowanego OpenJDK 25. Rozpoczynanie automatycznego pobierania...")
                _serverState.update { it.copy(errorMessage = "Pobieranie środowiska Java 25...") }

                val javaSuccess = javaRuntimeManager.installJavaRuntime { percent, msg ->
                    _serverState.update { it.copy(errorMessage = msg) }
                    if (percent % 25 == 0 || percent == 100) {
                        appendLog("[Java] $msg")
                    }
                }

                if (!javaSuccess || !javaRuntimeManager.isJavaInstalled()) {
                    _serverState.update {
                        it.copy(
                            status = ServerStatus.ERROR,
                            errorMessage = "Błąd instalacji środowiska Java 25! Sprawdź połączenie z Internetem."
                        )
                    }
                    appendLog("[BŁĄD] Nie udało się zainstalować środowiska Java 25.")
                    return@launch
                }

                appendLog("[Java] Środowisko OpenJDK 25 zainstalowane pomyślnie!")
            }

            // 2. Sprawdź czy silnik Folia 26.2 jest pobrany
            val foliaJar = foliaDownloader.getFoliaJar()
            if (!foliaJar.exists()) {
                appendLog("=== POBIERANIE SILNIKA FOLIA (Wersja 26.2 Eksperymentalna) ===")
                appendLog("[EULA] Automatycznie zatwierdzono eula.txt (eula=true).")
                configManager.ensureEulaAccepted()

                appendLog("[PaperMC] Pobieranie informacji o Folia 26.2 z PaperMC Fill v3 API...")
                _serverState.update { it.copy(errorMessage = "Pobieranie silnika Folia 26.2...") }

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
            }

            _serverState.update { it.copy(errorMessage = null) }
            launchServerProcess(ramGb, foliaJar)
        }
    }

    private suspend fun launchServerProcess(ramGb: Float, foliaJar: java.io.File) {
        try {
            val javaExe = javaRuntimeManager.findJavaExecutable()
            if (javaExe == null || !javaExe.exists()) {
                throw IllegalStateException("Nie znaleziono pliku binarnego Java po instalacji!")
            }
            javaRuntimeManager.makeExecutable(javaExe)
            val javaPath = javaExe.absolutePath
            val javaHome = javaRuntimeManager.getJavaHomeDir().absolutePath
            val binDir = javaExe.parentFile?.absolutePath ?: "$javaHome/bin"
            val libDir = java.io.File(javaHome, "lib").absolutePath

            val serverDir = configManager.serverDir
            val ramInt = ramGb.toInt()

            val tempDir = java.io.File(context.cacheDir, "tmp").apply { mkdirs() }

            // Zoptymalizowane flagi JVM dla procesorów ARM64 i silnika Folia na Androidzie
            val command = arrayListOf(
                javaPath,
                "-Xms512M",
                "-Xmx${ramInt}G",
                "-Djava.io.tmpdir=${tempDir.absolutePath}",
                "-Dterminal.jline=false",
                "-Dterminal.ansi=true",
                "-Dpaper.playerconnection.keepalive=60",
                "-XX:+UseG1GC",
                "-XX:+ParallelRefProcEnabled",
                "-XX:MaxGCPauseMillis=200",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+DisableExplicitGC",
                "-XX:G1NewSizePercent=20",
                "-XX:G1MaxNewSizePercent=40",
                "-XX:G1ReservePercent=15",
                "-XX:SurvivorRatio=32",
                "-Dusing.aikars.flags=https://mcflags.emc.gs",
                "-jar",
                foliaJar.absolutePath,
                "nogui"
            )

            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(serverDir)
            processBuilder.redirectErrorStream(true)

            // Ustaw pełne zmienne środowiskowe dla Android ARM64
            val env = processBuilder.environment()
            env["JAVA_HOME"] = javaHome
            env["PATH"] = "$binDir:" + (env["PATH"] ?: "/system/bin:/system/xbin")
            env["LD_LIBRARY_PATH"] = "$libDir:$libDir/server:/system/lib64:/apex/com.android.runtime/lib64:/apex/com.android.art/lib64:/vendor/lib64"
            env["TMPDIR"] = tempDir.absolutePath

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

            if (exitCode != 0) {
                val errorSnippet = recentLogs
                    .filter { !it.contains("ZAKOŃCZYŁ DZIAŁANIE") && it.isNotBlank() }
                    .takeLast(3)
                    .joinToString("\n")

                _serverState.update {
                    it.copy(
                        status = ServerStatus.ERROR,
                        errorMessage = if (errorSnippet.isNotBlank()) {
                            "Serwer wyłączył się (Kod: $exitCode):\n$errorSnippet"
                        } else {
                            "Serwer wyłączył się z kodem $exitCode. Wejdź w zakładkę Konsola, aby zobaczyć szczegóły!"
                        },
                        uptimeSeconds = 0,
                        playersOnline = 0
                    )
                }
            } else {
                _serverState.update { it.copy(status = ServerStatus.STOPPED, uptimeSeconds = 0, playersOnline = 0) }
            }

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
