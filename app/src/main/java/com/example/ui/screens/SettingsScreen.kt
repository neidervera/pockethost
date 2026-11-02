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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ServerViewModel

@Composable
fun SettingsScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val serverState by viewModel.serverState.collectAsState()

    var portInput by remember(config.port) { mutableStateOf(config.port.toString()) }
    var authUser by remember(config.authUsername) { mutableStateOf(config.authUsername) }
    var authPass by remember(config.authPassword) { mutableStateOf(config.authPassword) }
    var showPass by remember { mutableStateOf(false) }

    var showAddIpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Server Port
        SettingCard(title = "Configuración del Puerto", icon = Icons.Default.Tune) {
            Text(
                text = "Puerto en el que escuchará el servidor HTTP local (1024 a 65535):",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        portInput = digits
                        val p = digits.toIntOrNull()
                        if (p != null && p in 1..65535) {
                            viewModel.setPort(p)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    label = { Text("Puerto") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_server_port")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Port Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8080, 8000, 3000, 5000, 8888).forEach { preset ->
                    val isSelected = config.port == preset
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CyanNeon.copy(alpha = 0.2f) else SurfaceContainerDark,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else BorderDark,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                portInput = preset.toString()
                                viewModel.setPort(preset)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$preset",
                            color = if (isSelected) CyanNeon else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (serverState.isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nota: Para aplicar un nuevo puerto debes reiniciar el servidor.",
                    color = Color(0xFFFFB300),
                    fontSize = 11.sp
                )
            }
        }

        // Section: Security & Basic Authentication
        SettingCard(title = "Seguridad & Contraseña", icon = Icons.Default.Lock) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Autenticación Básica (HTTP Basic Auth)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Solicita usuario y contraseña a cualquier navegador que intente acceder.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = config.isBasicAuthEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setBasicAuth(checked, authUser, authPass)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = CyanNeon
                    ),
                    modifier = Modifier.testTag("switch_basic_auth")
                )
            }

            if (config.isBasicAuthEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = authUser,
                    onValueChange = {
                        authUser = it
                        viewModel.setBasicAuth(config.isBasicAuthEnabled, it, authPass)
                    },
                    label = { Text("Usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = authPass,
                    onValueChange = {
                        authPass = it
                        viewModel.setBasicAuth(config.isBasicAuthEnabled, authUser, it)
                    },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showPass) "Ocultar" else "Ver",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { showPass = !showPass }
                                .padding(8.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section: Web Features
        SettingCard(title = "Funciones Web", icon = Icons.Default.Security) {
            ToggleRow(
                title = "Listado de Directorios",
                description = "Muestra los archivos en HTML cuando una carpeta no tiene index.html",
                checked = config.isDirectoryListingEnabled,
                onCheckedChange = { viewModel.setDirectoryListing(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "Subida de Archivos desde Navegador",
                description = "Permite a otros dispositivos enviar archivos por POST al servidor",
                checked = config.isFileUploadAllowed,
                onCheckedChange = { viewModel.setFileUploadAllowed(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "Plantillas Dinámicas",
                description = "Reemplaza {{server_time}}, {{client_ip}} y {{device_model}} en archivos HTML",
                checked = config.isDynamicTemplateEnabled,
                onCheckedChange = { viewModel.setDynamicTemplate(it) }
            )
        }

        // Section: Blocked IP addresses
        SettingCard(title = "Lista Negra de Dispositivos (IPs)", icon = Icons.Default.Security) {
            Text(
                text = "Bloquea dispositivos específicos para impedirles el acceso al servidor:",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (config.blockedIps.isEmpty()) {
                Text(
                    text = "No hay direcciones IP bloqueadas actualmente.",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    config.blockedIps.forEach { ip ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceContainerDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ip,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Desbloquear",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.unblockIp(ip) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showAddIpDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bloquear Dirección IP Manualmente", fontSize = 12.sp)
            }
        }

        // Section: Maintenance
        SettingCard(title = "Mantenimiento y Demos", icon = Icons.Default.Refresh) {
            Button(
                onClick = { viewModel.resetDemoProject() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceContainerDark,
                    contentColor = CyanNeon
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restaurar Páginas Web Demo (Arcade, Dashboard, Galería)", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Dialog: Add IP to blocklist
    if (showAddIpDialog) {
        var ipToBlock by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddIpDialog = false },
            title = { Text("Bloquear Dirección IP", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = ipToBlock,
                    onValueChange = { ipToBlock = it },
                    placeholder = { Text("ej. 192.168.1.150") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ipToBlock.isNotBlank()) {
                            viewModel.blockIp(ipToBlock.trim())
                            showAddIpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) {
                    Text("Bloquear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddIpDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = CyanNeon
            )
        )
    }
}
