package com.pearium.minecraftmobile.core

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

enum class ServerEngine(val id: String, val displayName: String, val shortDesc: String) {
    FOLIA("folia", "Folia", "Wielowątkowy silnik regionalny (PaperMC)"),
    PURPUR("purpur", "Purpur", "Wysokowydajny fork Paper z optymalizacjami"),
    FABRIC("fabric", "Fabric", "Lekki, modułowy silnik z obsługą modów Fabric");

    companion object {
        fun fromId(id: String): ServerEngine {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.displayName.equals(id, ignoreCase = true) } ?: FOLIA
        }
    }
}

data class CoreTelemetry(
    val coreIndex: Int,          // 0 do 7
    val coreName: String,        // "Cortex-A55", "Cortex-A78", "Cortex-X1"
    val clusterType: String,     // "Silver", "Gold", "Prime"
    val role: String,            // "Energooszczędny", "Wydajny", "Prime"
    val maxFreqGhz: Float,       // 1.80f, 2.42f, 2.84f
    val curFreqMhz: Int,         // np. 1804
    val usagePercent: Float,     // 0f do 100f
    val isAllocated: Boolean     // czy przydzielony serwerowi
)

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val allocatedRamGb: Float = 4.0f,
    val allocatedCores: Int = 4,
    val totalCores: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(8),
    val selectedEngine: ServerEngine = ServerEngine.FOLIA,
    val availableEngines: List<ServerEngine> = ServerEngine.entries,
    val selectedVersion: String = "26.2",
    val installedVersion: String? = null,
    val isInstalled: Boolean = false,
    val availableVersions: List<String> = listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.20.6", "1.20.4", "1.19.4"),
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
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val downloadStatusMessage: String? = null,
    val motd: String = "§aMinecraft Mobile Server §7(Xiaomi 11T Pro)",
    val hasCustomIcon: Boolean = false
) {
    // Kompatybilność wsteczna z wcześniejszymi polami Folia
    val selectedFoliaVersion: String get() = selectedVersion
    val installedFoliaVersion: String? get() = if (isInstalled) selectedVersion else installedVersion
    val availableFoliaVersions: List<String> get() = availableVersions
}
