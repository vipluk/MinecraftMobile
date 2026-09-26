package com.pearium.minecraftmobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pearium.minecraftmobile.core.ConfigManager
import com.pearium.minecraftmobile.ui.theme.CardBackground
import com.pearium.minecraftmobile.ui.theme.CardBorder
import com.pearium.minecraftmobile.ui.theme.ConsoleBackground
import com.pearium.minecraftmobile.ui.theme.DarkEmerald
import com.pearium.minecraftmobile.ui.theme.EmeraldGreen
import com.pearium.minecraftmobile.ui.theme.ObsidianDark
import com.pearium.minecraftmobile.ui.theme.TextPrimary
import com.pearium.minecraftmobile.ui.theme.TextSecondary

@Composable
fun FileManagerScreen(
    configManager: ConfigManager
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // Stan właściwości serwera
    val properties = remember { mutableStateMapOf<String, String>() }

    // Stan edytora surowego tekstu
    var selectedRawFile by remember { mutableStateOf("server.properties") }
    var rawFileContent by remember { mutableStateOf("") }

    fun loadProps() {
        properties.clear()
        properties.putAll(configManager.readProperties())
    }

    fun loadRaw(filename: String) {
        selectedRawFile = filename
        rawFileContent = configManager.readRawFile(filename)
    }

    LaunchedEffect(Unit) {
        loadProps()
        loadRaw("server.properties")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        Text(
            text = "Konfiguracja Serwera",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = "Dostosuj server.properties i pliki konfiguracyjne",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBackground,
            contentColor = EmeraldGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EmeraldGreen
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    loadProps()
                },
                text = { Text("Kreator Ustawień", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    loadRaw(selectedRawFile)
                },
                text = { Text("Edytor Plików (RAW)", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Zakładka 1: Wizualny edytor opcji
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // MOTD
                        OutlinedTextField(
                            value = properties["motd"] ?: "",
                            onValueChange = { properties["motd"] = it },
                            label = { Text("Opis serwera (MOTD)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        // Max graczy & Port
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = properties["max-players"] ?: "20",
                                onValueChange = { properties["max-players"] = it },
                                label = { Text("Limit graczy") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            OutlinedTextField(
                                value = properties["server-port"] ?: "25565",
                                onValueChange = { properties["server-port"] = it },
                                label = { Text("Port (domyślnie 25565)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // Tryb Online (Premium / Non-Premium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Tryb Online (Tylko konta Premium)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (properties["online-mode"] == "true") "Wymaga oryginalnego Minecrafta" else "Dostępny dla wszystkich (Non-Premium)",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = properties["online-mode"] == "true",
                                onCheckedChange = { isChecked ->
                                    properties["online-mode"] = isChecked.toString()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = DarkEmerald
                                )
                            )
                        }

                        // PvP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Walka między graczami (PvP)", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (properties["pvp"] == "true") "Włączone" else "Wyłączone",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = properties["pvp"] == "true",
                                onCheckedChange = { isChecked ->
                                    properties["pvp"] = isChecked.toString()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = DarkEmerald
                                )
                            )
                        }

                        // Przycisk Zapisu
                        Button(
                            onClick = {
                                configManager.updateProperties(properties.toMap())
                                Toast.makeText(context, "Zapisano konfigurację serwera!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = ObsidianDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zapisz Ustawienia", color = ObsidianDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Zakładka 2: Edytor plików bezpośredni
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("server.properties", "eula.txt", "bukkit.yml").forEach { fn ->
                        Button(
                            onClick = { loadRaw(fn) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRawFile == fn) EmeraldGreen else CardBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = fn,
                                color = if (selectedRawFile == fn) ObsidianDark else TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawFileContent,
                    onValueChange = { rawFileContent = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ConsoleBackground,
                        unfocusedContainerColor = ConsoleBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        configManager.saveRawFile(selectedRawFile, rawFileContent)
                        Toast.makeText(context, "Zapisano plik $selectedRawFile!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = ObsidianDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zapisz Plik", color = ObsidianDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
