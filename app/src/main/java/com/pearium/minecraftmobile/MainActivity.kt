package com.pearium.minecraftmobile

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pearium.minecraftmobile.core.ConfigManager
import com.pearium.minecraftmobile.core.FoliaDownloader
import com.pearium.minecraftmobile.core.JavaRuntimeManager
import com.pearium.minecraftmobile.core.ServerProcessManager
import com.pearium.minecraftmobile.core.ServerState
import com.pearium.minecraftmobile.modrinth.ModrinthApiService
import com.pearium.minecraftmobile.modrinth.PluginManager
import com.pearium.minecraftmobile.service.MinecraftServerService
import com.pearium.minecraftmobile.tunnel.FRPClientManager
import com.pearium.minecraftmobile.ui.screens.ConsoleScreen
import com.pearium.minecraftmobile.ui.screens.DashboardScreen
import com.pearium.minecraftmobile.ui.screens.FileManagerScreen
import com.pearium.minecraftmobile.ui.screens.PluginsScreen
import com.pearium.minecraftmobile.ui.screens.TunnelScreen
import com.pearium.minecraftmobile.ui.theme.CardBackground
import com.pearium.minecraftmobile.ui.theme.EmeraldGreen
import com.pearium.minecraftmobile.ui.theme.MinecraftMobileTheme
import com.pearium.minecraftmobile.ui.theme.ObsidianDark
import com.pearium.minecraftmobile.ui.theme.TextPrimary
import com.pearium.minecraftmobile.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : ComponentActivity() {

    private var serverService: MinecraftServerService? = null
    private var isBound by mutableStateOf(false)

    // Stan serwera do podglądu w Compose
    private var serverState by mutableStateOf(ServerState())
    private val consoleLogs = mutableStateListOf<String>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as MinecraftServerService.LocalBinder
            val service = localBinder.getService()
            serverService = service
            isBound = true

            // Subskrypcja stanu serwera
            service.processManager.serverState.onEach { state ->
                serverState = state
            }.launchIn(CoroutineScope(Dispatchers.Main))

            // Subskrypcja logów
            service.processManager.logFlow.onEach { log ->
                consoleLogs.add(log)
            }.launchIn(CoroutineScope(Dispatchers.Main))

            consoleLogs.clear()
            consoleLogs.addAll(service.processManager.getRecentLogs())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverService = null
            isBound = false
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBatteryOptimizationIgnore()
        requestNotificationPermission()

        // Uruchomienie i bindowanie serwisu
        val serviceIntent = Intent(this, MinecraftServerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MinecraftMobileTheme {
                MainContent(
                    serverState = serverState,
                    logs = consoleLogs,
                    onStartServer = { ram ->
                        val intent = Intent(this, MinecraftServerService::class.java).apply {
                            action = MinecraftServerService.ACTION_START
                            putExtra(MinecraftServerService.EXTRA_RAM, ram)
                        }
                        startService(intent)
                    },
                    onStopServer = {
                        val intent = Intent(this, MinecraftServerService::class.java).apply {
                            action = MinecraftServerService.ACTION_STOP
                        }
                        startService(intent)
                    },
                    onRamChange = { ram ->
                        serverService?.processManager?.setRamAllocation(ram)
                    },
                    onSendCommand = { cmd ->
                        serverService?.processManager?.sendCommand(cmd)
                    },
                    configManager = serverService?.configManager ?: ConfigManager(this),
                    pluginManager = serverService?.pluginManager ?: PluginManager(ConfigManager(this)),
                    tunnelManager = serverService?.tunnelManager ?: FRPClientManager(this)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationIgnore() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

@Composable
fun MainContent(
    serverState: ServerState,
    logs: List<String>,
    onStartServer: (Float) -> Unit,
    onStopServer: () -> Unit,
    onRamChange: (Float) -> Unit,
    onSendCommand: (String) -> Unit,
    configManager: ConfigManager,
    pluginManager: PluginManager,
    tunnelManager: FRPClientManager
) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val modrinthService = remember { ModrinthApiService() }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                contentColor = TextPrimary
            ) {
                val items = listOf(
                    Triple("Panel", Icons.Default.Dashboard, 0),
                    Triple("Konsola", Icons.Default.Terminal, 1),
                    Triple("Pluginy", Icons.Default.Extension, 2),
                    Triple("Pliki", Icons.Default.Folder, 3),
                    Triple("Tunel", Icons.Default.Cloud, 4)
                )

                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedScreenIndex == index,
                        onClick = { selectedScreenIndex = index },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ObsidianDark,
                            selectedTextColor = EmeraldGreen,
                            indicatorColor = EmeraldGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDark)
                .padding(paddingValues)
        ) {
            when (selectedScreenIndex) {
                0 -> DashboardScreen(
                    serverState = serverState,
                    onStartServer = onStartServer,
                    onStopServer = onStopServer,
                    onRamChange = onRamChange,
                    onNavigateToConsole = { selectedScreenIndex = 1 }
                )
                1 -> ConsoleScreen(
                    logs = logs,
                    onSendCommand = onSendCommand
                )
                2 -> PluginsScreen(
                    pluginManager = pluginManager,
                    modrinthService = modrinthService
                )
                3 -> FileManagerScreen(
                    configManager = configManager
                )
                4 -> TunnelScreen(
                    tunnelManager = tunnelManager
                )
            }
        }
    }
}
