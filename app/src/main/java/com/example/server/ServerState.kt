package com.example.server

import java.io.File

data class ServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "127.0.0.1",
    val port: Int = 8080,
    val rootDirectory: File? = null,
    val activeConnections: Int = 0,
    val totalRequests: Long = 0L,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val errorCount: Long = 0L,
    val lastError: String? = null
) {
    val serverUrl: String
        get() = if (port == 80) "http://$ipAddress" else "http://$ipAddress:$port"

    val formattedUptime: String
        get() {
            val hours = uptimeSeconds / 3600
            val minutes = (uptimeSeconds % 3600) / 60
            val seconds = uptimeSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedBytesSent: String
        get() = formatBytes(bytesSent)

    val formattedBytesReceived: String
        get() = formatBytes(bytesReceived)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}
