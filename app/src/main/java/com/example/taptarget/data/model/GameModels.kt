package com.example.taptarget.data.model

enum class TargetState {
    SPAWNING,
    SHRINKING,
    HIT,
    MISSED
}

enum class GameState {
    READY,
    COUNTDOWN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class GameOverReason {
    MISSED_TARGET,
    TIMEOUT
}

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class Target(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val minRadius: Float,
    val durationMs: Long,
    val elapsedMs: Long = 0L
)

data class TapTargetResult(
    val difficulty: Difficulty,
    val score: Int,
    val targetsHit: Int,
    val bestStreak: Int,
    val currentStreak: Int = 0,
    val precision: Double,
    val reason: GameOverReason,
    val isNewBestScore: Boolean = false,
    val isNewBestStreak: Boolean = false,
    val isNewBestPrecision: Boolean = false
)

data class UserStats(
    val bestScore: Int = 0,
    val bestStreak: Int = 0,
    val bestPrecision: Double = 0.0,
    val totalGames: Int = 0,
    val totalTargetsHit: Int = 0
)

data class GameSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false
)

data class HitFeedback(
    val x: Float,
    val y: Float,
    val text: String,
    val timestamp: Long
)
