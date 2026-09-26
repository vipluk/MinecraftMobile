package com.pearium.minecraftmobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pearium.minecraftmobile.modrinth.InstalledPlugin
import com.pearium.minecraftmobile.modrinth.ModrinthApiService
import com.pearium.minecraftmobile.modrinth.ModrinthProjectHit
import com.pearium.minecraftmobile.modrinth.PluginManager
import com.pearium.minecraftmobile.ui.theme.CardBackground
import com.pearium.minecraftmobile.ui.theme.CardBorder
import com.pearium.minecraftmobile.ui.theme.DangerRed
import com.pearium.minecraftmobile.ui.theme.DarkEmerald
import com.pearium.minecraftmobile.ui.theme.EmeraldGreen
import com.pearium.minecraftmobile.ui.theme.MintAccent
import com.pearium.minecraftmobile.ui.theme.ObsidianDark
import com.pearium.minecraftmobile.ui.theme.TextPrimary
import com.pearium.minecraftmobile.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PluginsScreen(
    pluginManager: PluginManager,
    modrinthService: ModrinthApiService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("EssentialsX") }
    var isSearching by remember { mutableStateOf(false) }

    val searchResults = remember { mutableStateListOf<ModrinthProjectHit>() }
    val installedPlugins = remember { mutableStateListOf<InstalledPlugin>() }

    fun refreshInstalled() {
        installedPlugins.clear()
        installedPlugins.addAll(pluginManager.getInstalledPlugins())
    }

    LaunchedEffect(Unit) {
        refreshInstalled()
        // Domyślne wyszukanie popularnych pluginów
        isSearching = true
        val results = modrinthService.searchPlugins("Essentials")
        searchResults.clear()
        searchResults.addAll(results)
        isSearching = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        Text(
            text = "Menedżer Pluginów",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = "Integracja 1-kliknięciem z bazą Modrinth",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Zakładki
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = CardBackground,
            contentColor = EmeraldGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = EmeraldGreen
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Wyszukiwarka Modrinth", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = {
                    selectedTabIndex = 1
                    refreshInstalled()
                },
                text = { Text("Zainstalowane (${installedPlugins.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTabIndex == 0) {
            // Zakładka 1: Szukanie w Modrinth
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Szukaj pluginów (np. LuckPerms, WorldEdit)...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSearching = true
                            val hits = modrinthService.searchPlugins(searchQuery)
                            searchResults.clear()
                            searchResults.addAll(hits)
                            isSearching = false
                        }
                    },
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Szukaj", color = ObsidianDark, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldGreen)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak wyników wyszukiwania", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults) { hit ->
                        var isDownloading by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hit.title,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Autor: ${hit.author ?: "Nieznany"} • Pobrań: ${hit.downloads}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            isDownloading = true
                                            scope.launch {
                                                val success = pluginManager.installPluginFromModrinth(hit.projectId) { _, msg -> }
                                                isDownloading = false
                                                if (success) {
                                                    Toast.makeText(context, "Zainstalowano: ${hit.title}!", Toast.LENGTH_SHORT).show()
                                                    refreshInstalled()
                                                } else {
                                                    Toast.makeText(context, "Błąd instalacji ${hit.title}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = !isDownloading,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkEmerald)
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MintAccent, strokeWidth = 2.dp)
                                        } else {
                                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = MintAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Instaluj", color = MintAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = hit.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Zakładka 2: Zainstalowane pluginy
            if (installedPlugins.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Brak zainstalowanych wtyczek w folderze plugins/", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(installedPlugins) { plugin ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (plugin.isEnabled) EmeraldGreen else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = plugin.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (plugin.isEnabled) "Aktywny (${plugin.sizeBytes / 1024} KB)" else "Wyłączony",
                                            color = if (plugin.isEnabled) MintAccent else TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = plugin.isEnabled,
                                        onCheckedChange = {
                                            pluginManager.togglePlugin(plugin)
                                            refreshInstalled()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = EmeraldGreen,
                                            checkedTrackColor = DarkEmerald,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = CardBorder
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            pluginManager.deletePlugin(plugin)
                                            refreshInstalled()
                                            Toast.makeText(context, "Usunięto ${plugin.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Usuń",
                                            tint = DangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
