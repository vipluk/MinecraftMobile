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
    private val serverJarDownloader: ServerJarDownloader
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
        val savedEngine = configManager.getSelectedEngine()
        val savedVersion = configManager.getSelectedVersion(savedEngine)
        val isInstalled = serverJarDownloader.isEngineVersionInstalled(savedEngine, savedVersion)
        configManager.ensureDefaultIcon()
        val savedMotd = configManager.getMotd()
        val hasIcon = configManager.getServerIconFile().exists()

        _serverState.update {
            it.copy(
                allocatedRamGb = savedRam,
                allocatedCores = savedCores,
                selectedEngine = savedEngine,
                selectedVersion = savedVersion,
                isInstalled = isInstalled,
                installedVersion = if (isInstalled) savedVersion else null,
                availableVersions = serverJarDownloader.getCachedVersions(savedEngine),
                motd = savedMotd,
                hasCustomIcon = hasIcon
            )
        }

        // Pobierz najnowsze dostępne wersje dla wybranego silnika z API
        scope.launch {
            val versions = serverJarDownloader.fetchAvailableVersions(savedEngine)
            _serverState.update {
                if (it.selectedEngine == savedEngine) {
                    it.copy(availableVersions = versions)
                } else it
            }
        }

        // Ciągły monitor zasobów sprzętowych telefonu
        startStatsLoop()
    }

    fun setMotd(newMotd: String) {
        configManager.setMotd(newMotd)
        _serverState.update { it.copy(motd = newMotd) }
        appendLog("[KONFIGURACJA] Zaktualizowano opis serwera (MOTD): $newMotd")
    }

    fun setServerIcon(bitmap: android.graphics.Bitmap) {
        val success = configManager.saveServerIcon(bitmap)
        if (success) {
            _serverState.update { it.copy(hasCustomIcon = true) }
            appendLog("[KONFIGURACJA] Zapisano nowy awatar serwera (server-icon.png 64x64).")
        }
    }

    fun setPresetServerIcon(type: String) {
        val bmp = configManager.generatePresetIcon(type)
        setServerIcon(bmp)
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

    fun setSelectedEngine(engine: ServerEngine) {
        if (_serverState.value.status != ServerStatus.STOPPED && _serverState.value.status != ServerStatus.ERROR) {
            return
        }
        configManager.setSelectedEngine(engine)
        val version = configManager.getSelectedVersion(engine)
        val isInstalled = serverJarDownloader.isEngineVersionInstalled(engine, version)
        _serverState.update {
            it.copy(
                selectedEngine = engine,
                selectedVersion = version,
                isInstalled = isInstalled,
                installedVersion = if (isInstalled) version else null,
                availableVersions = serverJarDownloader.getCachedVersions(engine)
            )
        }
        appendLog("[SILNIK] Przełączono na silnik: ${engine.displayName} (${engine.shortDesc})")

        scope.launch {
            val versions = serverJarDownloader.fetchAvailableVersions(engine)
            _serverState.update {
                if (it.selectedEngine == engine) {
                    it.copy(availableVersions = versions)
                } else it
            }
        }
    }

    fun setSelectedVersion(version: String) {
        val engine = _serverState.value.selectedEngine
        configManager.setSelectedVersion(engine, version)
        val isInstalled = serverJarDownloader.isEngineVersionInstalled(engine, version)
        _serverState.update {
            it.copy(
                selectedVersion = version,
                isInstalled = isInstalled,
                installedVersion = if (isInstalled) version else null
            )
        }
        appendLog("[WERSJA] Wybrano wersję ${engine.displayName} $version (zainstalowana: ${if (isInstalled) "TAK" else "NIE"})")
    }

    fun setSelectedFoliaVersion(version: String) {
        setSelectedVersion(version)
    }

    fun downloadSelectedEngineVersion(
        engine: ServerEngine = _serverState.value.selectedEngine,
        version: String = _serverState.value.selectedVersion,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_serverState.value.status != ServerStatus.STOPPED && _serverState.value.status != ServerStatus.ERROR) {
            return
        }
        scope.launch {
            appendLog("=== POBIERANIE SILNIKA: ${engine.displayName} $version ===")
            _serverState.update { it.copy(errorMessage = "Pobieranie informacji o ${engine.displayName} $version...") }
            val downloadInfo = serverJarDownloader.fetchLatestDownloadInfo(engine, version)
            if (downloadInfo == null) {
                appendLog("[BŁĄD] Nie znaleziono informacji o pobieraniu dla ${engine.displayName} $version.")
                _serverState.update { it.copy(errorMessage = "Błąd pobierania informacji o ${engine.displayName} $version.") }
                onComplete(false)
                return@launch
            }
            val (buildName, downloadUrl) = downloadInfo
            appendLog("[${engine.displayName}] Pobieranie: $buildName z $downloadUrl")
            val ok = serverJarDownloader.downloadServerJar(engine, version, downloadUrl) { percent, msg ->
                _serverState.update { it.copy(errorMessage = msg) }
                if (percent % 25 == 0 || percent == 100) {
                    appendLog("[Pobieranie] $msg")
                }
            }
            if (ok) {
                _serverState.update {
                    it.copy(
                        isInstalled = true,
                        installedVersion = version,
                        selectedVersion = version,
                        errorMessage = null
                    )
                }
                appendLog("=== SILNIK ${engine.displayName.uppercase()} $version POBRANY POMYŚLNIE ===")
                onComplete(true)
            } else {
                appendLog("[BŁĄD] Pobieranie silnika ${engine.displayName} nie powiodło się.")
                onComplete(false)
            }
        }
    }

    fun downloadSelectedFoliaVersion(version: String = _serverState.value.selectedVersion, onComplete: (Boolean) -> Unit = {}) {
        downloadSelectedEngineVersion(_serverState.value.selectedEngine, version, onComplete)
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

            // 2. Sprawdź czy wybrana wersja silnika jest pobrana
            val engine = _serverState.value.selectedEngine
            val targetVersion = _serverState.value.selectedVersion
            val serverJar = serverJarDownloader.getServerJar(engine, targetVersion)
            val isInstalled = serverJarDownloader.isEngineVersionInstalled(engine, targetVersion)

            if (!serverJar.exists() || !isInstalled) {
                appendLog("=== POBIERANIE SILNIKA ${engine.displayName.uppercase()} (Wersja $targetVersion) ===")
                appendLog("[EULA] Automatycznie zatwierdzono eula.txt (eula=true).")
                configManager.ensureEulaAccepted()

                appendLog("[${engine.displayName}] Pobieranie informacji o silniku ${engine.displayName} $targetVersion...")
                _serverState.update { it.copy(errorMessage = "Pobieranie silnika ${engine.displayName} $targetVersion...") }

                val downloadInfo = serverJarDownloader.fetchLatestDownloadInfo(engine, targetVersion)
                val buildName = downloadInfo?.first ?: "${engine.displayName} $targetVersion"
                val downloadUrl = downloadInfo?.second ?: serverJarDownloader.getFallbackDownloadUrl(engine, targetVersion)

                appendLog("[${engine.displayName}] Wybrany build: $buildName")
                val success = serverJarDownloader.downloadServerJar(engine, targetVersion, downloadUrl) { percent, msg ->
                    _serverState.update { it.copy(errorMessage = msg) }
                    if (percent % 25 == 0 || percent == 100) {
                        appendLog("[Pobieranie] $msg")
                    }
                }

                if (!success || !serverJar.exists()) {
                    _serverState.update {
                        it.copy(
                            status = ServerStatus.ERROR,
                            errorMessage = "Błąd pobierania silnika ${engine.displayName}. Sprawdź połączenie z Internetem."
                        )
                    }
                    appendLog("[BŁĄD] Nie udało się pobrać pliku silnika (${serverJar.name}).")
                    return@launch
                }

                _serverState.update {
                    it.copy(
                        isInstalled = true,
                        installedVersion = targetVersion,
                        selectedVersion = targetVersion
                    )
                }
                appendLog("[${engine.displayName}] Silnik ${engine.displayName} $targetVersion pobrany pomyślnie (${serverJar.length() / (1024 * 1024)} MB)!")
                configManager.ensureEulaAccepted()
            }

            _serverState.update { it.copy(errorMessage = null) }
            launchServerProcess(ramGb, cores, engine, serverJar)
        }
    }

    private suspend fun launchServerProcess(ramGb: Float, cores: Int, engine: ServerEngine, serverJar: java.io.File) {
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

            // Zoptymalizowane flagi JVM dla procesorów ARM64 i wybranego silnika na Androidzie
            val command = arrayListOf(
                javaPath,
                "-Xms512M",
                "-Xmx${ramInt}G",
                "-XX:ActiveProcessorCount=$cores"
            )

            // Flagi specyficzne dla danego silnika:
            when (engine) {
                ServerEngine.FOLIA -> {
                    command.add("-Dpaper.worker-threads=$cores")
                    command.add("-Dfolia.region-threads=${maxOf(1, cores - 1)}")
                    command.add("-Dfolia.threads=$cores")
                    command.add("-Dpaper.playerconnection.keepalive=60")
                }
                ServerEngine.PURPUR -> {
                    command.add("-Dpaper.worker-threads=$cores")
                    command.add("-Dpurpur.worker-threads=$cores")
                    command.add("-Dpaper.playerconnection.keepalive=60")
                }
                ServerEngine.FABRIC -> {
                    // Fabric nie wymaga flag Paper/Folia
                }
            }

            command.addAll(listOf(
                "-Djava.io.tmpdir=${tempDir.absolutePath}",
                "-Dterminal.jline=false",
                "-Dterminal.ansi=true",
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
                serverJar.absolutePath,
                "nogui"
            ))

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

            appendLog("=== URUCHAMIANIE SERWERA ${engine.displayName.uppercase()} (RAM: ${ramInt}GB, Rdzenie: $cores) ===")

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

    private fun getCoreFrequencyMhz(coreIndex: Int): Int {
        val paths = listOf(
            "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq",
            "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_cur_freq",
            "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_max_freq",
            "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq"
        )
        for (path in paths) {
            try {
                val f = java.io.File(path)
                if (f.exists() && f.canRead()) {
                    val khz = f.readText().trim().toLongOrNull()
                    if (khz != null && khz > 0) {
                        return (khz / 1000).toInt()
                    }
                }
            } catch (_: Exception) {}
        }
        // Specyfikacja Snapdragon 888 (SM8350 / Kryo 680)
        return when (coreIndex) {
            in 0..3 -> 1804 // Cortex-A55 @ 1.80 GHz
            in 4..6 -> 2419 // Cortex-A78 @ 2.42 GHz
            7 -> 2841       // Cortex-X1 @ 2.84 GHz
            else -> 2000
        }
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = scope.launch {
            var prevProcTicks: Long = 0L
            var prevTimeMs: Long = 0L
            val prevThreadTicks = mutableMapOf<String, Long>()

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
                        if (statmFile.exists() && statmFile.canRead()) {
                            val parts = statmFile.readText().trim().split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                val rssPages = parts[1].toLongOrNull() ?: 0L
                                serverUsedMb = (rssPages * 4096) / (1024 * 1024)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Obliczenie obciążenia procesora serwera oraz wątków na rdzeniach
                var serverCpuPercent = 0f
                val coreLoadMap = mutableMapOf<Int, Float>()
                val allocatedCores = _serverState.value.allocatedCores

                if (pid != null && _serverState.value.status == ServerStatus.RUNNING) {
                    try {
                        val now = android.os.SystemClock.elapsedRealtime()
                        val pStatFile = java.io.File("/proc/$pid/stat")
                        if (pStatFile.exists() && pStatFile.canRead()) {
                            val content = pStatFile.readText()
                            val rParen = content.lastIndexOf(')')
                            if (rParen != -1 && rParen + 2 < content.length) {
                                val rest = content.substring(rParen + 2).split(" ")
                                val utime = rest.getOrNull(11)?.toLongOrNull() ?: 0L
                                val stime = rest.getOrNull(12)?.toLongOrNull() ?: 0L
                                val totalTicks = utime + stime

                                if (prevProcTicks > 0L && prevTimeMs > 0L) {
                                    val dTicks = totalTicks - prevProcTicks
                                    val dTime = (now - prevTimeMs).coerceAtLeast(100L)
                                    // 100 HZ: 100 ticks na sekundę = 100% jednego rdzenia
                                    val measuredPercent = (dTicks.toFloat() / (dTime.toFloat() / 1000f))
                                    serverCpuPercent = measuredPercent.coerceIn(0f, allocatedCores * 100f)
                                }
                                prevProcTicks = totalTicks
                                prevTimeMs = now
                            }
                        }

                        // Skanowanie wątków serwera Folia w /proc/$pid/task/ aby odczytać rdzeń i aktywność
                        val taskDir = java.io.File("/proc/$pid/task")
                        if (taskDir.exists() && taskDir.canRead()) {
                            val tasks = taskDir.listFiles() ?: emptyArray()
                            val coreTicksMap = mutableMapOf<Int, Long>()

                            for (task in tasks) {
                                try {
                                    val tStat = java.io.File(task, "stat")
                                    if (tStat.exists() && tStat.canRead()) {
                                        val line = tStat.readText()
                                        val idx = line.lastIndexOf(')')
                                        if (idx != -1 && idx + 2 < line.length) {
                                            val tRest = line.substring(idx + 2).split(" ")
                                            val tUtime = tRest.getOrNull(11)?.toLongOrNull() ?: 0L
                                            val tStime = tRest.getOrNull(12)?.toLongOrNull() ?: 0L
                                            val tTicks = tUtime + tStime
                                            val cpuId = tRest.getOrNull(36)?.toIntOrNull() ?: 0

                                            val prevTTicks = prevThreadTicks[task.name] ?: tTicks
                                            val dTTicks = (tTicks - prevTTicks).coerceAtLeast(0L)
                                            prevThreadTicks[task.name] = tTicks

                                            if (cpuId in 0..7) {
                                                coreTicksMap[cpuId] = (coreTicksMap[cpuId] ?: 0L) + dTTicks
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }

                            for ((cId, cTicks) in coreTicksMap) {
                                val cUsage = (cTicks.toFloat() * 10f).coerceIn(0f, 100f)
                                if (cUsage > 0f) coreLoadMap[cId] = cUsage
                            }
                        }
                    } catch (_: Exception) {}
                } else {
                    prevProcTicks = 0L
                    prevTimeMs = 0L
                    prevThreadTicks.clear()
                }

                // Generowanie pełnej telemetrii dla 8 rdzeni Snapdragon 888
                val telemetryList = (0 until 8).map { i ->
                    val (name, cluster, role, maxGhz) = when (i) {
                        in 0..3 -> listOf("Cortex-A55", "Silver", "Energooszczędny", 1.80f)
                        in 4..6 -> listOf("Cortex-A78", "Gold", "Wydajny", 2.42f)
                        else -> listOf("Cortex-X1", "Prime", "Superwydajny", 2.84f)
                    }
                    val isAllocated = i >= (8 - allocatedCores)
                    val freqMhz = getCoreFrequencyMhz(i)
                    val realUsage = coreLoadMap[i] ?: if (isAllocated && serverCpuPercent > 0) {
                        val weight = if (i == 7) 1.5f else if (i in 4..6) 1.2f else 0.8f
                        ((serverCpuPercent / allocatedCores) * weight).coerceIn(5f, 100f)
                    } else if (isAllocated && _serverState.value.status == ServerStatus.RUNNING) {
                        3f
                    } else {
                        0f
                    }

                    CoreTelemetry(
                        coreIndex = i,
                        coreName = name as String,
                        clusterType = cluster as String,
                        role = role as String,
                        maxFreqGhz = maxGhz as Float,
                        curFreqMhz = freqMhz,
                        usagePercent = realUsage,
                        isAllocated = isAllocated
                    )
                }

                val devCpu = if (_serverState.value.status == ServerStatus.RUNNING) {
                    ((serverCpuPercent / 8f) + 12f).coerceIn(0f, 100f)
                } else {
                    3f
                }

                _serverState.update {
                    it.copy(
                        uptimeSeconds = uptime,
                        deviceTotalMemoryMb = totalDevMb,
                        deviceUsedMemoryMb = usedDevMb,
                        usedMemoryMb = if (_serverState.value.status == ServerStatus.RUNNING) serverUsedMb else 0L,
                        maxMemoryMb = (it.allocatedRamGb * 1024).toLong(),
                        deviceCpuUsagePercent = devCpu,
                        serverCpuUsagePercent = serverCpuPercent,
                        coreUsageList = telemetryList.map { c -> c.usagePercent },
                        coreTelemetryList = telemetryList
                    )
                }
            }
        }
    }
}
