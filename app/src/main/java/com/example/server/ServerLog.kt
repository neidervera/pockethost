package com.example.server

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ServerLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val clientIp: String,
    val method: String,
    val path: String,
    val statusCode: Int,
    val bytesSent: Long,
    val durationMs: Long,
    val userAgent: String = "",
    val errorDetail: String? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val isSuccess: Boolean
        get() = statusCode in 200..299

    val isRedirect: Boolean
        get() = statusCode in 300..399

    val isClientError: Boolean
        get() = statusCode in 400..499

    val isServerError: Boolean
        get() = statusCode >= 500
}
