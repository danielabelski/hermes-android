package com.hermes.wrapper.ui

import androidx.lifecycle.ViewModel
import com.hermes.wrapper.data.SettingsRepository
import com.hermes.wrapper.data.SharePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val defaultUrl: String,
    private val defaultDashboardTerminalUrl: String
) : ViewModel() {
    private val settings = settingsRepository.getSettings(defaultUrl, defaultDashboardTerminalUrl)

    private val _uiState = MutableStateFlow(MainUiState(settings = settings))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var sharedText: String? = null
    private var sharedFileUris: List<String> = emptyList()

    fun onPageStarted(url: String?) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isOffline = false,
                currentUrl = url ?: it.currentUrl
            )
        }
    }

    fun onPageFinished(url: String?) {
        val next = url ?: _uiState.value.currentUrl
        settingsRepository.saveLastLoadedUrl(next)
        _uiState.update { it.copy(isLoading = false, currentUrl = next) }
    }

    fun onPageError(message: String, isOffline: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
                isOffline = isOffline
            )
        }
    }

    fun consumeSharePayload(payload: SharePayload) {
        sharedText = payload.sharedText?.takeIf { it.isNotBlank() }
        sharedFileUris = payload.fileUris.map { it.toString() }
        val banner = when {
            sharedText != null && sharedFileUris.isNotEmpty() -> "Shared text copied to clipboard, plus ${sharedFileUris.size} file(s) ready"
            sharedText != null -> "Shared text copied to clipboard"
            sharedFileUris.isNotEmpty() -> "${sharedFileUris.size} shared file(s) ready for upload"
            else -> null
        }
        _uiState.update { it.copy(pendingShareBanner = banner) }
    }

    fun consumeSharedText(): String? {
        val value = sharedText
        sharedText = null
        return value
    }

    fun consumeSharedFileUris(): List<String> {
        val staged = sharedFileUris
        sharedFileUris = emptyList()
        return staged
    }

    fun dismissShareBanner() {
        _uiState.update { it.copy(pendingShareBanner = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsVisible = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsVisible = false) }
    }

    fun saveAppUrls(serverUrl: String, dashboardTerminalUrl: String) {
        settingsRepository.saveAppUrls(serverUrl, dashboardTerminalUrl)
        val refreshed = settingsRepository.getSettings(defaultUrl, defaultDashboardTerminalUrl)
        _uiState.update {
            it.copy(
                settings = refreshed,
                currentUrl = refreshed.serverUrl,
                isSettingsVisible = false,
                errorMessage = null,
                isOffline = false
            )
        }
    }

    fun selectSurface(surface: MainSurface, url: String) {
        _uiState.update {
            it.copy(
                activeSurface = surface,
                currentUrl = url,
                isLoading = true,
                errorMessage = null,
                isOffline = false
            )
        }
    }

    fun resetSession() {
        settingsRepository.clearWebSession()
    }
}
