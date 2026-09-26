package com.pearium.minecraftmobile.modrinth

import com.google.gson.annotations.SerializedName

data class ModrinthSearchResponse(
    val hits: List<ModrinthProjectHit> = emptyList(),
    val offset: Int = 0,
    val limit: Int = 20,
    @SerializedName("total_hits") val totalHits: Int = 0
)

data class ModrinthProjectHit(
    @SerializedName("project_id") val projectId: String,
    val title: String,
    val description: String,
    val categories: List<String> = emptyList(),
    val client_side: String? = null,
    val server_side: String? = null,
    val downloads: Int = 0,
    @SerializedName("icon_url") val iconUrl: String? = null,
    val author: String? = null,
    val slug: String? = null
)

data class ModrinthVersion(
    val id: String,
    @SerializedName("project_id") val projectId: String,
    val name: String,
    @SerializedName("version_number") val versionNumber: String,
    @SerializedName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val files: List<ModrinthVersionFile> = emptyList()
)

data class ModrinthVersionFile(
    val url: String,
    val filename: String,
    val primary: Boolean = false,
    val size: Long = 0
)

data class InstalledPlugin(
    val name: String,
    val filename: String,
    val isEnabled: Boolean,
    val sizeBytes: Long
)
