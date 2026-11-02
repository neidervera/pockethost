package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.NetworkType
import com.example.ui.components.MetricsCards
import com.example.ui.components.QRCodeView
import com.example.ui.components.StatusIndicator
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val serverState by viewModel.serverState.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val diskUsage by viewModel.diskUsage.collectAsState()
    val qrModalVisible by viewModel.qrModalVisible.collectAsState()

    val isRunning = serverState.isRunning

    val buttonBgColor by animateColorAsState(
        targetValue = if (isRunning) CrimsonError else EmeraldActive,
        label = "btnColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRunning) 1.02f else 1.0f,
        animationSpec = spring(),
        label = "btnScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status Indicator Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(isRunning = isRunning)

            // Wi-Fi / Hotspot Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceContainerDark, RoundedCornerShape(999.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val wifiIcon = when (networkInfo.networkType) {
                    NetworkType.WIFI -> Icons.Default.Wifi
                    NetworkType.HOTSPOT -> Icons.Default.WifiTethering
                    else -> Icons.Default.WifiOff
                }
                val wifiColor = if (networkInfo.networkType == NetworkType.NONE) CrimsonError else CyanNeon

                Icon(
                    imageVector = wifiIcon,
                    contentDescription = null,
                    tint = wifiColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = networkInfo.networkLabel,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Warning if in loopback or disconnected
        if (networkInfo.networkType == NetworkType.LOOPBACK || networkInfo.networkType == NetworkType.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmberWarning.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sin Wi-Fi local activo. Los otros dispositivos deben conectarse al mismo Wi-Fi o al Punto de Acceso (Hotspot) de este celular.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. Main Hero Server Card with URL & IP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceDark,
                            if (isRunning) Color(0xFF0F2228) else Color(0xFF141924)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isRunning) CyanNeon.copy(alpha = 0.4f) else BorderDark,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
                .testTag("server_url_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DIRECCIÓN DE HOSTING LOCAL",
                    color = if (isRunning) CyanNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val displayUrl = if (isRunning) serverState.serverUrl else "http://${networkInfo.ipAddress}:${serverState.port}"

                Text(
                    text = displayUrl,
                    color = if (isRunning) Color.White else TextSecondary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("server_url_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Row (Copy, Share, Open, QR)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copiar",
                        onClick = { viewModel.copyUrlToClipboard() },
                        testTag = "action_copy_url"
                    )
                    ActionButton(
                        icon = Icons.Default.Share,
                        label = "Compartir",
                        onClick = { viewModel.shareServerUrl() },
                        testTag = "action_share_url"
                    )
                    ActionButton(
                        icon = Icons.Default.QrCode2,
                        label = "Ver QR",
                        onClick = { viewModel.showQrModal(true) },
                        testTag = "action_show_qr"
                    )
                    ActionButton(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        label = "Abrir",
                        onClick = { viewModel.openInBrowser() },
                        enabled = isRunning,
                        testTag = "action_open_browser"
                    )
                }
            }
        }

        // 3. Huge Start / Stop Server Master Button
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .scale(buttonScale)
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) CrimsonError.copy(alpha = 0.2f) else EmeraldActive.copy(alpha = 0.15f)
                )
                .border(
                    width = 2.dp,
                    color = buttonBgColor,
                    shape = CircleShape
                )
                .clickable { viewModel.toggleServer() }
                .testTag("server_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isRunning) "Detener Servidor" else "Iniciar Servidor",
                    tint = buttonBgColor,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRunning) "DETENER" else "INICIAR",
                    color = buttonBgColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 4. Instructions Hint
        Text(
            text = if (isRunning)
                "El servidor está escuchando en tu red Wi-Fi. Abre el navegador en cualquier PC, tablet o celular en la misma red y visita la URL."
            else
                "Toca INICIAR para activar el servidor web local y comenzar a alojar tus páginas.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 5. Telemetry & Metrics Cards
        Text(
            text = "Métricas en Tiempo Real",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        MetricsCards(
            state = serverState,
            diskUsageBytes = diskUsage
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // QR Code Modal Dialog
    if (qrModalVisible) {
        BasicAlertDialog(
            onDismissRequest = { viewModel.showQrModal(false) },
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Escanear para Conectar",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apunta con la cámara de otro dispositivo conectado a la misma red Wi-Fi:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val qrUrl = if (isRunning) serverState.serverUrl else "http://${networkInfo.ipAddress}:${serverState.port}"
                    QRCodeView(
                        content = qrUrl,
                        sizeDp = 220.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = qrUrl,
                        color = CyanNeon,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.copyUrlToClipboard() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copiar IP", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.showQrModal(false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = BackgroundDark)
                        ) {
                            Text("Cerrar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String
) {
    val contentColor = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceContainerDark, CircleShape)
                .border(1.dp, BorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) CyanNeon else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
