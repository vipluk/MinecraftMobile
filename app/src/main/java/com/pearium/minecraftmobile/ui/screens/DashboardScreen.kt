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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    onStartServer: (Float) -> Unit,
    onStopServer: () -> Unit,
    onRamChange: (Float) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nagłówek i status
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
                            text = "Minecraft Folia",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Xiaomi 11T Pro • Snapdragon 888",
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
                    Text(
                        text = errorMsg,
                        color = if (isInfo) MintAccent else DangerRed,
                        fontSize = 13.sp,
                        fontWeight = if (isInfo) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                (if (isInfo) EmeraldGreen else DangerRed).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Karta Adresu Serwera (pearium.com)
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

        // Karta przydziału pamięci RAM (Suwak 1-8 GB + Pole tekstowe)
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pamięć RAM Serwera",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    // Pole wpisywania RAM
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

                Spacer(modifier = Modifier.height(8.dp))

                // Suwak od 1 do 8 GB
                Slider(
                    value = ramSliderValue,
                    onValueChange = { newVal ->
                        ramSliderValue = newVal
                        ramTextValue = newVal.toInt().toString()
                        onRamChange(newVal)
                    },
                    valueRange = 1.0f..8.0f,
                    steps = 6, // 1, 2, 3, 4, 5, 6, 7, 8 GB
                    enabled = serverState.status == ServerStatus.STOPPED,
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

                if (serverState.status != ServerStatus.STOPPED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aby zmienić pamięć RAM, zatrzymaj serwer.",
                        color = WarningYellow,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Przyciski Akcji: Start / Stop
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
                        onStartServer(ramSliderValue)
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

        // Karta Statystyk na Żywo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATYSTYKI CZASU RZECZYWISTEGO",
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
                        label = "Gracze",
                        value = "${serverState.playersOnline} / ${serverState.maxPlayers}"
                    )
                    StatItem(
                        icon = Icons.Default.Memory,
                        label = "Przydział RAM",
                        value = "${serverState.allocatedRamGb.toInt()} GB"
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
