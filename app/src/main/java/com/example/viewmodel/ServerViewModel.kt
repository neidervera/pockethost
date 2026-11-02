package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.file.DefaultProjects
import com.example.file.FileItem
import com.example.file.FileManager
import com.example.network.NetworkInfo
import com.example.network.NetworkType
import com.example.network.NetworkUtils
import com.example.server.PocketServerService
import com.example.server.ServerConfig
import com.example.server.ServerConfigManager
import com.example.server.ServerLog
import com.example.server.ServerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class EditorState(
    val isOpen: Boolean = false,
    val file: File? = null,
    val content: String = "",
    val originalContent: String = "",
    val isSaving: Boolean = false,
    val isRunningCode: Boolean = false,
    val executionResult: com.example.server.ExecutionResult? = null,
    val message: String? = null
)


class ServerViewModel(application: Application) : AndroidViewModel(application) {

    val fileManager = FileManager(application)
    private val configManager = ServerConfigManager(application)

    val serverState: StateFlow<ServerState> = PocketServerService.serverState
    val logs: StateFlow<List<ServerLog>> = PocketServerService.logs
    val config: StateFlow<ServerConfig> = configManager.config

    val networkInfo: StateFlow<NetworkInfo> = NetworkUtils.observeNetworkState(application)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NetworkUtils.getLocalIpAddress(application)
        )

    private val _currentDir = MutableStateFlow<File>(fileManager.getProjectDir())
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _diskUsage = MutableStateFlow(0L)
    val diskUsage: StateFlow<Long> = _diskUsage.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _qrModalVisible = MutableStateFlow(false)
    val qrModalVisible: StateFlow<Boolean> = _qrModalVisible.asStateFlow()

    init {
        // Populate demo site on launch
        DefaultProjects.populateDefaultProjects(fileManager)
        refreshFiles()
    }

    fun toggleServer() {
        val app = getApplication<Application>()
        if (serverState.value.isRunning) {
            PocketServerService.instance?.stopServer() ?: run {
                val intent = Intent(app, PocketServerService::class.java).apply {
                    action = PocketServerService.ACTION_STOP
                }
                app.startService(intent)
            }
        } else {
            val intent = Intent(app, PocketServerService::class.java).apply {
                action = PocketServerService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
    }

    fun showQrModal(show: Boolean) {
        _qrModalVisible.value = show
    }

    fun copyUrlToClipboard() {
        val url = serverState.value.serverUrl
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("PocketHost URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "URL copiada: $url", Toast.LENGTH_SHORT).show()
    }

    fun openInBrowser() {
        val url = serverState.value.serverUrl
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "No se encontró un navegador compatible", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareServerUrl() {
        val url = serverState.value.serverUrl
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Conéctate a mi servidor PocketHost en la red Wi-Fi: $url")
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Compartir enlace del Servidor").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(chooser)
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = _currentDir.value
            val items = fileManager.listFiles(dir)
            val totalBytes = fileManager.calculateDirectorySize(fileManager.getProjectDir(config.value.selectedProjectFolder))
            withContext(Dispatchers.Main) {
                _fileItems.value = items
                _diskUsage.value = totalBytes
            }
        }
    }

    fun navigateToDir(dir: File) {
        _currentDir.value = dir
        refreshFiles()
    }

    fun navigateUp() {
        val projectRoot = fileManager.getProjectDir(config.value.selectedProjectFolder)
        val current = _currentDir.value
        if (current.canonicalPath != projectRoot.canonicalPath) {
            current.parentFile?.let { parent ->
                _currentDir.value = parent
                refreshFiles()
            }
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.createFile(_currentDir.value, name, content)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    refreshFiles()
                    Toast.makeText(getApplication(), "Archivo creado: $name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createNewFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.createFolder(_currentDir.value, name)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    refreshFiles()
                    Toast.makeText(getApplication(), "Carpeta creada: $name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun renameItem(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.renameItem(file, newName)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    refreshFiles()
                    Toast.makeText(getApplication(), "Renombrado a $newName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Error al renombrar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteItem(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.deleteItem(file)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    refreshFiles()
                    Toast.makeText(getApplication(), "Eliminado: ${file.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun importZipStream(stream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.unzipStream(stream, _currentDir.value)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    refreshFiles()
                    Toast.makeText(getApplication(), "¡Proyecto ZIP extraído! (${res.getOrNull()} archivos)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(getApplication(), "Error al descomprimir ZIP: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openInEditor(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.readFileText(file)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    val text = res.getOrNull() ?: ""
                    _editorState.value = EditorState(
                        isOpen = true,
                        file = file,
                        content = text,
                        originalContent = text
                    )
                } else {
                    Toast.makeText(getApplication(), res.exceptionOrNull()?.message ?: "No se puede abrir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateEditorContent(content: String) {
        _editorState.value = _editorState.value.copy(content = content)
    }

    fun saveEditorFile() {
        val state = _editorState.value
        val file = state.file ?: return
        _editorState.value = state.copy(isSaving = true)

        viewModelScope.launch(Dispatchers.IO) {
            val res = fileManager.writeFileText(file, state.content)
            withContext(Dispatchers.Main) {
                if (res.isSuccess) {
                    _editorState.value = state.copy(
                        isSaving = false,
                        originalContent = state.content,
                        message = "Archivo guardado exitosamente"
                    )
                    refreshFiles()
                    Toast.makeText(getApplication(), "Guardado exitosamente", Toast.LENGTH_SHORT).show()
                } else {
                    _editorState.value = state.copy(isSaving = false, message = "Error al guardar")
                }
            }
        }
    }

    fun closeEditor() {
        _editorState.value = EditorState(isOpen = false)
    }

    fun resetDemoProject() {
        viewModelScope.launch(Dispatchers.IO) {
            val root = fileManager.getProjectDir(config.value.selectedProjectFolder)
            DefaultProjects.resetDemoProject(root)
            withContext(Dispatchers.Main) {
                _currentDir.value = root
                refreshFiles()
                Toast.makeText(getApplication(), "Demostración restaurada con éxito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Settings actions
    fun setPort(port: Int) {
        configManager.updatePort(port)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun setBasicAuth(enabled: Boolean, user: String, pass: String) {
        configManager.updateBasicAuth(enabled, user, pass)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun setDirectoryListing(enabled: Boolean) {
        configManager.updateDirectoryListing(enabled)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun setFileUploadAllowed(allowed: Boolean) {
        configManager.updateFileUpload(allowed)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun setDynamicTemplate(enabled: Boolean) {
        configManager.updateDynamicTemplate(enabled)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun blockIp(ip: String) {
        configManager.blockIp(ip)
        PocketServerService.instance?.updateConfig(configManager.config.value)
        Toast.makeText(getApplication(), "IP $ip bloqueada", Toast.LENGTH_SHORT).show()
    }

    fun unblockIp(ip: String) {
        configManager.unblockIp(ip)
        PocketServerService.instance?.updateConfig(configManager.config.value)
    }

    fun clearAllLogs() {
        PocketServerService.clearLogs()
    }

    fun executeEditorCode() {
        val state = _editorState.value
        val file = state.file ?: return
        val ext = file.extension.lowercase()
        val lang = when (ext) {
            "py" -> "python"
            "cpp", "c", "cc" -> "cpp"
            else -> return
        }

        _editorState.value = state.copy(isRunningCode = true, executionResult = null)
        viewModelScope.launch(Dispatchers.IO) {
            val runner = com.example.server.CodeRunner(fileManager.getProjectDir(config.value.selectedProjectFolder))
            val result = runner.execute(lang, state.content, autoInstallLibs = true)
            withContext(Dispatchers.Main) {
                _editorState.value = _editorState.value.copy(
                    isRunningCode = false,
                    executionResult = result
                )
            }
        }
    }

    fun executeEditor24x7() {
        val state = _editorState.value
        val file = state.file ?: return
        val ext = file.extension.lowercase()
        val lang = when (ext) {
            "py" -> "python"
            "cpp", "c", "cc" -> "cpp"
            else -> return
        }
        val rootDir = fileManager.getProjectDir(config.value.selectedProjectFolder)
        com.example.server.BackgroundWorkerManager.startWorker(rootDir, state.content, lang, file.name, autoInstallLibs = true)
        Toast.makeText(getApplication(), "Script corriendo 24/7 en segundo plano", Toast.LENGTH_SHORT).show()
    }

    fun dismissExecutionResult() {
        _editorState.value = _editorState.value.copy(executionResult = null)
    }
}
