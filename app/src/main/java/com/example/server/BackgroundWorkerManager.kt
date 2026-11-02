package com.example.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class WorkerStatus(
    val isRunning: Boolean = false,
    val scriptName: String = "",
    val language: String = "python",
    val startTime: Long = 0L,
    val uptimeSeconds: Long = 0L,
    val recentLogs: List<String> = emptyList(),
    val totalRunsCount: Long = 0L
)

object BackgroundWorkerManager {

    private val isRunningFlag = AtomicBoolean(false)
    private var workerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var scriptContent: String = ""
    private var scriptName: String = "script_24_7.py"
    private var language: String = "python"
    private var startTime: Long = 0L
    private val logBuffer = mutableListOf<String>()
    private var runCount: Long = 0L

    val isRunning: Boolean get() = isRunningFlag.get()

    fun startWorker(
        rootDirectory: File,
        code: String,
        lang: String = "python",
        name: String = "worker_24_7.py",
        autoInstallLibs: Boolean = true
    ): JSONObject {
        if (isRunningFlag.get()) {
            stopWorker()
        }

        scriptContent = code
        scriptName = name
        language = lang
        startTime = System.currentTimeMillis()
        isRunningFlag.set(true)
        runCount++

        synchronized(logBuffer) {
            logBuffer.clear()
            appendLog("[PocketHost 24/7] Iniciando proceso en segundo plano: $scriptName ($language)")
        }

        // Save worker script to root directory
        try {
            val scriptFile = File(rootDirectory, scriptName)
            scriptFile.writeText(code, Charsets.UTF_8)
            appendLog("[PocketHost 24/7] Script guardado en ${scriptFile.name}")
        } catch (e: Exception) {
            appendLog("[PocketHost 24/7] Advertencia al guardar archivo: ${e.message}")
        }

        val runner = CodeRunner(rootDirectory)

        workerJob = scope.launch {
            appendLog("[PocketHost 24/7] Proceso activo y corriendo continuamente en segundo plano.")

            // Continuous execution loop
            while (isActive && isRunningFlag.get()) {
                val loopStart = System.currentTimeMillis()
                try {
                    val result = runner.execute(language, scriptContent, autoInstallLibs = autoInstallLibs)
                    if (result.stdout.isNotEmpty()) {
                        appendLog("[stdout] " + result.stdout.trimEnd())
                    }
                    if (result.stderr.isNotEmpty()) {
                        appendLog("[stderr] " + result.stderr.trimEnd())
                    }
                    if (result.installedLibs.isNotEmpty()) {
                        appendLog("[libs] Auto-descargadas: " + result.installedLibs.joinToString())
                    }
                } catch (e: Exception) {
                    appendLog("[error] Excepcion en proceso: " + (e.message ?: "Error"))
                }

                // If code runs as a recurring loop or service, keep it alive
                val elapsed = System.currentTimeMillis() - loopStart
                if (elapsed < 5000) {
                    // Small delay between recurring script executions to prevent CPU exhaustion
                    kotlinx.coroutines.delay(5000 - elapsed)
                }
            }

            appendLog("[PocketHost 24/7] Proceso detenido.")
        }

        return getStatusJson()
    }

    fun stopWorker(): JSONObject {
        if (!isRunningFlag.get()) return getStatusJson()

        isRunningFlag.set(false)
        workerJob?.cancel()
        workerJob = null
        appendLog("[PocketHost 24/7] Servicio detenido por el usuario.")

        return getStatusJson()
    }

    fun getStatusJson(): JSONObject {
        val uptime = if (isRunningFlag.get()) (System.currentTimeMillis() - startTime) / 1000 else 0L
        val logsCopy = synchronized(logBuffer) { logBuffer.takeLast(60) }

        return JSONObject().apply {
            put("is_running", isRunningFlag.get())
            put("script_name", scriptName)
            put("language", language)
            put("uptime_seconds", uptime)
            put("uptime_formatted", formatUptime(uptime))
            put("total_runs", runCount)
            val jsonLogs = org.json.JSONArray()
            logsCopy.forEach { jsonLogs.put(it) }
            put("logs", jsonLogs)
        }
    }

    private fun appendLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        synchronized(logBuffer) {
            logBuffer.add("[$time] $msg")
            if (logBuffer.size > 200) {
                logBuffer.removeAt(0)
            }
        }
    }

    private fun formatUptime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}
