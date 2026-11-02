package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.server.ServerState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MetricsCards(
    state: ServerState,
    diskUsageBytes: Long,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                icon = Icons.Default.Devices,
                iconColor = CyanNeon,
                title = "Dispositivos",
                value = "${state.activeConnections} activos",
                modifier = Modifier.weight(1f),
                testTag = "metric_active_clients"
            )
            MetricBox(
                icon = Icons.Default.Http,
                iconColor = EmeraldActive,
                title = "Peticiones",
                value = "${state.totalRequests} total",
                modifier = Modifier.weight(1f),
                testTag = "metric_total_requests"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                icon = Icons.Default.CloudUpload,
                iconColor = CyanNeon,
                title = "Datos Enviados",
                value = state.formattedBytesSent,
                modifier = Modifier.weight(1f),
                testTag = "metric_bytes_sent"
            )
            MetricBox(
                icon = Icons.Default.CloudDownload,
                iconColor = PurpleAccent,
                title = "Datos Recibidos",
                value = state.formattedBytesReceived,
                modifier = Modifier.weight(1f),
                testTag = "metric_bytes_received"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricBox(
                icon = Icons.Default.Timer,
                iconColor = EmeraldActive,
                title = "Tiempo Activo",
                value = if (state.isRunning) state.formattedUptime else "--:--",
                modifier = Modifier.weight(1f),
                testTag = "metric_uptime"
            )
            MetricBox(
                icon = Icons.Default.Storage,
                iconColor = Color(0xFFFFB300),
                title = "Espacio Web",
                value = ServerState.formatBytes(diskUsageBytes),
                modifier = Modifier.weight(1f),
                testTag = "metric_disk_storage"
            )
        }
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .background(SurfaceContainerDark, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF26324A), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
