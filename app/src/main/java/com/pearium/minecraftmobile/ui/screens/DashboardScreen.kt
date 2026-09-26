package com.pearium.minecraftmobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pearium.minecraftmobile.core.ServerState
import com.pearium.minecraftmobile.core.ServerStatus
import com.pearium.minecraftmobile.ui.theme.CardBackground
import com.pearium.minecraftmobile.ui.theme.CardBorder
import com.pearium.minecraftmobile.ui.theme.DangerRed
import com.pearium.minecraftmobile.ui.theme.DarkEmerald
import com.pearium.minecraftmobile.ui.theme.EmeraldGreen
import com.pearium.minecraftmobile.ui.theme.MintAccent
import com.pearium.minecraftmobile.ui.theme.ObsidianDark
import com.pearium.minecraftmobile.ui.theme.TextPrimary
import com.pearium.minecraftmobile.ui.theme.TextSecondary
import com.pearium.minecraftmobile.ui.theme.WarningYellow

@Composable
fun DashboardScreen(
    serverState: ServerState,
    onStartServer: (Float, Int) -> Unit,
    onStopServer: () -> Unit,
    onRamChange: (Float) -> Unit,
    onCoresChange: (Int) -> Unit,
    onVersionChange: (String) -> Unit,
    onDownloadVersion: (String) -> Unit,
    onNavigateToConsole: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var ramSliderValue by remember(serverState.allocatedRamGb) {
        mutableFloatStateOf(serverState.allocatedRamGb)
    }
    var ramTextValue by remember(serverState.allocatedRamGb) {
        mutableStateOf(serverState.allocatedRamGb.toInt().toString())
    }
    var coreSliderValue by remember(serverState.allocatedCores) {
        mutableIntStateOf(serverState.allocatedCores)
    }

    var versionDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Nagłówek i status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Minecraft Folia Mobile",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Xiaomi 11T Pro • Snapdragon 888 (${serverState.totalCores} rdzeni)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Wskaźnik statusu
                    val statusColor = when (serverState.status) {
                        ServerStatus.RUNNING -> EmeraldGreen
                        ServerStatus.STARTING -> WarningYellow
                        ServerStatus.STOPPING -> WarningYellow
                        ServerStatus.STOPPED -> TextSecondary
                        ServerStatus.ERROR -> DangerRed
                    }
                    val statusLabel = when (serverState.status) {
                        ServerStatus.RUNNING -> "ONLINE"
                        ServerStatus.STARTING -> "STARTUJE..."
                        ServerStatus.STOPPING -> "ZAMYKANIE"
                        ServerStatus.STOPPED -> "OFFLINE"
                        ServerStatus.ERROR -> "BŁĄD"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                serverState.errorMessage?.let { errorMsg ->
                    val isInfo = serverState.status == ServerStatus.STARTING || errorMsg.startsWith("Pobieranie")
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                (if (isInfo) EmeraldGreen else DangerRed).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = errorMsg,
                            color = if (isInfo) MintAccent else DangerRed,
                            fontSize = 13.sp,
                            fontWeight = if (isInfo) FontWeight.Medium else FontWeight.Normal
                        )
                        if (!isInfo) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "👉 Kliknij tutaj, aby otworzyć Konsolę i zobaczyć pełne logi",
                                color = MintAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onNavigateToConsole() }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Karta Wyboru Wersji Folia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wersja Silnika Folia",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    val isServerStopped = serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.ERROR

                    // Przycisk wyboru wersji z rozwijanym menu
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = isServerStopped) {
                                    versionDropdownExpanded = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${serverState.selectedFoliaVersion}",
                                color = MintAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Wybierz wersję",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = versionDropdownExpanded,
                            onDismissRequest = { versionDropdownExpanded = false },
                            modifier = Modifier
                                .background(CardBackground)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        ) {
                            serverState.availableFoliaVersions.forEach { version ->
                                val isSelected = version == serverState.selectedFoliaVersion
                                val isInstalled = version == serverState.installedFoliaVersion
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Folia $version" + if (version == "26.2") " (Najnowsza)" else "",
                                                color = if (isSelected) EmeraldGreen else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            if (isInstalled) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Pobrana",
                                                    color = MintAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        versionDropdownExpanded = false
                                        onVersionChange(version)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Informacja o stanie zainstalowania wybranej wersji
                val isTargetInstalled = serverState.installedFoliaVersion == serverState.selectedFoliaVersion
                if (isTargetInstalled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Plik Folia ${serverState.selectedFoliaVersion} jest zainstalowany i gotowy do startu.",
                            color = EmeraldGreen,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarningYellow.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wybrano inną wersję: ${serverState.selectedFoliaVersion}",
                                color = WarningYellow,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Pobierze się automatycznie po starcie lub kliknij pobierz.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.ERROR) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onDownloadVersion(serverState.selectedFoliaVersion) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MintAccent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MintAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pobierz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Karta Przydziału Zasobów: RAM i Rdzenie CPU
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Sekcja RAM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Przydział Pamięci RAM",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedTextField(
                        value = ramTextValue,
                        onValueChange = { input ->
                            ramTextValue = input
                            val parsed = input.toFloatOrNull()
                            if (parsed != null && parsed in 1.0f..8.0f) {
                                ramSliderValue = parsed
                                onRamChange(parsed)
                            }
                        },
                        modifier = Modifier.width(90.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        suffix = { Text("GB", color = TextSecondary, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Slider(
                    value = ramSliderValue,
                    onValueChange = { newVal ->
                        ramSliderValue = newVal
                        ramTextValue = newVal.toInt().toString()
                        onRamChange(newVal)
                    },
                    valueRange = 1.0f..8.0f,
                    steps = 6,
                    enabled = serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.ERROR,
                    colors = SliderDefaults.colors(
                        thumbColor = MintAccent,
                        activeTrackColor = EmeraldGreen,
                        inactiveTrackColor = CardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 GB", color = TextSecondary, fontSize = 11.sp)
                    Text("Domyślnie 4 GB", color = TextSecondary, fontSize = 11.sp)
                    Text("8 GB (Maks)", color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
                Spacer(modifier = Modifier.height(14.dp))

                // Sekcja Wyboru Rdzeni CPU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Przydział Rdzeni CPU",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "${coreSliderValue} z ${serverState.totalCores} rdzeni",
                        color = MintAccent,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }

                Slider(
                    value = coreSliderValue.toFloat(),
                    onValueChange = { newVal ->
                        val cores = newVal.toInt()
                        coreSliderValue = cores
                        onCoresChange(cores)
                    },
                    valueRange = 1.0f..serverState.totalCores.toFloat(),
                    steps = (serverState.totalCores - 2).coerceAtLeast(0),
                    enabled = serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.ERROR,
                    colors = SliderDefaults.colors(
                        thumbColor = MintAccent,
                        activeTrackColor = EmeraldGreen,
                        inactiveTrackColor = CardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 rdzeń", color = TextSecondary, fontSize = 11.sp)
                    Text("Zalecane: 4 rdzenie", color = TextSecondary, fontSize = 11.sp)
                    Text("${serverState.totalCores} rdzeni (Maks)", color = TextSecondary, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Silnik Folia przydziela dedykowane wątki do równoległego tickingowania każdego regionu świata na wybranych rdzeniach.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // 4. Przyciski Akcji: Start / Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val isRunning = serverState.status == ServerStatus.RUNNING || serverState.status == ServerStatus.STARTING

            Button(
                onClick = {
                    if (isRunning) {
                        onStopServer()
                    } else {
                        onStartServer(ramSliderValue, coreSliderValue)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) DangerRed else EmeraldGreen
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ObsidianDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Zatrzymaj Serwer" else "Włącz Serwer",
                    color = ObsidianDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = onNavigateToConsole,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Konsola",
                    tint = TextPrimary
                )
            }
        }

        // 5. Karta Adresu Serwera (pearium.com)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ADRES SERWERA MINECRAFT",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianDark)
                        .clickable {
                            clipboardManager.setText(AnnotatedString("pearium.com:25565"))
                            Toast.makeText(context, "Skopiowano adres pearium.com:25565", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "pearium.com:25565",
                        color = MintAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Kopiuj",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "W przeglądarce: pearium.com otwiera stronę WWW • W Minecraft: łączy z Twoim telefonem",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // 6. Karta Monitora Sprzętowego (Live Telemetria RAM i CPU Cores)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONITOR SPRZĘTOWY (CZAS RZECZYWISTY)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "LIVE",
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zużycie RAM Telefonu
                val deviceTotalRamGb = if (serverState.deviceTotalMemoryMb > 0) serverState.deviceTotalMemoryMb / 1024f else 12f
                val deviceUsedRamGb = if (serverState.deviceUsedMemoryMb > 0) serverState.deviceUsedMemoryMb / 1024f else 0f
                val deviceRamPercent = if (deviceTotalRamGb > 0) (deviceUsedRamGb / deviceTotalRamGb).coerceIn(0f, 1f) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAM Telefonu",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", deviceUsedRamGb)} GB / ${String.format("%.1f", deviceTotalRamGb)} GB (${(deviceRamPercent * 100).toInt()}%)",
                        color = MintAccent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { deviceRamPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (deviceRamPercent > 0.85f) DangerRed else if (deviceRamPercent > 0.70f) WarningYellow else EmeraldGreen,
                    trackColor = ObsidianDark,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Zużycie RAM Serwera
                val isRunning = serverState.status == ServerStatus.RUNNING
                val serverMaxRamGb = serverState.allocatedRamGb
                val serverUsedRamGb = if (isRunning) serverState.usedMemoryMb / 1024f else 0f
                val serverRamPercent = if (serverMaxRamGb > 0) (serverUsedRamGb / serverMaxRamGb).coerceIn(0f, 1f) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAM Serwera (Folia)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (isRunning) {
                            "${String.format("%.1f", serverUsedRamGb)} GB / ${serverMaxRamGb.toInt()} GB (${(serverRamPercent * 100).toInt()}%)"
                        } else {
                            "0.0 GB / ${serverMaxRamGb.toInt()} GB (Offline)"
                        },
                        color = if (isRunning) MintAccent else TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { serverRamPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (serverRamPercent > 0.90f) DangerRed else if (serverRamPercent > 0.75f) WarningYellow else MintAccent,
                    trackColor = ObsidianDark,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
                Spacer(modifier = Modifier.height(14.dp))

                // Sekcja Wykorzystania Procesora (CPU)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CPU Telefonu",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${serverState.deviceCpuUsagePercent.toInt()}%",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CPU Serwera Folia",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isRunning) "${serverState.serverCpuUsagePercent.toInt()}%" else "0%",
                            color = if (isRunning) MintAccent else TextSecondary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktywne rdzenie",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${serverState.allocatedCores} / ${serverState.totalCores}",
                            color = EmeraldGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "WYKORZYSTANIE RDZENI PROCESORA (Snapdragon 888)",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Wykres słupkowy obciążenia każdego z rdzeni CPU
                val coresCount = serverState.totalCores
                val coreUsages = if (serverState.coreUsageList.isNotEmpty()) {
                    serverState.coreUsageList
                } else {
                    List(coresCount) { 0f }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianDark)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (i in 0 until coresCount) {
                        val usage = coreUsages.getOrElse(i) { 0f }
                        val isAllocated = i < serverState.allocatedCores
                        val barColor = if (!isAllocated) {
                            TextSecondary.copy(alpha = 0.3f)
                        } else if (usage > 80f) {
                            DangerRed
                        } else if (usage > 50f) {
                            WarningYellow
                        } else {
                            EmeraldGreen
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${usage.toInt()}%",
                                color = if (isAllocated) barColor else TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CardBackground),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight((usage / 100f).coerceIn(0.08f, 1f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "R${i + 1}",
                                color = if (isAllocated) TextPrimary else TextSecondary.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = if (isAllocated) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 7. Podstawowe Wskaźniki Serwera
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATUS ROZGRYWKI",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        icon = Icons.Default.People,
                        label = "Gracze online",
                        value = "${serverState.playersOnline} / ${serverState.maxPlayers}"
                    )
                    StatItem(
                        icon = Icons.Default.Extension,
                        label = "Wersja silnika",
                        value = "v${serverState.installedFoliaVersion ?: serverState.selectedFoliaVersion}"
                    )
                    StatItem(
                        icon = Icons.Default.Timer,
                        label = "Czas pracy",
                        value = formatUptime(serverState.uptimeSeconds)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}

private fun formatUptime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) "%02d:%02d:%02d".format(hrs, mins, secs) else "%02d:%02d".format(mins, secs)
}
