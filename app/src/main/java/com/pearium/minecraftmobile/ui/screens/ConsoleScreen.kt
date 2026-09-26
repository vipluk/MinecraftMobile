package com.pearium.minecraftmobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pearium.minecraftmobile.ui.theme.CardBackground
import com.pearium.minecraftmobile.ui.theme.CardBorder
import com.pearium.minecraftmobile.ui.theme.ConsoleBackground
import com.pearium.minecraftmobile.ui.theme.ConsoleText
import com.pearium.minecraftmobile.ui.theme.EmeraldGreen
import com.pearium.minecraftmobile.ui.theme.ObsidianDark
import com.pearium.minecraftmobile.ui.theme.TextPrimary
import com.pearium.minecraftmobile.ui.theme.TextSecondary
import com.pearium.minecraftmobile.ui.theme.WarningYellow

@Composable
fun ConsoleScreen(
    logs: List<String>,
    onSendCommand: (String) -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val chipScrollState = rememberScrollState()

    // Automatyczne przewijanie na dół przy nowych logach
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val quickCommands = listOf(
        "tps",
        "list",
        "op ",
        "whitelist on",
        "time set day",
        "weather clear",
        "difficulty peaceful",
        "gamemode creative",
        "save-all"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        // Nagłówek konsoli
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Konsola Serwera",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${logs.size} wpisów w buforze",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Okno terminala z logami
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ConsoleBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak logów. Włącz serwer, aby zobaczyć konsolę.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    items(logs) { logLine ->
                        val lineColor = when {
                            logLine.contains("WARN", ignoreCase = true) -> WarningYellow
                            logLine.contains("ERROR", ignoreCase = true) || logLine.contains("Exception") -> Color(0xFFFF5252)
                            logLine.contains("Done (") -> EmeraldGreen
                            logLine.startsWith(">") -> Color(0xFFE0E0E0)
                            else -> ConsoleText
                        }

                        Text(
                            text = logLine,
                            color = lineColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pasek szybkich komend (Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (cmd.endsWith(" ")) {
                                commandText = cmd
                            } else {
                                onSendCommand(cmd)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "/$cmd",
                        color = EmeraldGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pole wprowadzania komendy
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Wpisz komendę (np. op nick, tps, gamemode)...", color = TextSecondary, fontSize = 13.sp)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandText.isNotBlank()) {
                            onSendCommand(commandText)
                            commandText = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onSendCommand(commandText)
                        commandText = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Wyślij",
                    tint = ObsidianDark
                )
            }
        }
    }
}
