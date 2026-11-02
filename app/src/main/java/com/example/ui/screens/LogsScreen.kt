package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.server.ServerLog
import com.example.server.ServerState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ServerViewModel

enum class LogFilter { ALL, SUCCESS, ERRORS }

@Composable
fun LogsScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.logs.collectAsState()
    val config by viewModel.config.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LogFilter.ALL) }

    val filteredLogs = remember(logs, searchQuery, selectedFilter) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                    log.path.contains(searchQuery, ignoreCase = true) ||
                    log.clientIp.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                LogFilter.ALL -> true
                LogFilter.SUCCESS -> log.isSuccess
                LogFilter.ERRORS -> log.isClientError || log.isServerError
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // Search & Clear Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrar por ruta o IP...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_search_logs")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { viewModel.clearAllLogs() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Limpiar registros",
                    tint = CrimsonError
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filters row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == LogFilter.ALL,
                onClick = { selectedFilter = LogFilter.ALL },
                label = { Text("Todos (${logs.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                    selectedLabelColor = CyanNeon
                )
            )
            FilterChip(
                selected = selectedFilter == LogFilter.SUCCESS,
                onClick = { selectedFilter = LogFilter.SUCCESS },
                label = { Text("2xx Éxito", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldActive.copy(alpha = 0.2f),
                    selectedLabelColor = EmeraldActive
                )
            )
            FilterChip(
                selected = selectedFilter == LogFilter.ERRORS,
                onClick = { selectedFilter = LogFilter.ERRORS },
                label = { Text("4xx/5xx Errores", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrimsonError.copy(alpha = 0.2f),
                    selectedLabelColor = CrimsonError
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log Items List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (logs.isEmpty()) "Sin registros de peticiones aún" else "No hay resultados para este filtro",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (logs.isEmpty()) {
                        Text(
                            text = "Las solicitudes de los navegadores aparecerán aquí en vivo",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogCardItem(
                        log = log,
                        isBlocked = config.blockedIps.contains(log.clientIp),
                        onBlockIp = { viewModel.blockIp(log.clientIp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LogCardItem(
    log: ServerLog,
    isBlocked: Boolean,
    onBlockIp: () -> Unit
) {
    val statusColor = when {
        log.isSuccess -> EmeraldActive
        log.isRedirect -> PurpleAccent
        log.statusCode == 404 -> AmberWarning
        else -> CrimsonError
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("log_item_${log.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status code and Method badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${log.statusCode}",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = log.method,
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Time and duration
                Text(
                    text = "${log.formattedTime} • ${log.durationMs}ms",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Path
            Text(
                text = log.path,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer row: Client IP + Data bytes + Block IP action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "IP: ${log.clientIp}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " • ${ServerState.formatBytes(log.bytesSent)}",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                if (!isBlocked && log.clientIp != "127.0.0.1" && log.clientIp != "localhost") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CrimsonError.copy(alpha = 0.1f))
                            .clickable { onBlockIp() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = CrimsonError,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bloquear",
                            color = CrimsonError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isBlocked) {
                    Text(
                        text = "BLOQUEADA",
                        color = CrimsonError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
