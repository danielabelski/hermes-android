package com.hermes.wrapper.data

data class AppSettings(
    val serverUrl: String,
    val dashboardTerminalUrl: String,
    val allowedHosts: Set<String>,
    val isConfigured: Boolean
)
