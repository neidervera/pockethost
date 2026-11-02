package com.example.server

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServerConfig(
    val port: Int = 8080,
    val isBasicAuthEnabled: Boolean = false,
    val authUsername: String = "admin",
    val authPassword: String = "admin123",
    val isDirectoryListingEnabled: Boolean = true,
    val isFileUploadAllowed: Boolean = true,
    val isDynamicTemplateEnabled: Boolean = true,
    val blockedIps: Set<String> = emptySet(),
    val selectedProjectFolder: String = "default"
)

class ServerConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pockethost_settings", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ServerConfig> = _config.asStateFlow()

    private fun loadConfig(): ServerConfig {
        return ServerConfig(
            port = prefs.getInt("port", 8080),
            isBasicAuthEnabled = prefs.getBoolean("auth_enabled", false),
            authUsername = prefs.getString("auth_user", "admin") ?: "admin",
            authPassword = prefs.getString("auth_pass", "admin123") ?: "admin123",
            isDirectoryListingEnabled = prefs.getBoolean("dir_listing", true),
            isFileUploadAllowed = prefs.getBoolean("allow_upload", true),
            isDynamicTemplateEnabled = prefs.getBoolean("dynamic_template", true),
            blockedIps = prefs.getStringSet("blocked_ips", emptySet()) ?: emptySet(),
            selectedProjectFolder = prefs.getString("project_folder", "default") ?: "default"
        )
    }

    fun updatePort(port: Int) {
        prefs.edit().putInt("port", port).apply()
        _config.value = _config.value.copy(port = port)
    }

    fun updateBasicAuth(enabled: Boolean, username: String, pass: String) {
        prefs.edit()
            .putBoolean("auth_enabled", enabled)
            .putString("auth_user", username)
            .putString("auth_pass", pass)
            .apply()
        _config.value = _config.value.copy(
            isBasicAuthEnabled = enabled,
            authUsername = username,
            authPassword = pass
        )
    }

    fun updateDirectoryListing(enabled: Boolean) {
        prefs.edit().putBoolean("dir_listing", enabled).apply()
        _config.value = _config.value.copy(isDirectoryListingEnabled = enabled)
    }

    fun updateFileUpload(allowed: Boolean) {
        prefs.edit().putBoolean("allow_upload", allowed).apply()
        _config.value = _config.value.copy(isFileUploadAllowed = allowed)
    }

    fun updateDynamicTemplate(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_template", enabled).apply()
        _config.value = _config.value.copy(isDynamicTemplateEnabled = enabled)
    }

    fun blockIp(ip: String) {
        val newSet = _config.value.blockedIps + ip
        prefs.edit().putStringSet("blocked_ips", newSet).apply()
        _config.value = _config.value.copy(blockedIps = newSet)
    }

    fun unblockIp(ip: String) {
        val newSet = _config.value.blockedIps - ip
        prefs.edit().putStringSet("blocked_ips", newSet).apply()
        _config.value = _config.value.copy(blockedIps = newSet)
    }

    fun setProjectFolder(folderName: String) {
        prefs.edit().putString("project_folder", folderName).apply()
        _config.value = _config.value.copy(selectedProjectFolder = folderName)
    }
}
