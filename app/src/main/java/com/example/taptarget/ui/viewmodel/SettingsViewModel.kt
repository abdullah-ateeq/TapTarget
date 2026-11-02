package com.example.taptarget.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.taptarget.data.model.AppTheme
import com.example.taptarget.data.model.GameSettings
import com.example.taptarget.data.repository.GamePreferencesRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GamePreferencesRepository.getInstance(application)

    val settings: StateFlow<GameSettings> = repository.settings

    fun setTheme(theme: AppTheme) {
        repository.updateTheme(theme)
    }

    fun setSoundEnabled(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        repository.setHapticsEnabled(enabled)
    }

    fun setReducedMotion(enabled: Boolean) {
        repository.setReducedMotion(enabled)
    }

    fun resetStatistics() {
        repository.resetStatistics()
    }
}
