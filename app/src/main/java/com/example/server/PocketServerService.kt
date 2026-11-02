package com.example.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.file.DefaultProjects
import com.example.file.FileManager
import com.example.network.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PocketServerService : Service() {

    private val binder = LocalBinder()
    private var server: PocketHttpServer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var uptimeJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): PocketServerService = this@PocketServerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    fun startServer(): Result<Unit> {
        if (server?.isRunning == true) return Result.success(Unit)

        val fileManager = FileManager(this)
        val configManager = ServerConfigManager(this)
        val config = configManager.config.value

        // Ensure default templates are populated
        DefaultProjects.populateDefaultProjects(fileManager)
        val targetDir = fileManager.getProjectDir(config.selectedProjectFolder)

        val networkInfo = NetworkUtils.getLocalIpAddress(this)

        val newServer = PocketHttpServer(
            port = config.port,
            rootDirectory = targetDir,
            config = config,
            onStateUpdated = { state ->
                val enrichedState = state.copy(ipAddress = networkInfo.ipAddress)
                _serverState.value = enrichedState
                updateNotification(enrichedState)
            },
            onLogCreated = { log ->
                addLog(log)
            }
        )

        val result = newServer.start()
        if (result.isSuccess) {
            server = newServer
            acquireLocks()

            val state = ServerState(
                isRunning = true,
                ipAddress = networkInfo.ipAddress,
                port = config.port,
                rootDirectory = targetDir
            )
            _serverState.value = state

            val notification = buildNotification(state)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Periodic uptime updater
            uptimeJob?.cancel()
            uptimeJob = serviceScope.launch {
                while (isActive && server?.isRunning == true) {
                    delay(1000)
                    val current = _serverState.value
                    if (current.isRunning) {
                        _serverState.value = current.copy(uptimeSeconds = current.uptimeSeconds + 1)
                    }
                }
            }
        }
        return result
    }

    fun stopServer() {
        uptimeJob?.cancel()
        server?.stop()
        server = null
        releaseLocks()

        _serverState.value = _serverState.value.copy(
            isRunning = false,
            activeConnections = 0
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun updateConfig(newConfig: ServerConfig) {
        val currentServer = server
        if (currentServer != null && currentServer.isRunning) {
            currentServer.config = newConfig
            val fileManager = FileManager(this)
            val newDir = fileManager.getProjectDir(newConfig.selectedProjectFolder)
            currentServer.rootDirectory = newDir
            currentServer.port = newConfig.port
        }
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PocketHost::ServerWakeLock")?.apply {
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety timeout
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "PocketHost::WifiLock")?.apply {
                acquire()
            }
        } catch (e: Exception) {
            // Non-fatal if locks are restricted
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            // Ignore release errors
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PocketHost Servidor Web",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones del estado del servidor local PocketHost"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: ServerState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, PocketServerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketHost Servidor Activo")
            .setContentText("${state.serverUrl} • ${state.totalRequests} peticiones")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(state: ServerState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    override fun onDestroy() {
        stopServer()
        instance = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "pockethost_server_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.pockethost.START"
        const val ACTION_STOP = "com.example.pockethost.STOP"

        var instance: PocketServerService? = null
            private set

        private val _serverState = MutableStateFlow(ServerState())
        val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

        private val _logs = MutableStateFlow<List<ServerLog>>(emptyList())
        val logs: StateFlow<List<ServerLog>> = _logs.asStateFlow()

        fun addLog(log: ServerLog) {
            val current = _logs.value
            // Keep recent 200 logs
            _logs.value = listOf(log) + current.take(199)
        }

        fun clearLogs() {
            _logs.value = emptyList()
        }
    }
}
