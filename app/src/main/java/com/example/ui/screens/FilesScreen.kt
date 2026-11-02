package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.file.FileItem
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(
    viewModel: ServerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDir by viewModel.currentDir.collectAsState()
    val fileItems by viewModel.fileItems.collectAsState()
    val config by viewModel.config.collectAsState()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<File?>(null) }
    var itemToDelete by remember { mutableStateOf<File?>(null) }

    // Android file picker for ZIP import
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it)
            if (stream != null) {
                viewModel.importZipStream(stream)
            }
        }
    }

    // Android file picker for single file upload
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                val fileName = "archivo_${System.currentTimeMillis()}"
                viewModel.createNewFile(fileName, input.readBytes().toString(Charsets.UTF_8))
            }
        }
    }

    val isAtRoot = currentDir.canonicalPath == viewModel.fileManager.getProjectDir(config.selectedProjectFolder).canonicalPath

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // Top Navigation Bar / Breadcrumbs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateUp() },
                enabled = !isAtRoot,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Carpeta superior",
                    tint = if (!isAtRoot) CyanNeon else TextSecondary.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Directorio raíz: /${config.selectedProjectFolder}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = if (isAtRoot) "/" else currentDir.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { viewModel.refreshFiles() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar archivos",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                icon = Icons.Default.Add,
                label = "Archivo",
                onClick = { showNewFileDialog = true },
                modifier = Modifier.weight(1f),
                testTag = "btn_new_file"
            )
            ActionChip(
                icon = Icons.Default.CreateNewFolder,
                label = "Carpeta",
                onClick = { showNewFolderDialog = true },
                modifier = Modifier.weight(1f),
                testTag = "btn_new_folder"
            )
            ActionChip(
                icon = Icons.Default.FolderZip,
                label = "Extraer ZIP",
                onClick = { zipPickerLauncher.launch("application/zip") },
                modifier = Modifier.weight(1f),
                testTag = "btn_import_zip"
            )
            ActionChip(
                icon = Icons.Default.UploadFile,
                label = "Subir",
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1f),
                testTag = "btn_upload_file"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Files List or Empty State
        if (fileItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Esta carpeta está vacía",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.resetDemoProject() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restaurar Proyectos Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                items(fileItems, key = { it.path }) { item ->
                    FileRowItem(
                        item = item,
                        onClick = {
                            if (item.isDirectory) {
                                viewModel.navigateToDir(item.file)
                            } else {
                                viewModel.openInEditor(item.file)
                            }
                        },
                        onEdit = { viewModel.openInEditor(item.file) },
                        onRename = { itemToRename = item.file },
                        onDelete = { itemToDelete = item.file }
                    )
                }
            }
        }
    }

    // Dialog: Create New File
    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Crear Nuevo Archivo", color = TextPrimary) },
            text = {
                Column {
                    Text("Introduce el nombre con extensión (ej. index.html, style.css, script.js):", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        placeholder = { Text("mi_pagina.html") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_file_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            viewModel.createNewFile(fileName.trim())
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = BackgroundDark)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Create New Folder
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Crear Nueva Carpeta", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    placeholder = { Text("assets, js, css...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_folder_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createNewFolder(folderName.trim())
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = BackgroundDark)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Rename
    itemToRename?.let { target ->
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Renombrar", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newName != target.name) {
                            viewModel.renameItem(target, newName.trim())
                        }
                        itemToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = BackgroundDark)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToRename = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Dialog: Delete Confirmation
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar ${if (target.isDirectory) "Carpeta" else "Archivo"}", color = CrimsonError) },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente \"${target.name}\"?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyanNeon,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FileRowItem(
    item: FileItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (icon, iconColor) = when {
        item.isDirectory -> Pair(Icons.Default.Folder, CyanNeon)
        item.extension in listOf("html", "htm") -> Pair(Icons.Default.Code, Color(0xFFFF5722))
        item.extension == "css" -> Pair(Icons.Default.Code, Color(0xFF29B6F6))
        item.extension in listOf("js", "mjs") -> Pair(Icons.Default.Code, Color(0xFFFFCA28))
        item.extension in listOf("png", "jpg", "jpeg", "svg", "webp", "gif") -> Pair(Icons.Default.Image, PurpleAccent)
        item.extension == "zip" -> Pair(Icons.Default.FolderZip, AmberWarning)
        else -> Pair(Icons.Default.Description, TextSecondary)
    }

    val isEditable = !item.isDirectory && item.extension in listOf("html", "htm", "css", "js", "json", "txt", "md", "xml", "svg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("file_item_${item.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = item.formattedSize,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = " • " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.lastModified)),
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = TextSecondary
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(SurfaceDark)
            ) {
                if (isEditable) {
                    DropdownMenuItem(
                        text = { Text("Editar código", color = CyanNeon) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Renombrar", color = TextPrimary) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                    onClick = {
                        menuExpanded = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar", color = CrimsonError) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonError) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
