package com.hermes.wrapper.ui

import com.hermes.wrapper.data.AppSettings

enum class MainSurface {
    WEB_UI,
    TERMINAL
}

data class MainUiState(
    val settings: AppSettings,
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = true,
    val isSettingsVisible: Boolean = false,
    val pendingShareBanner: String? = null,
    val currentUrl: String = settings.serverUrl,
    val activeSurface: MainSurface = MainSurface.WEB_UI
)
