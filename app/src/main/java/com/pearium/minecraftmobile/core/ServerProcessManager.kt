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

    init {
        val savedRam = configManager.getAllocatedRam()
        val savedCores = configManager.getAllocatedCores()
        val savedVersion = configManager.getSelectedFoliaVersion()
        val installedVer = foliaDownloader.getInstalledVersion()

        _serverState.update {
            it.copy(
                allocatedRamGb = savedRam,
                allocatedCores = savedCores,
                selectedFoliaVersion = savedVersion,
                installedFoliaVersion = installedVer
            )
        }

        // Pobierz najnowsze dostępne wersje Folia z API
        scope.launch {
            val versions = foliaDownloader.fetchAvailableVersions()
            _serverState.update { it.copy(availableFoliaVersions = versions) }
        }

        // Ciągły monitor zasobów sprzętowych telefonu
        startStatsLoop()
    }

    fun setRamAllocation(ramGb: Float) {
        val clamped = ramGb.coerceIn(1.0f, 8.0f)
        configManager.setAllocatedRam(clamped)
        _serverState.update { it.copy(allocatedRamGb = clamped) }
    }

    fun setCoreAllocation(cores: Int) {
        val maxAvailable = _serverState.value.totalCores
        val clamped = cores.coerceIn(1, maxAvailable)
        configManager.setAllocatedCores(clamped)
        _serverState.update { it.copy(allocatedCores = clamped) }
    }

    fun setSelectedFoliaVersion(version: String) {
        configManager.setSelectedFoliaVersion(version)
        _serverState.update { it.copy(selectedFoliaVersion = version) }
    }

    fun downloadSelectedFoliaVersion(version: String = _serverState.value.selectedFoliaVersion, onComplete: (Boolean) -> Unit = {}) {
        if (_serverState.value.status != ServerStatus.STOPPED && _serverState.value.status != ServerStatus.ERROR) {
            return
        }
        scope.launch {
            appendLog("=== POBIERANIE WYBRANEJ WERSJI FOLIA: $version ===")
            _serverState.update { it.copy(errorMessage = "Pobieranie informacji o Folia $version...") }
            val downloadInfo = foliaDownloader.fetchLatestDownloadInfo(version)
            if (downloadInfo == null) {
                appendLog("[BŁĄD] Nie znaleziono informacji o wersji $version.")
                _serverState.update { it.copy(errorMessage = "Błąd pobierania informacji o wersji $version.") }
                onComplete(false)
                return@launch
            }
            val (buildName, downloadUrl) = downloadInfo
            appendLog("[PaperMC] Pobieranie: $buildName")
            val ok = foliaDownloader.downloadFolia(version, downloadUrl) { percent, msg ->
                _serverState.update { it.copy(errorMessage = msg) }
                if (percent % 25 == 0 || percent == 100) {
                    appendLog("[Pobieranie] $msg")
                }
            }
            if (ok) {
                _serverState.update {
                    it.copy(
                        installedFoliaVersion = version,
                        selectedFoliaVersion = version,
                        errorMessage = null
                    )
                }
                appendLog("[PaperMC] Wersja Folia $version zainstalowana pomyślnie!")
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun startServer(
        ramGb: Float = _serverState.value.allocatedRamGb,
        cores: Int = _serverState.value.allocatedCores
    ) {
        if (_serverState.value.status != ServerStatus.STOPPED && _serverState.value.status != ServerStatus.ERROR) {
            return
        }

        // Zawsze upewnij się, że EULA jest zaakceptowana (eula=true)
        configManager.ensureEulaAccepted()

        _serverState.update {
            it.copy(
                status = ServerStatus.STARTING,
                allocatedRamGb = ramGb,
                allocatedCores = cores,
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

            // 2. Sprawdź czy wybrana wersja Folia jest pobrana
            val targetVersion = _serverState.value.selectedFoliaVersion
            val installedVersion = foliaDownloader.getInstalledVersion()
            val foliaJar = foliaDownloader.getFoliaJar()

            if (!foliaJar.exists() || installedVersion != targetVersion) {
                appendLog("=== POBIERANIE SILNIKA FOLIA (Wersja $targetVersion) ===")
                appendLog("[EULA] Automatycznie zatwierdzono eula.txt (eula=true).")
                configManager.ensureEulaAccepted()

                appendLog("[PaperMC] Pobieranie informacji o Folia $targetVersion z PaperMC Fill v3 API...")
                _serverState.update { it.copy(errorMessage = "Pobieranie silnika Folia $targetVersion...") }

                val downloadInfo = foliaDownloader.fetchLatestDownloadInfo(targetVersion)
                val buildName = downloadInfo?.first ?: "Folia $targetVersion"
                val downloadUrl = downloadInfo?.second ?: "https://fill-data.papermc.io/v1/objects/128a634192261cd38bb4a5dc54075018a0f896fd6c6f529e37dca6e99e32b3b3/folia-26.2-7.jar"

                appendLog("[PaperMC] Wybrany build: $buildName")
                val success = foliaDownloader.downloadFolia(targetVersion, downloadUrl) { percent, msg ->
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

                _serverState.update { it.copy(installedFoliaVersion = targetVersion) }
                appendLog("[PaperMC] Silnik Folia $targetVersion pobrany pomyślnie (${foliaJar.length() / (1024 * 1024)} MB)!")
                configManager.ensureEulaAccepted()
            }

            _serverState.update { it.copy(errorMessage = null) }
            launchServerProcess(ramGb, cores, foliaJar)
        }
    }

    private suspend fun launchServerProcess(ramGb: Float, cores: Int, foliaJar: java.io.File) {
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
                "-XX:ActiveProcessorCount=$cores",
                "-Dpaper.worker-threads=$cores",
                "-Dfolia.region-threads=${maxOf(1, cores - 1)}",
                "-Dfolia.threads=$cores",
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

            appendLog("=== URUCHAMIANIE SERWERA FOLIA (RAM: ${ramInt}GB, Rdzenie: $cores) ===")

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
                        playersOnline = 0,
                        usedMemoryMb = 0,
                        serverCpuUsagePercent = 0f
                    )
                }
            } else {
                _serverState.update {
                    it.copy(
                        status = ServerStatus.STOPPED,
                        uptimeSeconds = 0,
                        playersOnline = 0,
                        usedMemoryMb = 0,
                        serverCpuUsagePercent = 0f
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            appendLog("[BŁĄD] Nie udało się wystartować procesu: ${e.localizedMessage}")
            _serverState.update {
                it.copy(
                    status = ServerStatus.ERROR,
                    errorMessage = e.localizedMessage,
                    usedMemoryMb = 0,
                    serverCpuUsagePercent = 0f
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
            _serverState.update {
                it.copy(
                    status = ServerStatus.STOPPED,
                    uptimeSeconds = 0,
                    playersOnline = 0,
                    usedMemoryMb = 0,
                    serverCpuUsagePercent = 0f
                )
            }
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

    private fun getProcessPid(proc: Process): Int? {
        return try {
            val field = proc.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            field.getInt(proc)
        } catch (e: Exception) {
            try {
                val method = proc.javaClass.getMethod("pid")
                (method.invoke(proc) as Long).toInt()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = scope.launch {
            var prevTotalTicks: Long = 0
            var prevIdleTicks: Long = 0
            var prevProcTicks: Long = 0
            val prevCores = mutableMapOf<String, Pair<Long, Long>>()

            val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            var uptime = 0L

            while (isActive) {
                delay(1000)
                if (_serverState.value.status == ServerStatus.RUNNING) {
                    uptime++
                } else if (_serverState.value.status == ServerStatus.STOPPED) {
                    uptime = 0
                }

                // 1. Pamięć RAM telefonu
                var totalDevMb = 0L
                var usedDevMb = 0L
                if (actMan != null) {
                    actMan.getMemoryInfo(memInfo)
                    totalDevMb = memInfo.totalMem / (1024 * 1024)
                    usedDevMb = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
                }

                // 2. Pamięć RAM procesu serwera (RSS)
                var serverUsedMb = 0L
                val currentProc = process
                val pid = currentProc?.let { getProcessPid(it) }
                if (pid != null) {
                    try {
                        val statmFile = java.io.File("/proc/$pid/statm")
                        if (statmFile.exists()) {
                            val parts = statmFile.readText().trim().split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                val rssPages = parts[1].toLongOrNull() ?: 0L
                                serverUsedMb = (rssPages * 4096) / (1024 * 1024)
                            }
                        }
                    } catch (ignored: Exception) {}
                }

                // 3. Obciążenie CPU telefonu i rdzeni z /proc/stat
                var devCpuPercent = 0f
                val coreList = mutableListOf<Float>()
                var serverCpuPercent = 0f

                try {
                    val statFile = java.io.File("/proc/stat")
                    if (statFile.exists()) {
                        val lines = statFile.readLines()
                        for (line in lines) {
                            if (line.startsWith("cpu ")) {
                                val tokens = line.substring(4).trim().split("\\s+".toRegex()).mapNotNull { it.toLongOrNull() }
                                if (tokens.size >= 4) {
                                    val total = tokens.sum()
                                    val idleAll = tokens[3] + (if (tokens.size > 4) tokens[4] else 0L)
                                    if (prevTotalTicks > 0) {
                                        val dTotal = total - prevTotalTicks
                                        val dIdle = idleAll - prevIdleTicks
                                        if (dTotal > 0) {
                                            devCpuPercent = ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
                                        }
                                    }
                                    prevTotalTicks = total
                                    prevIdleTicks = idleAll
                                }
                            } else if (line.matches(Regex("^cpu\\d+\\s+.*"))) {
                                val parts = line.split("\\s+".toRegex())
                                val coreId = parts[0]
                                val tokens = parts.drop(1).mapNotNull { it.toLongOrNull() }
                                if (tokens.size >= 4) {
                                    val total = tokens.sum()
                                    val idleAll = tokens[3] + (if (tokens.size > 4) tokens[4] else 0L)
                                    val prev = prevCores[coreId]
                                    if (prev != null) {
                                        val dTotal = total - prev.first
                                        val dIdle = idleAll - prev.second
                                        val cPercent = if (dTotal > 0) ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f) else 0f
                                        coreList.add(cPercent)
                                    } else {
                                        coreList.add(0f)
                                    }
                                    prevCores[coreId] = Pair(total, idleAll)
                                }
                            }
                        }
                    }

                    // Obciążenie serwera Minecraft z /proc/$pid/stat
                    if (pid != null && prevTotalTicks > 0) {
                        val pStatFile = java.io.File("/proc/$pid/stat")
                        if (pStatFile.exists()) {
                            val content = pStatFile.readText()
                            val rParen = content.lastIndexOf(')')
                            if (rParen != -1 && rParen + 2 < content.length) {
                                val rest = content.substring(rParen + 2).split(" ")
                                if (rest.size > 12) {
                                    val utime = rest[11].toLongOrNull() ?: 0L
                                    val stime = rest[12].toLongOrNull() ?: 0L
                                    val pTicks = utime + stime
                                    if (prevProcTicks > 0) {
                                        val dProc = pTicks - prevProcTicks
                                        val numCores = _serverState.value.allocatedCores
                                        serverCpuPercent = (dProc.toFloat() * 5f).coerceIn(0f, 100f * numCores)
                                    }
                                    prevProcTicks = pTicks
                                }
                            }
                        }
                    }
                } catch (ignored: Exception) {}

                _serverState.update {
                    it.copy(
                        uptimeSeconds = uptime,
                        deviceTotalMemoryMb = totalDevMb,
                        deviceUsedMemoryMb = usedDevMb,
                        usedMemoryMb = if (_serverState.value.status == ServerStatus.RUNNING) serverUsedMb else 0L,
                        maxMemoryMb = (it.allocatedRamGb * 1024).toLong(),
                        deviceCpuUsagePercent = devCpuPercent,
                        serverCpuUsagePercent = if (_serverState.value.status == ServerStatus.RUNNING) serverCpuPercent else 0f,
                        coreUsageList = if (coreList.isNotEmpty()) coreList else it.coreUsageList
                    )
                }
            }
        }
    }
}
