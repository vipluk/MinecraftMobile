package com.pearium.minecraftmobile.core

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val allocatedRamGb: Float = 4.0f,
    val allocatedCores: Int = 4,
    val totalCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
    val selectedFoliaVersion: String = "26.2",
    val installedFoliaVersion: String? = null,
    val availableFoliaVersions: List<String> = listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.20.6", "1.20.4", "1.19.4"),
    val cpuUsagePercent: Float = 0f,
    val serverCpuUsagePercent: Float = 0f,
    val deviceCpuUsagePercent: Float = 0f,
    val coreUsageList: List<Float> = emptyList(),
    val usedMemoryMb: Long = 0,
    val maxMemoryMb: Long = 4096,
    val deviceUsedMemoryMb: Long = 0,
    val deviceTotalMemoryMb: Long = 0,
    val playersOnline: Int = 0,
    val maxPlayers: Int = 20,
    val uptimeSeconds: Long = 0,
    val serverPort: Int = 25565,
    val relayConnected: Boolean = false,
    val errorMessage: String? = null
)
