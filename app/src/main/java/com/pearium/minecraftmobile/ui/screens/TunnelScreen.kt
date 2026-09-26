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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pearium.minecraftmobile.tunnel.FRPClientManager
import com.pearium.minecraftmobile.tunnel.TunnelConfig
import com.pearium.minecraftmobile.tunnel.TunnelStatus
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
fun TunnelScreen(
    tunnelManager: FRPClientManager
) {
    val context = LocalContext.current
    val status by tunnelManager.status.collectAsState()
    val config by tunnelManager.config.collectAsState()
    val scrollState = rememberScrollState()

    var host by remember(config.serverHost) { mutableStateOf(config.serverHost) }
    var port by remember(config.serverPort) { mutableStateOf(config.serverPort.toString()) }
    var token by remember(config.token) { mutableStateOf(config.token) }
    var autoStart by remember(config.isAutoStart) { mutableStateOf(config.isAutoStart) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tunel Sieciowy i Domena",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = "Ominięcie braku publicznego IP przez maszynę GCP Spot",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // Karta Statusu Połączenia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (status) {
                        TunnelStatus.CONNECTED -> Icons.Default.CloudDone
                        TunnelStatus.CONNECTING -> Icons.Default.CloudQueue
                        TunnelStatus.DISCONNECTED -> Icons.Default.CloudOff
                        TunnelStatus.ERROR -> Icons.Default.Cloud
                    }
                    val iconTint = when (status) {
                        TunnelStatus.CONNECTED -> EmeraldGreen
                        TunnelStatus.CONNECTING -> WarningYellow
                        TunnelStatus.DISCONNECTED -> TextSecondary
                        TunnelStatus.ERROR -> DangerRed
                    }

                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (status) {
                                TunnelStatus.CONNECTED -> "Połączono z pearium.com"
                                TunnelStatus.CONNECTING -> "Nawiązywanie tunelu..."
                                TunnelStatus.DISCONNECTED -> "Tunel rozłączony"
                                TunnelStatus.ERROR -> "Błąd połączenia z GCP"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Port Minecraft: 25565 -> GCP:25565",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        if (status == TunnelStatus.CONNECTED) {
                            tunnelManager.stopTunnel()
                        } else {
                            tunnelManager.startTunnel()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == TunnelStatus.CONNECTED) DangerRed else DarkEmerald
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (status == TunnelStatus.CONNECTED) "Rozłącz" else "Połącz",
                        color = if (status == TunnelStatus.CONNECTED) ObsidianDark else MintAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Karta Konfiguracji Przekaźnika GCP
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PARAMETRY PRZEKAŹNIKA GCP (FRANKFURT)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Adres domeny lub IP serwera GCP") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port sterujący (FRP)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Klucz autoryzacyjny") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Automatyczny tunel", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Włączaj tunel razem ze startem serwera", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGreen,
                            checkedTrackColor = DarkEmerald
                        )
                    )
                }

                Button(
                    onClick = {
                        val parsedPort = port.toIntOrNull() ?: 7000
                        val updated = config.copy(
                            serverHost = host,
                            serverPort = parsedPort,
                            token = token,
                            isAutoStart = autoStart
                        )
                        tunnelManager.updateConfig(updated)
                        Toast.makeText(context, "Zapisano ustawienia tunelu!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = ObsidianDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zapisz Ustawienia Tunelu", color = ObsidianDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Karta informacyjna jak działa routing pearium.com
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Jak działa domena pearium.com?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Przeglądarka internetowa (Port 80 i 443):\n" +
                            "Otwiera stronę aplikacji na serwerze GCP w Niemczech.\n\n" +
                            "• Klient gry Minecraft (Port 25565):\n" +
                            "Łącząc się z pearium.com, serwer przekaźnikowy kieruje ruch bezpośrednio do Twojego Xiaomi 11T Pro przez aktywny tunel, bez potrzeby publicznego IP u Twojego operatora.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
