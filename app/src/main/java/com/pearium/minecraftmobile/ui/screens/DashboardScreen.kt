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
import androidx.compose.material3.CircularProgressIndicator
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
import com.pearium.minecraftmobile.core.ServerEngine
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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.pearium.minecraftmobile.core.ConfigManager

fun parseMinecraftMotd(motd: String): AnnotatedString {
    return buildAnnotatedString {
        var currentColor = Color(0xFFAAAAAA)
        var isBold = false
        var isItalic = false

        var i = 0
        while (i < motd.length) {
            val c = motd[i]
            if ((c == '§' || c == '&') && i + 1 < motd.length) {
                when (motd[i + 1].lowercaseChar()) {
                    '0' -> { currentColor = Color(0xFF000000); isBold = false; isItalic = false }
                    '1' -> { currentColor = Color(0xFF0000AA); isBold = false; isItalic = false }
                    '2' -> { currentColor = Color(0xFF00AA00); isBold = false; isItalic = false }
                    '3' -> { currentColor = Color(0xFF00AAAA); isBold = false; isItalic = false }
                    '4' -> { currentColor = Color(0xFFAA0000); isBold = false; isItalic = false }
                    '5' -> { currentColor = Color(0xFFAA00AA); isBold = false; isItalic = false }
                    '6' -> { currentColor = Color(0xFFFFAA00); isBold = false; isItalic = false }
                    '7' -> { currentColor = Color(0xFFAAAAAA); isBold = false; isItalic = false }
                    '8' -> { currentColor = Color(0xFF555555); isBold = false; isItalic = false }
                    '9' -> { currentColor = Color(0xFF5555FF); isBold = false; isItalic = false }
                    'a' -> { currentColor = Color(0xFF55FF55); isBold = false; isItalic = false }
                    'b' -> { currentColor = Color(0xFF55FFFF); isBold = false; isItalic = false }
                    'c' -> { currentColor = Color(0xFFFF5555); isBold = false; isItalic = false }
                    'd' -> { currentColor = Color(0xFFFF55FF); isBold = false; isItalic = false }
                    'e' -> { currentColor = Color(0xFFFFFF55); isBold = false; isItalic = false }
                    'f' -> { currentColor = Color(0xFFFFFFFF); isBold = false; isItalic = false }
                    'l' -> isBold = true
                    'o' -> isItalic = true
                    'r' -> { currentColor = Color(0xFFAAAAAA); isBold = false; isItalic = false }
                }
                i += 2
                continue
            }
            withStyle(
                SpanStyle(
                    color = currentColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                )
            ) {
                append(c)
            }
            i++
        }
    }
}

@Composable
fun DashboardScreen(
    serverState: ServerState,
    onStartServer: (Float, Int) -> Unit,
    onStopServer: () -> Unit,
    onRamChange: (Float) -> Unit,
    onCoresChange: (Int) -> Unit,
    onEngineChange: (ServerEngine) -> Unit = {},
    onVersionChange: (String) -> Unit,
    onDownloadVersion: (String) -> Unit,
    onNavigateToConsole: () -> Unit,
    configManager: ConfigManager? = null,
    onUpdateMotd: (String) -> Unit = {},
    onUpdateIcon: (Bitmap) -> Unit = {},
    onSetPresetIcon: (String) -> Unit = {}
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

    var motdText by remember(serverState.motd) { mutableStateOf(serverState.motd) }
    var serverIconBitmap by remember(serverState.hasCustomIcon) { mutableStateOf(configManager?.getServerIconBitmap()) }
    var isEditingAppearance by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val original = BitmapFactory.decodeStream(stream)
                    if (original != null) {
                        val scaled = Bitmap.createScaledBitmap(original, 64, 64, true)
                        serverIconBitmap = scaled
                        onUpdateIcon(scaled)
                        Toast.makeText(context, "Zaktualizowano awatar serwera (64x64)!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Błąd wczytywania zdjęcia: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                            text = "Minecraft ${serverState.selectedEngine.displayName} Mobile",
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
        // 2. Karta Wyboru Silnika i Wersji Serwera
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val isServerStopped = serverState.status == ServerStatus.STOPPED || serverState.status == ServerStatus.ERROR

                // Nagłówek sekcji
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Silnik i Wersja Serwera",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    if (!isServerStopped) {
                        Text(
                            text = "Serwer aktywny",
                            color = WarningYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zakładki wyboru silnika (Folia / Purpur / Fabric)
                Text(
                    text = "Wybierz silnik serwera:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServerEngine.entries.forEach { engine ->
                        val isSelected = engine == serverState.selectedEngine
                        val engineIcon = when (engine) {
                            ServerEngine.FOLIA -> Icons.Default.Memory
                            ServerEngine.PURPUR -> Icons.Default.Speed
                            ServerEngine.FABRIC -> Icons.Default.Extension
                        }
                        val badgeText = when (engine) {
                            ServerEngine.FOLIA -> "Wielowątkowy"
                            ServerEngine.PURPUR -> "Paper Fork"
                            ServerEngine.FABRIC -> "Modyfikacje"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MintAccent.copy(alpha = 0.16f)
                                    else ObsidianDark
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MintAccent else CardBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = isServerStopped && !serverState.isDownloading) {
                                    onEngineChange(engine)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = engineIcon,
                                    contentDescription = engine.displayName,
                                    tint = if (isSelected) MintAccent else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = engine.displayName,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = badgeText,
                                    color = if (isSelected) EmeraldGreen else TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pigułka informacyjna o wybranym silniku
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = when (serverState.selectedEngine) {
                            ServerEngine.FOLIA -> "⚡ Folia: Dzieli świat na niezależne wątki regionalne. Maksymalna wydajność wielordzeniowa (Snapdragon 888)."
                            ServerEngine.PURPUR -> "🚀 Purpur: Zoptymalizowany fork PaperMC z pełnym wsparciem klasycznych pluginów Spigot/Paper."
                            ServerEngine.FABRIC -> "🧩 Fabric: Ultralekki modułowy silnik z obsługą modów. Pliki .jar modów umieszczaj w folderze mods/."
                        },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rząd wyboru wersji dla wybranego silnika
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wersja Minecraft:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Dla silnika ${serverState.selectedEngine.displayName}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = isServerStopped && !serverState.isDownloading) {
                                    versionDropdownExpanded = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${serverState.selectedVersion}",
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
                            serverState.availableVersions.forEach { version ->
                                val isSelected = version == serverState.selectedVersion
                                val isThisVersionInstalled = isSelected && serverState.isInstalled
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${serverState.selectedEngine.displayName} $version",
                                                color = if (isSelected) EmeraldGreen else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                            if (isThisVersionInstalled) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Informacja o stanie zainstalowania wybranego silnika/wersji
                if (serverState.isInstalled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EmeraldGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Silnik ${serverState.selectedEngine.displayName} ${serverState.selectedVersion} jest pobrany i gotowy do startu.",
                            color = EmeraldGreen,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (serverState.isDownloading) MintAccent.copy(alpha = 0.12f)
                                else WarningYellow.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (serverState.isDownloading) {
                                        "Pobieranie ${serverState.selectedEngine.displayName} ${serverState.selectedVersion}: ${serverState.downloadProgressPercent}%"
                                    } else {
                                        "Wersja niepobrana: ${serverState.selectedEngine.displayName} ${serverState.selectedVersion}"
                                    },
                                    color = if (serverState.isDownloading) MintAccent else WarningYellow,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (serverState.isDownloading) {
                                        serverState.downloadStatusMessage ?: "Pobieranie pliku serwera..."
                                    } else {
                                        "Pobierze się automatycznie przy starcie serwera."
                                    },
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            if (isServerStopped) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (!serverState.isDownloading) {
                                            onDownloadVersion(serverState.selectedVersion)
                                        }
                                    },
                                    enabled = !serverState.isDownloading,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MintAccent,
                                        disabledContentColor = MintAccent.copy(alpha = 0.7f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (serverState.isDownloading) MintAccent.copy(alpha = 0.5f) else MintAccent
                                    )
                                ) {
                                    if (serverState.isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MintAccent
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${serverState.downloadProgressPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
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

                        if (serverState.isDownloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (serverState.downloadProgressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MintAccent,
                                trackColor = ObsidianDark
                            )
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
                val engineThreadDesc = when (serverState.selectedEngine) {
                    ServerEngine.FOLIA -> "Silnik Folia przydziela dedykowane wątki do równoległego tickingowania każdego regionu świata na wybranych rdzeniach."
                    ServerEngine.PURPUR -> "Silnik Purpur optymalizuje przetwarzanie chunków, encji i asynchroniczny zapis na wybranych rdzeniach."
                    ServerEngine.FABRIC -> "Silnik Fabric wykorzystuje zoptymalizowany podział zadań na dedykowanych rdzeniach procesora."
                }
                Text(
                    text = engineThreadDesc,
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

        // 5b. Karta Wyglądu Serwera (Awatar i Opis MOTD)
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
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WYGLĄD NA LIŚCIE MINECRAFT",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = if (isEditingAppearance) "Zwiń edytor" else "Edytuj wygląd",
                        color = MintAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isEditingAppearance = !isEditingAppearance }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Realistyczny podgląd wpisu na liście serwerów Minecraft PC
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C0E12))
                        .border(1.dp, Color(0xFF222933), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (serverIconBitmap != null) {
                        Image(
                            bitmap = serverIconBitmap!!.asImageBitmap(),
                            contentDescription = "Awatar serwera",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MintAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "pearium.com",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${serverState.playersOnline}/${serverState.maxPlayers}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ping",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = parseMinecraftMotd(motdText),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp,
                            maxLines = 2
                        )
                    }
                }

                if (isEditingAppearance) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Sekcja 1: Zmiana Awatara (64x64 PNG)
                    Text(
                        text = "AWATAR SERWERA (64x64 PNG)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkEmerald),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MintAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Własne zdjęcie", color = MintAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSetPresetIcon("pearium")
                                serverIconBitmap = configManager?.getServerIconBitmap()
                                Toast.makeText(context, "Ustawiono logo Pearium!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("🍐 Pearium", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onSetPresetIcon("grass")
                                serverIconBitmap = configManager?.getServerIconBitmap()
                                Toast.makeText(context, "Ustawiono blok trawy!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("🌿 Trawa", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sekcja 2: Edycja Opisu (MOTD) z paletą kolorów
                    Text(
                        text = "OPIS SERWERA (MOTD Z KOLORAMI)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = motdText,
                        onValueChange = { motdText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Treść MOTD (użyj §a, §b itp.)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pasek narzędzi z kolorami Minecraft
                    Text("Kliknij kolor, aby wstawić kod:", color = TextSecondary, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val colorChips = listOf(
                            Triple("Zielony", "§a", Color(0xFF55FF55)),
                            Triple("Błękit", "§b", Color(0xFF55FFFF)),
                            Triple("Złoty", "§6", Color(0xFFFFAA00)),
                            Triple("Czerwień", "§c", Color(0xFFFF5555)),
                            Triple("Żółty", "§e", Color(0xFFFFFF55)),
                            Triple("Biały", "§f", Color(0xFFFFFFFF)),
                            Triple("Pogrub", "§l", TextPrimary),
                            Triple("Reset", "§r", TextSecondary)
                        )

                        colorChips.forEach { (label, code, chipColor) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(chipColor.copy(alpha = 0.15f))
                                    .border(1.dp, chipColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable { motdText += code }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = code, color = chipColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onUpdateMotd(motdText)
                            Toast.makeText(context, "Zapisano opis MOTD w server.properties!", Toast.LENGTH_SHORT).show()
                            isEditingAppearance = false
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = ObsidianDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zapisz Wygląd Serwera", color = ObsidianDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
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
                            text = "RAM Serwera (${serverState.selectedEngine.displayName})",
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
                            text = "CPU Serwera (${serverState.selectedEngine.displayName})",
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

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RDZENIE PROCESORA (Snapdragon 888 Tri-Cluster)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "8 rdzeni",
                        color = MintAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Lista telemetrii dla 8 rdzeni z taktowaniem i rolą
                val telemetryList = serverState.coreTelemetryList.ifEmpty {
                    (0 until 8).map { i ->
                        val (name, cluster, role, maxGhz) = when (i) {
                            in 0..3 -> listOf("Cortex-A55", "Silver", "Energooszczędny", 1.80f)
                            in 4..6 -> listOf("Cortex-A78", "Gold", "Wydajny", 2.42f)
                            else -> listOf("Cortex-X1", "Prime", "Superwydajny", 2.84f)
                        }
                        com.pearium.minecraftmobile.core.CoreTelemetry(
                            coreIndex = i,
                            coreName = name as String,
                            clusterType = cluster as String,
                            role = role as String,
                            maxFreqGhz = maxGhz as Float,
                            curFreqMhz = when (i) { in 0..3 -> 1804; in 4..6 -> 2419; else -> 2841 },
                            usagePercent = serverState.coreUsageList.getOrElse(i) { 0f },
                            isAllocated = i >= (8 - serverState.allocatedCores)
                        )
                    }
                }

                // 2 rzędy po 4 rdzenie
                val row1 = telemetryList.take(4)
                val row2 = telemetryList.drop(4)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Rząd 1: Energooszczędne Cortex-A55 (R1-R4)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row1.forEach { core ->
                            val isAllocated = core.isAllocated
                            val usage = core.usagePercent
                            val barColor = if (!isAllocated) TextSecondary.copy(alpha = 0.3f)
                            else if (usage > 80f) DangerRed
                            else if (usage > 40f) WarningYellow
                            else EmeraldGreen

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllocated) ObsidianDark else CardBackground)
                                    .border(1.dp, if (isAllocated) barColor.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("R${core.coreIndex + 1}", color = if (isAllocated) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Silver", color = TextSecondary, fontSize = 8.sp)
                                }
                                Text("A55", color = TextSecondary, fontSize = 9.sp)
                                Text("${String.format("%.2f", core.curFreqMhz / 1000f)}G", color = MintAccent, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (usage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = barColor,
                                    trackColor = CardBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${usage.toInt()}%", color = if (isAllocated) barColor else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Rząd 2: Wydajne Cortex-A78 (R5-R7) oraz Ekstremalny Cortex-X1 (R8)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row2.forEach { core ->
                            val isAllocated = core.isAllocated
                            val usage = core.usagePercent
                            val isPrime = core.coreIndex == 7
                            val barColor = if (!isAllocated) TextSecondary.copy(alpha = 0.3f)
                            else if (usage > 80f) DangerRed
                            else if (usage > 40f) WarningYellow
                            else if (isPrime) MintAccent
                            else EmeraldGreen

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllocated) ObsidianDark else CardBackground)
                                    .border(1.dp, if (isPrime && isAllocated) MintAccent.copy(alpha = 0.6f) else if (isAllocated) barColor.copy(alpha = 0.4f) else CardBorder, RoundedCornerShape(8.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("R${core.coreIndex + 1}", color = if (isAllocated) TextPrimary else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(if (isPrime) "PRIME" else "Gold", color = if (isPrime) MintAccent else WarningYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(if (isPrime) "X1" else "A78", color = if (isPrime) MintAccent else TextSecondary, fontSize = 9.sp, fontWeight = if (isPrime) FontWeight.Bold else FontWeight.Normal)
                                Text("${String.format("%.2f", core.curFreqMhz / 1000f)}G", color = if (isPrime) MintAccent else TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (usage / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = barColor,
                                    trackColor = CardBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${usage.toInt()}%", color = if (isAllocated) barColor else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• R1-R4: Energooszczędne Cortex-A55 (1.80 GHz) • R5-R7: Wydajne Cortex-A78 (2.42 GHz) • R8: Superwydajny Cortex-X1 (2.84 GHz)",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
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
                        label = "Silnik & Wersja",
                        value = "${serverState.selectedEngine.displayName} v${serverState.selectedVersion}"
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
