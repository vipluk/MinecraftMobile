package com.pearium.minecraftmobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.pearium.minecraftmobile.MainActivity
import com.pearium.minecraftmobile.R
import com.pearium.minecraftmobile.core.ConfigManager
import com.pearium.minecraftmobile.core.FoliaDownloader
import com.pearium.minecraftmobile.core.JavaRuntimeManager
import com.pearium.minecraftmobile.core.ServerProcessManager
import com.pearium.minecraftmobile.core.ServerStatus
import com.pearium.minecraftmobile.modrinth.PluginManager
import com.pearium.minecraftmobile.tunnel.FRPClientManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MinecraftServerService : Service() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    lateinit var configManager: ConfigManager private set
    lateinit var javaRuntimeManager: JavaRuntimeManager private set
    lateinit var foliaDownloader: FoliaDownloader private set
    lateinit var processManager: ServerProcessManager private set
    lateinit var pluginManager: PluginManager private set
    lateinit var tunnelManager: FRPClientManager private set

    inner class LocalBinder : Binder() {
        fun getService(): MinecraftServerService = this@MinecraftServerService
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        configManager = ConfigManager(this)
        javaRuntimeManager = JavaRuntimeManager(this)
        foliaDownloader = FoliaDownloader(this)
        processManager = ServerProcessManager(this, configManager, javaRuntimeManager, foliaDownloader)
        pluginManager = PluginManager(configManager)
        tunnelManager = FRPClientManager(this)

        createNotificationChannel()
        acquireLocks()

        // Obserwacja stanu serwera i aktualizacja powiadomienia
        processManager.serverState.onEach { state ->
            updateNotification(state.status, state.playersOnline, state.allocatedRamGb)
            if (state.status == ServerStatus.RUNNING && tunnelManager.config.value.isAutoStart) {
                tunnelManager.startTunnel()
            }
        }.launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val ram = intent.getFloatExtra(EXTRA_RAM, 4.0f)
                startForeground(NOTIFICATION_ID, buildNotification("Uruchamianie...", 0, ram))
                processManager.startServer(ram)
            }
            ACTION_STOP -> {
                processManager.stopServer()
                tunnelManager.stopTunnel()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        instance = null
    }

    private fun acquireLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MinecraftMobile::ServerWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 godziny
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "MinecraftMobile::ServerWifiLock"
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseLocks() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(status: ServerStatus, players: Int, ramGb: Float) {
        val title = when (status) {
            ServerStatus.RUNNING -> "Serwer Folia Aktywny (${players} graczy)"
            ServerStatus.STARTING -> "Startowanie serwera Folia..."
            ServerStatus.STOPPING -> "Zatrzymywanie serwera..."
            ServerStatus.STOPPED -> "Serwer zatrzymany"
            ServerStatus.ERROR -> "Błąd działania serwera"
        }
        val notification = buildNotification(title, players, ramGb)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String, players: Int, ramGb: Float): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MinecraftServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MinecraftMobile: $statusText")
            .setContentText("RAM: ${ramGb.toInt()}GB | Domena: pearium.com:25565")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "Zatrzymaj", stopPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "minecraft_mobile_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.pearium.minecraftmobile.START"
        const val ACTION_STOP = "com.pearium.minecraftmobile.STOP"
        const val EXTRA_RAM = "extra_ram"

        var instance: MinecraftServerService? = null
            private set
    }
}
