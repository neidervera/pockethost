package com.example.server

import android.os.Build
import android.util.Base64
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class PocketHttpServer(
    var port: Int = 8080,
    var rootDirectory: File,
    var config: ServerConfig = ServerConfig(),
    private val onStateUpdated: ((ServerState) -> Unit)? = null,
    private val onLogCreated: ((ServerLog) -> Unit)? = null
) {
    private var serverSocket: ServerSocket? = null
    private var executor: ExecutorService? = null
    private val isRunningFlag = AtomicBoolean(false)

    val isRunning: Boolean get() = isRunningFlag.get()

    private val activeConnections = AtomicInteger(0)
    private val totalRequests = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private var startTimeMillis: Long = 0L

    fun start(): Result<Unit> {
        if (isRunningFlag.get()) return Result.success(Unit)

        return try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
            serverSocket = socket

            executor = Executors.newCachedThreadPool()
            isRunningFlag.set(true)
            startTimeMillis = System.currentTimeMillis()

            notifyState()

            // Main acceptor loop thread
            Thread {
                while (isRunningFlag.get() && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        executor?.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunningFlag.get()) break
                    }
                }
            }.apply {
                name = "PocketHost-Acceptor"
                isDaemon = true
                start()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            isRunningFlag.set(false)
            notifyState(lastError = e.message ?: "Error al iniciar servidor")
            Result.failure(e)
        }
    }

    fun stop() {
        if (!isRunningFlag.get()) return
        isRunningFlag.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore socket close errors
        }
        executor?.shutdownNow()
        serverSocket = null
        executor = null
        activeConnections.set(0)
        notifyState()
    }

    private fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress.hostAddress ?: "desconocido"
        val startTime = System.currentTimeMillis()
        var method = "GET"
        var requestPath = "/"
        var statusCode = 200
        var sentBytes = 0L
        var userAgent = ""
        var errorMsg: String? = null

        activeConnections.incrementAndGet()
        notifyState()

        try {
            socket.soTimeout = 15000 // 15s timeout
            val inputStream = BufferedInputStream(socket.getInputStream())
            val outputStream = BufferedOutputStream(socket.getOutputStream())

            // 1. Check IP Blacklist
            if (config.blockedIps.contains(clientIp)) {
                statusCode = 403
                val body = "<h1>403 Prohibido</h1><p>Tu dirección IP ($clientIp) ha sido bloqueada por el administrador.</p>"
                sendResponse(outputStream, 403, "Forbidden", "text/html; charset=utf-8", body.toByteArray())
                sentBytes = body.length.toLong()
                return
            }

            // 2. Read Request Line and Headers
            val headerLines = readHeaders(inputStream)
            if (headerLines.isEmpty()) return

            val requestLine = headerLines[0].split(" ")
            if (requestLine.size < 2) return

            method = requestLine[0].uppercase()
            val rawUri = requestLine[1]
            val queryIndex = rawUri.indexOf('?')
            requestPath = if (queryIndex != -1) rawUri.substring(0, queryIndex) else rawUri
            val queryString = if (queryIndex != -1) rawUri.substring(queryIndex + 1) else ""

            val headers = mutableMapOf<String, String>()
            for (i in 1 until headerLines.size) {
                val colon = headerLines[i].indexOf(':')
                if (colon != -1) {
                    val key = headerLines[i].substring(0, colon).trim().lowercase()
                    val value = headerLines[i].substring(colon + 1).trim()
                    headers[key] = value
                }
            }
            userAgent = headers["user-agent"] ?: ""

            // 3. Check Basic Authentication if enabled
            if (config.isBasicAuthEnabled) {
                val authHeader = headers["authorization"]
                val valid = checkBasicAuth(authHeader)
                if (!valid) {
                    statusCode = 401
                    val body = "<h1>401 No Autorizado</h1><p>Se requieren credenciales válidas para acceder a PocketHost.</p>"
                    val extraHeaders = mapOf("WWW-Authenticate" to "Basic realm=\"PocketHost Server\"")
                    sendResponse(outputStream, 401, "Unauthorized", "text/html; charset=utf-8", body.toByteArray(), extraHeaders)
                    sentBytes = body.length.toLong()
                    return
                }
            }

            // 4. Route APIs
            if (requestPath.startsWith("/api/")) {
                val apiResult = handleApiRequest(method, requestPath, queryString, headers, inputStream, clientIp)
                statusCode = apiResult.status
                sendResponse(outputStream, apiResult.status, apiResult.statusText, apiResult.contentType, apiResult.body, apiResult.extraHeaders)
                sentBytes = apiResult.body.size.toLong()
                return
            }

            // 5. Handle File Upload via POST
            if (method == "POST" && requestPath == "/upload") {
                if (!config.isFileUploadAllowed) {
                    statusCode = 403
                    val body = "Subida de archivos deshabilitada en la configuración".toByteArray()
                    sendResponse(outputStream, 403, "Forbidden", "text/plain; charset=utf-8", body)
                    sentBytes = body.size.toLong()
                    return
                }
                // Handle file upload
                val queryParams = parseQueryParams(queryString)
                val fileName = queryParams["name"] ?: "archivo_${System.currentTimeMillis()}"
                val safeFileName = File(fileName).name // Prevent path injection
                val targetFile = File(rootDirectory, safeFileName)
                val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

                FileOutputStream(targetFile).use { fos ->
                    copyStreamWithLimit(inputStream, fos, contentLength)
                }

                val body = "{\"success\":true,\"file\":\"$safeFileName\",\"size\":${targetFile.length()}}".toByteArray()
                statusCode = 200
                sendResponse(outputStream, 200, "OK", "application/json", body)
                sentBytes = body.size.toLong()
                return
            }

            // 6. Serve Static Files
            val decodedPath = URLDecoder.decode(requestPath, "UTF-8")
            val targetFile = resolveFile(rootDirectory, decodedPath)

            if (targetFile == null) {
                // Path traversal detected!
                statusCode = 403
                val body = "<h1>403 Prohibido</h1><p>Intento de acceso fuera del directorio raíz denegado.</p>".toByteArray()
                sendResponse(outputStream, 403, "Forbidden", "text/html; charset=utf-8", body)
                sentBytes = body.size.toLong()
                return
            }

            if (!targetFile.exists()) {
                statusCode = 404
                val body = generate404Html(requestPath).toByteArray()
                sendResponse(outputStream, 404, "Not Found", "text/html; charset=utf-8", body)
                sentBytes = body.size.toLong()
                return
            }

            if (targetFile.isDirectory) {
                val indexFile = File(targetFile, "index.html").takeIf { it.exists() }
                    ?: File(targetFile, "index.htm").takeIf { it.exists() }

                if (indexFile != null) {
                    sentBytes = serveFile(indexFile, headers, outputStream, clientIp)
                } else if (config.isDirectoryListingEnabled) {
                    val html = generateDirectoryListingHtml(targetFile, requestPath)
                    val body = html.toByteArray(Charsets.UTF_8)
                    sendResponse(outputStream, 200, "OK", "text/html; charset=utf-8", body)
                    sentBytes = body.size.toLong()
                } else {
                    statusCode = 403
                    val body = "<h1>403 Directorio Prohibido</h1><p>El listado de directorios está deshabilitado.</p>".toByteArray()
                    sendResponse(outputStream, 403, "Forbidden", "text/html; charset=utf-8", body)
                    sentBytes = body.size.toLong()
                }
            } else {
                sentBytes = serveFile(targetFile, headers, outputStream, clientIp)
            }

        } catch (e: Exception) {
            statusCode = 500
            errorMsg = e.message
            errorCount.incrementAndGet()
            try {
                val body = "<h1>500 Error Interno</h1><p>${e.message}</p>".toByteArray()
                sendResponse(BufferedOutputStream(socket.getOutputStream()), 500, "Internal Server Error", "text/html; charset=utf-8", body)
                sentBytes = body.size.toLong()
            } catch (ignored: Exception) {}
        } finally {
            val duration = System.currentTimeMillis() - startTime
            totalRequests.incrementAndGet()
            bytesSent.addAndGet(sentBytes)
            activeConnections.decrementAndGet()

            try {
                socket.close()
            } catch (ignored: Exception) {}

            onLogCreated?.invoke(
                ServerLog(
                    clientIp = clientIp,
                    method = method,
                    path = requestPath,
                    statusCode = statusCode,
                    bytesSent = sentBytes,
                    durationMs = duration,
                    userAgent = userAgent,
                    errorDetail = errorMsg
                )
            )

            notifyState()
        }
    }

    private fun checkBasicAuth(authHeader: String?): Boolean {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false
        return try {
            val encoded = authHeader.removePrefix("Basic ").trim()
            val decoded = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            val parts = decoded.split(":", limit = 2)
            if (parts.size == 2) {
                parts[0] == config.authUsername && parts[1] == config.authPassword
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private data class ApiResponse(
        val status: Int,
        val statusText: String,
        val contentType: String,
        val body: ByteArray,
        val extraHeaders: Map<String, String> = emptyMap()
    )

    private fun handleApiRequest(
        method: String,
        path: String,
        queryString: String,
        headers: Map<String, String>,
        inputStream: InputStream,
        clientIp: String
    ): ApiResponse {
        val query = parseQueryParams(queryString)
        return when (path) {
            "/api/status" -> {
                val uptimeSec = (System.currentTimeMillis() - startTimeMillis) / 1000
                val json = """
{
  "server": "PocketHost Server",
  "version": "1.0.0",
  "status": "online",
  "port": $port,
  "device": "${Build.MANUFACTURER.replace("\"", "")} ${Build.MODEL.replace("\"", "")}",
  "uptime_seconds": $uptimeSec,
  "active_connections": ${activeConnections.get()},
  "total_requests": ${totalRequests.get()},
  "bytes_sent": ${bytesSent.get()},
  "bytes_received": ${bytesReceived.get()},
  "root_folder": "${rootDirectory.name}"
}
                """.trimIndent()
                ApiResponse(200, "OK", "application/json", json.toByteArray(Charsets.UTF_8))
            }
            "/api/ping" -> {
                val json = "{\"pong\":true,\"timestamp\":${System.currentTimeMillis()},\"client_ip\":\"$clientIp\"}"
                ApiResponse(200, "OK", "application/json", json.toByteArray(Charsets.UTF_8))
            }
            "/api/files" -> {
                val subPath = query["path"] ?: ""
                val targetDir = resolveFile(rootDirectory, subPath) ?: rootDirectory
                val files = targetDir.listFiles() ?: emptyArray()
                val list = files.joinToString(",") { f ->
                    val relPath = f.canonicalPath.removePrefix(rootDirectory.canonicalPath).trimStart(File.separatorChar)
                    val mime = if (f.isDirectory) "inode/directory" else getMimeType(f.extension.lowercase())
                    "{\"name\":\"${f.name}\",\"path\":\"$relPath\",\"is_dir\":${f.isDirectory},\"size\":${f.length()},\"formatted_size\":\"${ServerState.formatBytes(f.length())}\",\"modified\":${f.lastModified()},\"extension\":\"${f.extension.lowercase()}\",\"mime\":\"$mime\"}"
                }
                val json = "{\"files\":[$list]}"
                ApiResponse(200, "OK", "application/json", json.toByteArray(Charsets.UTF_8))
            }
            "/api/download" -> {
                val filePath = query["file"] ?: ""
                val file = resolveFile(rootDirectory, filePath)
                if (file == null || !file.exists() || file.isDirectory) {
                    ApiResponse(404, "Not Found", "application/json", "{\"error\":\"Archivo no encontrado\"}".toByteArray())
                } else {
                    val mime = getMimeType(file.extension.lowercase())
                    val extra = mapOf("Content-Disposition" to "attachment; filename=\"${file.name}\"")
                    ApiResponse(200, "OK", mime, file.readBytes(), extra)
                }
            }
            "/api/preview" -> {
                val filePath = query["file"] ?: ""
                val file = resolveFile(rootDirectory, filePath)
                if (file == null || !file.exists() || file.isDirectory) {
                    ApiResponse(404, "Not Found", "application/json", "{\"error\":\"Archivo no encontrado\"}".toByteArray())
                } else {
                    val mime = getMimeType(file.extension.lowercase())
                    val extra = mapOf("Content-Disposition" to "inline; filename=\"${file.name}\"")
                    ApiResponse(200, "OK", mime, file.readBytes(), extra)
                }
            }
            "/api/upload" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST\"}".toByteArray())
                } else if (!config.isFileUploadAllowed) {
                    ApiResponse(403, "Forbidden", "application/json", "{\"error\":\"Subida de archivos deshabilitada\"}".toByteArray())
                } else {
                    val name = query["name"] ?: "upload_${System.currentTimeMillis()}"
                    val dirParam = query["dir"] ?: ""
                    val targetDir = resolveFile(rootDirectory, dirParam) ?: rootDirectory
                    targetDir.mkdirs()
                    val safeName = File(name).name
                    val file = File(targetDir, safeName)
                    val len = headers["content-length"]?.toLongOrNull() ?: 0L
                    FileOutputStream(file).use { fos ->
                        copyStreamWithLimit(inputStream, fos, len)
                    }
                    val json = "{\"success\":true,\"name\":\"$safeName\",\"size\":${file.length()},\"formatted_size\":\"${ServerState.formatBytes(file.length())}\"}"
                    ApiResponse(200, "OK", "application/json", json.toByteArray(Charsets.UTF_8))
                }
            }
            "/api/delete" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST\"}".toByteArray())
                } else {
                    val filePath = query["file"] ?: ""
                    val file = resolveFile(rootDirectory, filePath)
                    if (file == null || !file.exists() || file.canonicalPath == rootDirectory.canonicalPath) {
                        ApiResponse(400, "Bad Request", "application/json", "{\"error\":\"No se puede eliminar ese elemento\"}".toByteArray())
                    } else {
                        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                        ApiResponse(200, "OK", "application/json", "{\"success\":$deleted}".toByteArray(Charsets.UTF_8))
                    }
                }
            }
            "/api/create_folder" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST\"}".toByteArray())
                } else {
                    val folderName = query["name"] ?: "nueva_carpeta"
                    val dirParam = query["dir"] ?: ""
                    val targetDir = resolveFile(rootDirectory, dirParam) ?: rootDirectory
                    val safeFolder = File(targetDir, File(folderName).name)
                    val created = safeFolder.mkdirs()
                    ApiResponse(200, "OK", "application/json", "{\"success\":$created,\"name\":\"${safeFolder.name}\"}".toByteArray(Charsets.UTF_8))
                }
            }
            "/api/run" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST con JSON\"}".toByteArray())
                } else {
                    val len = headers["content-length"]?.toIntOrNull() ?: 0
                    val bodyBytes = ByteArray(len)
                    var readTotal = 0
                    while (readTotal < len) {
                        val r = inputStream.read(bodyBytes, readTotal, len - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    val bodyStr = String(bodyBytes, Charsets.UTF_8)
                    val json = try { JSONObject(bodyStr) } catch (e: Exception) { JSONObject() }
                    val lang = json.optString("language", "python")
                    val code = json.optString("code", "")
                    val stdin = json.optString("stdin", "")
                    val autoLibs = json.optBoolean("auto_install_libs", true)

                    val runner = CodeRunner(rootDirectory)
                    val result = runBlocking {
                        runner.execute(lang, code, stdin, autoLibs)
                    }

                    val resObj = JSONObject().apply {
                        put("success", result.success)
                        put("language", result.language)
                        put("stdout", result.stdout)
                        put("stderr", result.stderr)
                        put("exit_code", result.exitCode)
                        put("duration_ms", result.durationMs)
                        val libsArray = JSONArray()
                        result.installedLibs.forEach { libsArray.put(it) }
                        put("installed_libs", libsArray)
                        if (result.error != null) put("error", result.error)
                    }
                    ApiResponse(200, "OK", "application/json", resObj.toString().toByteArray(Charsets.UTF_8))
                }
            }
            "/api/daemon/start" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST\"}".toByteArray())
                } else {
                    val len = headers["content-length"]?.toIntOrNull() ?: 0
                    val bodyBytes = ByteArray(len)
                    var readTotal = 0
                    while (readTotal < len) {
                        val r = inputStream.read(bodyBytes, readTotal, len - readTotal)
                        if (r == -1) break
                        readTotal += r
                    }
                    val json = try { JSONObject(String(bodyBytes, Charsets.UTF_8)) } catch (e: Exception) { JSONObject() }
                    val code = json.optString("code", "")
                    val lang = json.optString("language", "python")
                    val name = json.optString("name", "worker_24_7.py")
                    val autoLibs = json.optBoolean("auto_install_libs", true)

                    val status = BackgroundWorkerManager.startWorker(rootDirectory, code, lang, name, autoLibs)
                    ApiResponse(200, "OK", "application/json", status.toString().toByteArray(Charsets.UTF_8))
                }
            }
            "/api/daemon/stop" -> {
                if (method != "POST") {
                    ApiResponse(405, "Method Not Allowed", "application/json", "{\"error\":\"Use POST\"}".toByteArray())
                } else {
                    val status = BackgroundWorkerManager.stopWorker()
                    ApiResponse(200, "OK", "application/json", status.toString().toByteArray(Charsets.UTF_8))
                }
            }
            "/api/daemon/status" -> {
                val status = BackgroundWorkerManager.getStatusJson()
                ApiResponse(200, "OK", "application/json", status.toString().toByteArray(Charsets.UTF_8))
            }
            else -> {
                ApiResponse(404, "Not Found", "application/json", "{\"error\":\"API endpoint not found\"}".toByteArray())
            }
        }
    }

    private fun serveFile(
        file: File,
        headers: Map<String, String>,
        outputStream: OutputStream,
        clientIp: String
    ): Long {
        val extension = file.extension.lowercase()
        val mimeType = getMimeType(extension)

        // Interpolate templates for HTML files if enabled
        if (config.isDynamicTemplateEnabled && (extension == "html" || extension == "htm")) {
            val content = file.readText(Charsets.UTF_8)
            val uptime = (System.currentTimeMillis() - startTimeMillis) / 1000
            val hours = uptime / 3600
            val minutes = (uptime % 3600) / 60
            val seconds = uptime % 60
            val uptimeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val device = "${Build.MANUFACTURER} ${Build.MODEL}"

            val interpolated = content
                .replace("{{server_time}}", timeStr)
                .replace("{{server_uptime}}", uptimeStr)
                .replace("{{client_ip}}", clientIp)
                .replace("{{device_model}}", device)
                .replace("{{pocket_version}}", "1.0.0")

            val bytes = interpolated.toByteArray(Charsets.UTF_8)
            sendResponse(outputStream, 200, "OK", mimeType, bytes)
            return bytes.size.toLong()
        }

        // Support HTTP Range Header for audio/video playback and resume
        val rangeHeader = headers["range"]
        val fileLength = file.length()

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val rangeSpec = rangeHeader.removePrefix("bytes=").trim()
            val parts = rangeSpec.split("-")
            val start = parts[0].toLongOrNull() ?: 0L
            val end = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toLongOrNull() ?: (fileLength - 1) else (fileLength - 1)

            val contentLength = (end - start + 1).coerceAtLeast(0)

            val headerText = StringBuilder()
            headerText.append("HTTP/1.1 206 Partial Content\r\n")
            headerText.append("Content-Type: $mimeType\r\n")
            headerText.append("Content-Length: $contentLength\r\n")
            headerText.append("Content-Range: bytes $start-$end/$fileLength\r\n")
            headerText.append("Accept-Ranges: bytes\r\n")
            headerText.append("Connection: close\r\n\r\n")

            outputStream.write(headerText.toString().toByteArray(Charsets.UTF_8))

            FileInputStream(file).use { fis ->
                fis.skip(start)
                val buffer = ByteArray(8192)
                var remaining = contentLength
                while (remaining > 0) {
                    val read = fis.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read == -1) break
                    outputStream.write(buffer, 0, read)
                    remaining -= read
                }
            }
            outputStream.flush()
            return contentLength
        } else {
            // Full file transfer
            val headerText = StringBuilder()
            headerText.append("HTTP/1.1 200 OK\r\n")
            headerText.append("Content-Type: $mimeType\r\n")
            headerText.append("Content-Length: $fileLength\r\n")
            headerText.append("Accept-Ranges: bytes\r\n")
            headerText.append("Connection: close\r\n\r\n")

            outputStream.write(headerText.toString().toByteArray(Charsets.UTF_8))

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var count: Int
                while (fis.read(buffer).also { count = it } != -1) {
                    outputStream.write(buffer, 0, count)
                }
            }
            outputStream.flush()
            return fileLength
        }
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val header = StringBuilder()
        header.append("HTTP/1.1 $statusCode $statusText\r\n")
        header.append("Content-Type: $contentType\r\n")
        header.append("Content-Length: ${body.size}\r\n")
        header.append("Server: PocketHost/1.0 (Android)\r\n")
        header.append("Access-Control-Allow-Origin: *\r\n")
        for ((k, v) in extraHeaders) {
            header.append("$k: $v\r\n")
        }
        header.append("Connection: close\r\n\r\n")

        output.write(header.toString().toByteArray(Charsets.UTF_8))
        output.write(body)
        output.flush()
    }

    private fun resolveFile(rootDir: File, requestPath: String): File? {
        val cleanPath = requestPath.trimStart('/').replace('\\', '/')
        val target = File(rootDir, cleanPath)
        val canonicalRoot = rootDir.canonicalPath
        val canonicalTarget = target.canonicalPath

        // Prevent path traversal
        return if (canonicalTarget.startsWith(canonicalRoot)) target else null
    }

    private fun readHeaders(inputStream: InputStream): List<String> {
        val lines = mutableListOf<String>()
        val lineBuffer = ByteArrayOutputStream()
        var lastChar = -1

        while (true) {
            val b = inputStream.read()
            if (b == -1) break
            if (b == '\n'.code && lastChar == '\r'.code) {
                val raw = lineBuffer.toByteArray()
                // Drop trailing \r
                val str = String(raw, 0, (raw.size - 1).coerceAtLeast(0), Charsets.UTF_8)
                if (str.isEmpty()) break // End of headers
                lines.add(str)
                lineBuffer.reset()
            } else {
                lineBuffer.write(b)
            }
            lastChar = b
        }
        return lines
    }

    private fun copyStreamWithLimit(input: InputStream, output: OutputStream, limit: Long) {
        val buffer = ByteArray(8192)
        var remaining = if (limit > 0) limit else Long.MAX_VALUE
        while (remaining > 0) {
            val toRead = if (limit > 0) minOf(buffer.size.toLong(), remaining).toInt() else buffer.size
            val read = input.read(buffer, 0, toRead)
            if (read == -1) break
            output.write(buffer, 0, read)
            if (limit > 0) remaining -= read
            bytesReceived.addAndGet(read.toLong())
        }
        output.flush()
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf("=")
            if (idx != -1) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun getMimeType(ext: String): String {
        return when (ext) {
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "txt", "md" -> "text/plain; charset=utf-8"
            "xml" -> "application/xml; charset=utf-8"
            "py" -> "text/x-python; charset=utf-8"
            "cpp", "c", "h", "hpp" -> "text/x-c; charset=utf-8"
            else -> "application/octet-stream"
        }
    }

    private fun generateDirectoryListingHtml(dir: File, requestPath: String): String {
        val files = dir.listFiles() ?: emptyArray()
        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val parentPath = if (requestPath == "/" || requestPath.isEmpty()) null else {
            val idx = requestPath.trimEnd('/').lastIndexOf('/')
            if (idx == -1) "/" else requestPath.substring(0, idx + 1)
        }

        val rows = StringBuilder()
        if (parentPath != null) {
            rows.append("""<tr><td>[SUBIR] <a href="$parentPath">.. (Directorio superior)</a></td><td>--</td><td>--</td></tr>""")
        }

        for (f in sortedFiles) {
            val icon = if (f.isDirectory) "[CARPETA]" else "[ARCHIVO]"
            val link = (requestPath.trimEnd('/') + "/" + f.name)
            val size = if (f.isDirectory) "--" else ServerState.formatBytes(f.length())
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(f.lastModified()))
            rows.append("""<tr><td>$icon <a href="$link">${f.name}</a></td><td>$size</td><td>$date</td></tr>""")
        }

        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Índice de $requestPath - PocketHost</title>
    <style>
        body { font-family: monospace; background: #0b0f17; color: #f0f6fc; padding: 24px; }
        h1 { color: #00e5ff; border-bottom: 1px solid #30363d; padding-bottom: 10px; }
        table { width: 100%; border-collapse: collapse; margin-top: 16px; }
        th, td { text-align: left; padding: 10px; border-bottom: 1px solid #21262d; }
        th { color: #8b949e; }
        a { color: #58a6ff; text-decoration: none; }
        a:hover { text-decoration: underline; color: #00e5ff; }
    </style>
</head>
<body>
    <h1>Índice de $requestPath</h1>
    <table>
        <tr><th>Nombre</th><th>Tamaño</th><th>Modificado</th></tr>
        $rows
    </table>
    <p style="margin-top:20px; font-size:0.8rem; color:#8b949e;">PocketHost Server en Android</p>
</body>
</html>
        """.trimIndent()
    }

    private fun generate404Html(path: String): String {
        return """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>404 No Encontrado - PocketHost</title>
    <style>
        body { background: #0b0f17; color: #f0f6fc; font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
        .box { text-align: center; background: #161b22; border: 1px solid #30363d; padding: 40px; border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); }
        h1 { font-size: 3rem; color: #ff3366; margin: 0; }
        p { color: #8b949e; margin-top: 10px; }
        a { color: #00e5ff; text-decoration: none; font-weight: bold; }
    </style>
</head>
<body>
    <div class="box">
        <h1>404</h1>
        <h2>Archivo No Encontrado</h2>
        <p>La ruta <code>$path</code> no existe en el servidor local.</p>
        <p><a href="/">← Ir a la página principal</a></p>
    </div>
</body>
</html>
        """.trimIndent()
    }

    private fun notifyState(lastError: String? = null) {
        val uptime = if (isRunningFlag.get()) (System.currentTimeMillis() - startTimeMillis) / 1000 else 0L
        val state = ServerState(
            isRunning = isRunningFlag.get(),
            port = port,
            rootDirectory = rootDirectory,
            activeConnections = activeConnections.get(),
            totalRequests = totalRequests.get(),
            bytesSent = bytesSent.get(),
            bytesReceived = bytesReceived.get(),
            uptimeSeconds = uptime,
            errorCount = errorCount.get(),
            lastError = lastError
        )
        onStateUpdated?.invoke(state)
    }
}
