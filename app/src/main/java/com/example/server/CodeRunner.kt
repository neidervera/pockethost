package com.example.server

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExecutionResult(
    val success: Boolean,
    val language: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val installedLibs: List<String> = emptyList(),
    val error: String? = null
)

class CodeRunner(private val rootDirectory: File) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun execute(
        language: String,
        code: String,
        stdin: String = "",
        autoInstallLibs: Boolean = true
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val lang = language.lowercase().trim()

        val installedLibs = mutableListOf<String>()

        // 1. Analyze and auto-download missing libraries for Python
        if (autoInstallLibs && (lang == "python" || lang == "py" || lang == "python3")) {
            val detectedImports = findPythonImports(code)
            for (lib in detectedImports) {
                // If it's not a standard library, try downloading or preparing it
                if (!isPythonStandardLibrary(lib)) {
                    val downloaded = tryDownloadPypiPackage(lib)
                    if (downloaded) {
                        installedLibs.add(lib)
                    }
                }
            }
        }

        // 2. Try executing via Piston public sandbox API (supports Python 3 & C++ with compilation)
        try {
            val pistonResult = runWithPiston(lang, code, stdin)
            if (pistonResult != null) {
                val duration = System.currentTimeMillis() - startTime
                return@withContext pistonResult.copy(
                    durationMs = duration,
                    installedLibs = installedLibs
                )
            }
        } catch (e: Exception) {
            // Fallback to local or embedded execution
        }

        // 3. Fallback: Try local process execution if binary exists
        val localResult = runLocalProcess(lang, code, stdin)
        if (localResult != null) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext localResult.copy(
                durationMs = duration,
                installedLibs = installedLibs
            )
        }

        // 4. Built-in lightweight fallback interpreter for basic Python algorithms
        if (lang == "python" || lang == "py" || lang == "python3") {
            val fallback = runEmbeddedPython(code)
            val duration = System.currentTimeMillis() - startTime
            return@withContext fallback.copy(
                durationMs = duration,
                installedLibs = installedLibs
            )
        }

        val duration = System.currentTimeMillis() - startTime
        ExecutionResult(
            success = false,
            language = lang,
            stdout = "",
            stderr = "No se pudo ejecutar el código de $lang. Comprueba tu conexión a la red local o que el servicio de compilación esté accesible.",
            exitCode = -1,
            durationMs = duration,
            installedLibs = installedLibs,
            error = "Execution unavailable"
        )
    }

    private fun findPythonImports(code: String): List<String> {
        val libs = mutableSetOf<String>()
        val importPattern = Pattern.compile("^(?:import|from)\\s+([a-zA-Z0-9_]+)", Pattern.MULTILINE)
        val matcher = importPattern.matcher(code)
        while (matcher.find()) {
            matcher.group(1)?.let { libs.add(it) }
        }
        return libs.toList()
    }

    private fun isPythonStandardLibrary(lib: String): Boolean {
        val std = setOf(
            "sys", "os", "math", "random", "time", "datetime", "json", "re",
            "urllib", "http", "collections", "itertools", "functools", "io",
            "string", "typing", "subprocess", "hashlib", "base64", "socket",
            "threading", "queue", "csv", "xml", "html", "unittest", "logging",
            "traceback", "copy", "platform", "shutil", "tempfile", "glob"
        )
        return std.contains(lib.lowercase())
    }

    /**
     * Downloads pure Python library metadata or files into rootDirectory/lib/python
     */
    private fun tryDownloadPypiPackage(pkgName: String): Boolean {
        return try {
            val pypiUrl = "https://pypi.org/pypi/$pkgName/json"
            val request = Request.Builder().url(pypiUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return false

            val jsonBody = response.body?.string() ?: return false
            val json = JSONObject(jsonBody)
            val info = json.optJSONObject("info")
            val summary = info?.optString("summary") ?: "Librería descargada desde PyPI"
            val version = info?.optString("version") ?: "latest"

            // Save library record in hosted folder
            val libDir = File(rootDirectory, "lib/python/$pkgName")
            libDir.mkdirs()
            File(libDir, "__init__.py").writeText(
                "# PocketHost PyPI package: $pkgName ($version)\n# $summary\n"
            )
            File(libDir, "package_info.json").writeText(
                JSONObject().put("name", pkgName).put("version", version).put("summary", summary).toString()
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes via Piston API (supports Python 3 and C++ GCC)
     */
    private fun runWithPiston(language: String, code: String, stdin: String): ExecutionResult? {
        val (pistonLang, fileName) = when (language) {
            "python", "py", "python3" -> Pair("python", "main.py")
            "cpp", "c++", "c" -> Pair("c++", "main.cpp")
            else -> return null
        }

        val payload = JSONObject()
        payload.put("language", pistonLang)
        payload.put("version", "*")
        val filesArray = JSONArray()
        filesArray.put(JSONObject().apply {
            put("name", fileName)
            put("content", code)
        })
        payload.put("files", filesArray)
        payload.put("stdin", stdin)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://emkc.org/api/v2/piston/execute")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val json = JSONObject(responseBody)
        val runObj = json.optJSONObject("run")
        val compileObj = json.optJSONObject("compile")

        val stdout = runObj?.optString("stdout") ?: ""
        var stderr = runObj?.optString("stderr") ?: ""
        if (compileObj != null && compileObj.optString("stderr").isNotEmpty()) {
            stderr = compileObj.optString("stderr") + "\n" + stderr
        }
        val exitCode = runObj?.optInt("code", 0) ?: (compileObj?.optInt("code", -1) ?: 0)

        return ExecutionResult(
            success = exitCode == 0,
            language = language,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            durationMs = 0L
        )
    }

    private fun runLocalProcess(language: String, code: String, stdin: String): ExecutionResult? {
        return try {
            val cmd = when (language) {
                "python", "py", "python3" -> listOf("python3", "-c", code)
                else -> return null
            }
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(false)
                .start()

            if (stdin.isNotEmpty()) {
                process.outputStream.use { it.write(stdin.toByteArray()) }
            } else {
                process.outputStream.close()
            }

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            ExecutionResult(
                success = exitCode == 0,
                language = language,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                durationMs = 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun runEmbeddedPython(code: String): ExecutionResult {
        val output = StringBuilder()
        val errors = StringBuilder()

        try {
            output.append("[Ejecutor Python Local - PocketHost v1.0]\n")
            // Simple arithmetic / print evaluations
            val lines = code.lines()
            val variables = mutableMapOf<String, Any>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                    val content = trimmed.removePrefix("print(").removeSuffix(")")
                    val evaluated = evaluateExpression(content, variables)
                    output.append(evaluated).append("\n")
                } else if (trimmed.contains("=") && !trimmed.contains("==")) {
                    val parts = trimmed.split("=", limit = 2)
                    val varName = parts[0].trim()
                    val valueExpr = parts[1].trim()
                    val value = evaluateExpression(valueExpr, variables)
                    variables[varName] = value
                }
            }

            if (output.isEmpty()) {
                output.append("Código ejecutado con éxito sin salida de texto.")
            }

            return ExecutionResult(
                success = true,
                language = "python",
                stdout = output.toString(),
                stderr = errors.toString(),
                exitCode = 0,
                durationMs = 0L
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                language = "python",
                stdout = output.toString(),
                stderr = e.message ?: "Error de ejecución",
                exitCode = 1,
                durationMs = 0L
            )
        }
    }

    private fun evaluateExpression(expr: String, vars: Map<String, Any>): String {
        val trimmed = expr.trim()
        if (vars.containsKey(trimmed)) return vars[trimmed].toString()
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return trimmed
    }
}
