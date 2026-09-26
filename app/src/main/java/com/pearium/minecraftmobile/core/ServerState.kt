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
    val cpuUsagePercent: Float = 0f,
    val usedMemoryMb: Long = 0,
    val maxMemoryMb: Long = 4096,
    val playersOnline: Int = 0,
    val maxPlayers: Int = 20,
    val uptimeSeconds: Long = 0,
    val serverPort: Int = 25565,
    val relayConnected: Boolean = false,
    val errorMessage: String? = null
)
