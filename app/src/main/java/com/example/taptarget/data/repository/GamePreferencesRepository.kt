package com.example.taptarget.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.taptarget.data.model.AppTheme
import com.example.taptarget.data.model.Difficulty
import com.example.taptarget.data.model.GameSettings
import com.example.taptarget.data.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamePreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _stats = MutableStateFlow(loadStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(loadDifficulty())
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty.asStateFlow()

    private fun loadStats(): UserStats {
        return UserStats(
            bestScore = prefs.getInt(KEY_BEST_SCORE, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            bestPrecision = prefs.getFloat(KEY_BEST_PRECISION, 0f).toDouble(),
            totalGames = prefs.getInt(KEY_TOTAL_GAMES, 0),
            totalTargetsHit = prefs.getInt(KEY_TOTAL_TARGETS_HIT, 0)
        )
    }

    private fun loadSettings(): GameSettings {
        val themeStr = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.SYSTEM)
        val sound = prefs.getBoolean(KEY_SOUND, true)
        val haptics = prefs.getBoolean(KEY_HAPTICS, true)
        val reducedMotion = prefs.getBoolean(KEY_REDUCED_MOTION, false)
        return GameSettings(
            theme = theme,
            soundEnabled = sound,
            hapticsEnabled = haptics,
            reducedMotion = reducedMotion
        )
    }

    private fun loadDifficulty(): Difficulty {
        val diffStr = prefs.getString(KEY_DIFFICULTY, Difficulty.EASY.name) ?: Difficulty.EASY.name
        return runCatching { Difficulty.valueOf(diffStr) }.getOrDefault(Difficulty.EASY)
    }

    fun saveDifficulty(difficulty: Difficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.name).apply()
        _selectedDifficulty.value = difficulty
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _settings.value = _settings.value.copy(hapticsEnabled = enabled)
    }

    fun setReducedMotion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        _settings.value = _settings.value.copy(reducedMotion = enabled)
    }

    /**
     * Records a finished game session and updates best records.
     * Returns a Triple of (isNewBestScore, isNewBestStreak, isNewBestPrecision).
     */
    fun recordGameResult(
        score: Int,
        streak: Int,
        precision: Double,
        targetsHit: Int
    ): Triple<Boolean, Boolean, Boolean> {
        val current = _stats.value
        val isNewBestScore = score > current.bestScore
        val isNewBestStreak = streak > current.bestStreak
        val isNewBestPrecision = targetsHit >= 5 && precision > current.bestPrecision

        val newBestScore = if (isNewBestScore) score else current.bestScore
        val newBestStreak = if (isNewBestStreak) streak else current.bestStreak
        val newBestPrecision = if (isNewBestPrecision) precision else current.bestPrecision
        val newTotalGames = current.totalGames + 1
        val newTotalTargetsHit = current.totalTargetsHit + targetsHit

        prefs.edit()
            .putInt(KEY_BEST_SCORE, newBestScore)
            .putInt(KEY_BEST_STREAK, newBestStreak)
            .putFloat(KEY_BEST_PRECISION, newBestPrecision.toFloat())
            .putInt(KEY_TOTAL_GAMES, newTotalGames)
            .putInt(KEY_TOTAL_TARGETS_HIT, newTotalTargetsHit)
            .apply()

        _stats.value = UserStats(
            bestScore = newBestScore,
            bestStreak = newBestStreak,
            bestPrecision = newBestPrecision,
            totalGames = newTotalGames,
            totalTargetsHit = newTotalTargetsHit
        )

        return Triple(isNewBestScore, isNewBestStreak, isNewBestPrecision)
    }

    fun resetStatistics() {
        prefs.edit()
            .putInt(KEY_BEST_SCORE, 0)
            .putInt(KEY_BEST_STREAK, 0)
            .putFloat(KEY_BEST_PRECISION, 0f)
            .putInt(KEY_TOTAL_GAMES, 0)
            .putInt(KEY_TOTAL_TARGETS_HIT, 0)
            .apply()

        _stats.value = UserStats()
    }

    companion object {
        private const val PREFS_NAME = "taptarget_preferences"
        private const val KEY_BEST_SCORE = "key_best_score"
        private const val KEY_BEST_STREAK = "key_best_streak"
        private const val KEY_BEST_PRECISION = "key_best_precision"
        private const val KEY_TOTAL_GAMES = "key_total_games"
        private const val KEY_TOTAL_TARGETS_HIT = "key_total_targets_hit"
        private const val KEY_THEME = "key_theme"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_HAPTICS = "key_haptics"
        private const val KEY_REDUCED_MOTION = "key_reduced_motion"
        private const val KEY_DIFFICULTY = "key_difficulty"

        @Volatile
        private var INSTANCE: GamePreferencesRepository? = null

        fun getInstance(context: Context): GamePreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamePreferencesRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
