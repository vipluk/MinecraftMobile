package com.pearium.minecraftmobile.core

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

data class CoreTelemetry(
    val coreIndex: Int,          // 0 do 7
    val coreName: String,        // "Cortex-A55", "Cortex-A78", "Cortex-X1"
    val clusterType: String,     // "Silver", "Gold", "Prime"
    val role: String,            // "Energooszczędny", "Wydajny", "Prime"
    val maxFreqGhz: Float,       // 1.80f, 2.42f, 2.84f
    val curFreqMhz: Int,         // np. 1804
    val usagePercent: Float,     // 0f do 100f
    val isAllocated: Boolean     // czy przydzielony serwerowi Folia
)

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val allocatedRamGb: Float = 4.0f,
    val allocatedCores: Int = 4,
    val totalCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(8),
    val selectedFoliaVersion: String = "26.2",
    val installedFoliaVersion: String? = null,
    val availableFoliaVersions: List<String> = listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.20.6", "1.20.4", "1.19.4"),
    val cpuUsagePercent: Float = 0f,
    val serverCpuUsagePercent: Float = 0f,
    val deviceCpuUsagePercent: Float = 0f,
    val coreUsageList: List<Float> = emptyList(),
    val coreTelemetryList: List<CoreTelemetry> = emptyList(),
    val usedMemoryMb: Long = 0,
    val maxMemoryMb: Long = 4096,
    val deviceUsedMemoryMb: Long = 0,
    val deviceTotalMemoryMb: Long = 0,
    val playersOnline: Int = 0,
    val maxPlayers: Int = 20,
    val uptimeSeconds: Long = 0,
    val serverPort: Int = 25565,
    val relayConnected: Boolean = false,
    val errorMessage: String? = null,
    val motd: String = "§aMinecraft Mobile Server §7(Xiaomi 11T Pro)",
    val hasCustomIcon: Boolean = false
)
