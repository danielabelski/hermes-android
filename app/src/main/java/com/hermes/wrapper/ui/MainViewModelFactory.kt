package com.hermes.wrapper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hermes.wrapper.data.SettingsRepository

class MainViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val defaultUrl: String,
    private val defaultDashboardTerminalUrl: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(settingsRepository, defaultUrl, defaultDashboardTerminalUrl) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
