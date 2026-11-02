package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.file.DefaultProjects
import com.example.file.FileManager
import com.example.server.ServerConfig
import com.example.server.ServerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PocketHost Server", appName)
    }

    @Test
    fun `server state url formatting`() {
        val stateDefault = ServerState(ipAddress = "192.168.1.105", port = 8080)
        assertEquals("http://192.168.1.105:8080", stateDefault.serverUrl)

        val statePort80 = ServerState(ipAddress = "192.168.1.105", port = 80)
        assertEquals("http://192.168.1.105", statePort80.serverUrl)
    }

    @Test
    fun `server state byte formatting`() {
        assertEquals("500 B", ServerState.formatBytes(500))
        assertEquals("1.0 KB", ServerState.formatBytes(1024))
        assertEquals("2.5 MB", ServerState.formatBytes(2621440))
    }

    @Test
    fun `file manager operations and safe directory creation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileManager = FileManager(context)
        val projectDir = fileManager.getProjectDir("test_project")

        assertTrue(projectDir.exists())

        // Create file
        val fileResult = fileManager.createFile(projectDir, "test.html", "<h1>Hello</h1>")
        assertTrue(fileResult.isSuccess)
        val testFile = fileResult.getOrThrow()
        assertTrue(testFile.exists())

        // Read file
        val readResult = fileManager.readFileText(testFile)
        assertTrue(readResult.isSuccess)
        assertEquals("<h1>Hello</h1>", readResult.getOrThrow())

        // List files
        val files = fileManager.listFiles(projectDir)
        assertEquals(1, files.size)
        assertEquals("test.html", files[0].name)
        assertEquals("html", files[0].extension)

        // Delete file
        val deleteResult = fileManager.deleteItem(testFile)
        assertTrue(deleteResult.isSuccess)
        assertFalse(testFile.exists())
    }

    @Test
    fun `default projects template generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileManager = FileManager(context)
        val demoDir = fileManager.getProjectDir("demo_test")

        DefaultProjects.resetDemoProject(demoDir)

        val indexFile = File(demoDir, "index.html")
        val cssFile = File(demoDir, "style.css")
        val jsFile = File(demoDir, "app.js")
        val filesWebFile = File(demoDir, "files_web.html")
        val runnerFile = File(demoDir, "runner.html")

        assertTrue(indexFile.exists())
        assertTrue(cssFile.exists())
        assertTrue(jsFile.exists())
        assertTrue(filesWebFile.exists())
        assertTrue(runnerFile.exists())

        val indexHtml = indexFile.readText()
        assertTrue(indexHtml.contains("PocketHost Server"))
        assertTrue(indexHtml.contains("Archivos"))
        assertTrue(indexHtml.contains("Ejecutar Python"))
    }

    @Test
    fun `code runner executes basic python`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileManager = FileManager(context)
        val demoDir = fileManager.getProjectDir("runner_test")

        val runner = com.example.server.CodeRunner(demoDir)
        val result = kotlinx.coroutines.runBlocking {
            runner.execute("python", "print('PocketHost Test')", autoInstallLibs = false)
        }

        assertNotNull(result)
        assertTrue(result.stdout.contains("PocketHost Test"))
    }
}
