package com.hermeswebui.android.data

import android.content.Context
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getSettings(defaultUrl: String, defaultDashboardTerminalUrl: String): AppSettings {
        val serverUrl = sharedPreferences.getString(KEY_SERVER_URL, defaultUrl)?.trim().orEmpty()
        val dashboardTerminalUrl = sharedPreferences
            .getString(KEY_DASHBOARD_TERMINAL_URL, defaultDashboardTerminalUrl)
            ?.trim()
            .orEmpty()
        val parsedHosts = setOf(serverUrl, dashboardTerminalUrl)
            .mapNotNull { url -> runCatching { url.toUri().host.orEmpty().lowercase() }.getOrNull() }
            .filter { it.isNotBlank() }
            .toSet()
        val hostCsv = sharedPreferences.getString(KEY_ALLOWED_HOSTS, parsedHosts.joinToString(",")).orEmpty()
        val isConfigured = sharedPreferences.getBoolean(KEY_IS_CONFIGURED, false)
        val allowlist = hostCsv
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        return AppSettings(
            serverUrl = serverUrl,
            dashboardTerminalUrl = dashboardTerminalUrl,
            allowedHosts = allowlist,
            isConfigured = isConfigured
        )
    }

    fun saveAppUrls(serverUrl: String, dashboardTerminalUrl: String) {
        val hosts = setOf(serverUrl, dashboardTerminalUrl)
            .mapNotNull { url -> runCatching { url.toUri().host.orEmpty().lowercase() }.getOrNull() }
            .filter { it.isNotBlank() }
            .toSet()
        sharedPreferences.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_DASHBOARD_TERMINAL_URL, dashboardTerminalUrl)
            .putString(KEY_ALLOWED_HOSTS, hosts.joinToString(","))
            .putBoolean(KEY_IS_CONFIGURED, true)
            .apply()
    }

    fun clearWebSession() {
        sharedPreferences.edit().remove(KEY_LAST_URL).apply()
    }

    fun saveLastLoadedUrl(url: String) {
        sharedPreferences.edit().putString(KEY_LAST_URL, url).apply()
    }

    fun getLastLoadedUrl(): String? = sharedPreferences.getString(KEY_LAST_URL, null)

    companion object {
        private const val FILE_NAME = "hermes_secure_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DASHBOARD_TERMINAL_URL = "dashboard_terminal_url"
        private const val KEY_ALLOWED_HOSTS = "allowed_hosts"
        private const val KEY_LAST_URL = "last_url"
        private const val KEY_IS_CONFIGURED = "is_configured"
    }
}
