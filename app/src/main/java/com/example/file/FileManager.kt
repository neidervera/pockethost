package com.example.file

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class FileItem(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String
) {
    val formattedSize: String
        get() = if (isDirectory) "--" else formatBytes(size)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            val pre = "KMGTPE"[exp - 1]
            return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
        }
    }
}

class FileManager(private val context: Context) {

    val rootSitesDir: File
        get() {
            val dir = File(context.filesDir, "hosted_sites")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getProjectDir(projectName: String = "default"): File {
        val dir = File(rootSitesDir, projectName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listProjects(): List<String> {
        val projects = rootSitesDir.listFiles { f -> f.isDirectory }
        return projects?.map { it.name }?.sorted() ?: listOf("default")
    }

    fun listFiles(dir: File): List<FileItem> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val files = dir.listFiles() ?: return emptyList()
        return files.map { f ->
            FileItem(
                file = f,
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                size = if (f.isDirectory) calculateDirectorySize(f) else f.length(),
                lastModified = f.lastModified(),
                extension = f.extension.lowercase()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (!dir.isDirectory) return dir.length()
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    fun createFile(parentDir: File, name: String, initialContent: String = ""): Result<File> {
        return try {
            val newFile = File(parentDir, name)
            if (newFile.exists()) {
                Result.failure(IllegalArgumentException("El archivo ya existe"))
            } else {
                newFile.parentFile?.mkdirs()
                newFile.writeText(initialContent, Charsets.UTF_8)
                Result.success(newFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createFolder(parentDir: File, name: String): Result<File> {
        return try {
            val newDir = File(parentDir, name)
            if (newDir.exists()) {
                Result.failure(IllegalArgumentException("La carpeta ya existe"))
            } else {
                newDir.mkdirs()
                Result.success(newDir)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readFileText(file: File): Result<String> {
        return try {
            if (file.length() > 5 * 1024 * 1024) { // 5MB limit for editor
                Result.failure(IllegalStateException("El archivo es demasiado grande para editar (> 5MB)"))
            } else {
                Result.success(file.readText(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeFileText(file: File, content: String): Result<Unit> {
        return try {
            file.writeText(content, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun renameItem(file: File, newName: String): Result<File> {
        return try {
            val target = File(file.parentFile, newName)
            if (target.exists()) {
                Result.failure(IllegalArgumentException("Ya existe un elemento con ese nombre"))
            } else if (file.renameTo(target)) {
                Result.success(target)
            } else {
                Result.failure(IllegalStateException("No se pudo renombrar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteItem(file: File): Result<Unit> {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Safely unzips an InputStream into target directory preventing Path Traversal (Zip Slip).
     */
    fun unzipStream(inputStream: InputStream, targetDir: File): Result<Int> {
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val canonicalDestDir = targetDir.canonicalPath
            var extractedCount = 0

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    val canonicalPath = file.canonicalPath
                    if (!canonicalPath.startsWith(canonicalDestDir)) {
                        throw SecurityException("Zip Slip detectado: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                        extractedCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses the project directory into a ZIP file in cache for sharing.
     */
    fun zipDirectory(sourceDir: File, zipFile: File): Result<File> {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val sourcePath = sourceDir.canonicalPath
                sourceDir.walkTopDown().forEach { file ->
                    val relativePath = file.canonicalPath.removePrefix(sourcePath).trimStart(File.separatorChar)
                    if (relativePath.isNotEmpty()) {
                        if (file.isDirectory) {
                            zos.putNextEntry(ZipEntry("$relativePath/"))
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(ZipEntry(relativePath))
                            FileInputStream(file).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
