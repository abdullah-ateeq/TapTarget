package com.example.taptarget.ui.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptarget.audio.HapticManager
import com.example.taptarget.audio.SoundManager
import com.example.taptarget.data.model.Difficulty
import com.example.taptarget.data.model.GameOverReason
import com.example.taptarget.data.model.GameState
import com.example.taptarget.data.model.HitFeedback
import com.example.taptarget.data.model.TapTargetResult
import com.example.taptarget.data.model.Target
import com.example.taptarget.data.model.TargetState
import com.example.taptarget.data.model.UserStats
import com.example.taptarget.data.repository.GamePreferencesRepository
import com.example.taptarget.game.TargetMath
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class GameUiState(
    val gameState: GameState = GameState.READY,
    val targetState: TargetState = TargetState.SPAWNING,
    val difficulty: Difficulty = Difficulty.EASY,
    val currentTarget: Target? = null,
    val currentRadiusDp: Float = 70f,
    val remainingGlobalTimeSec: Float = 12f,
    val maxGlobalTimeSec: Float = 15f,
    val score: Int = 0,
    val streak: Int = 0,
    val bestStreakInSession: Int = 0,
    val targetsHit: Int = 0,
    val accumulatedPrecision: Double = 0.0,
    val currentPrecisionPct: Int = 0,
    val countdownNumber: Int? = null,
    val countdownText: String? = null,
    val hitFeedback: HitFeedback? = null,
    val activeMilestone: String? = null,
    val lastResult: TapTargetResult? = null,
    val gameOverReason: GameOverReason? = null,
    val hitBurstProgress: Float = 0f,
    val missRingProgress: Float = 0f
)

class TapTargetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GamePreferencesRepository.getInstance(application)
    val soundManager = SoundManager()
    val hapticManager = HapticManager(application)

    val stats: StateFlow<UserStats> = repository.stats
    val settings = repository.settings
    val selectedDifficulty = repository.selectedDifficulty

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null
    private var countdownJob: Job? = null
    private var transitionJob: Job? = null

    // Playable bounds in DP set by the Game Canvas
    private var canvasWidthDp: Float = 360f
    private var canvasHeightDp: Float = 640f
    private var topMarginDp: Float = 90f
    private var bottomMarginDp: Float = 40f
    private var sideMarginDp: Float = 24f

    private var lastTickingSecond: Int = -1

    init {
        // Sync audio/haptic preferences
        viewModelScope.launch {
            settings.collect { s ->
                soundManager.isEnabled = s.soundEnabled
                hapticManager.isEnabled = s.hapticsEnabled
            }
        }
    }

    fun setPlayableBounds(widthDp: Float, heightDp: Float, topMargin: Float, bottomMargin: Float, sideMargin: Float) {
        if (widthDp > 0 && heightDp > 0) {
            canvasWidthDp = widthDp
            canvasHeightDp = heightDp
            topMarginDp = topMargin
            bottomMarginDp = bottomMargin
            sideMarginDp = sideMargin
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        repository.saveDifficulty(difficulty)
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    /**
     * Starts a new game session with 3-2-1-GO countdown.
     */
    fun startGame(difficulty: Difficulty = _uiState.value.difficulty) {
        stopAllJobs()
        val initialTime = TargetMath.getInitialTimerSeconds(difficulty)
        val maxTime = TargetMath.getMaxTimerSeconds(difficulty)

        _uiState.value = GameUiState(
            gameState = GameState.COUNTDOWN,
            targetState = TargetState.SPAWNING,
            difficulty = difficulty,
            remainingGlobalTimeSec = initialTime,
            maxGlobalTimeSec = maxTime,
            score = 0,
            streak = 0,
            bestStreakInSession = 0,
            targetsHit = 0,
            accumulatedPrecision = 0.0,
            currentPrecisionPct = 0,
            countdownNumber = 3,
            countdownText = "3"
        )

        countdownJob = viewModelScope.launch {
            soundManager.playCountdown(isGo = false)
            delay(700L)

            _uiState.update { it.copy(countdownNumber = 2, countdownText = "2") }
            soundManager.playCountdown(isGo = false)
            delay(700L)

            _uiState.update { it.copy(countdownNumber = 1, countdownText = "1") }
            soundManager.playCountdown(isGo = false)
            delay(700L)

            _uiState.update { it.copy(countdownNumber = 0, countdownText = "GO!") }
            soundManager.playCountdown(isGo = true)
            delay(450L)

            _uiState.update {
                it.copy(
                    gameState = GameState.PLAYING,
                    countdownNumber = null,
                    countdownText = null
                )
            }

            spawnNewTarget()
            startGameLoop()
        }
    }

    private fun spawnNewTarget() {
        val state = _uiState.value
        val duration = TargetMath.getTargetDurationForHit(state.difficulty, state.targetsHit)
        val startRadius = TargetMath.getTargetStartRadius(state.difficulty, state.targetsHit)
        val minRadius = TargetMath.getTargetMinRadius()

        val pos = TargetMath.generateTargetPosition(
            width = canvasWidthDp,
            height = canvasHeightDp,
            radius = startRadius,
            previousX = state.currentTarget?.x,
            previousY = state.currentTarget?.y,
            topMargin = topMarginDp,
            bottomMargin = bottomMarginDp,
            leftMargin = sideMarginDp,
            rightMargin = sideMarginDp
        )

        val newTarget = Target(
            x = pos.x,
            y = pos.y,
            maxRadius = startRadius,
            minRadius = minRadius,
            durationMs = duration,
            elapsedMs = 0L
        )

        _uiState.update {
            it.copy(
                currentTarget = newTarget,
                targetState = TargetState.SHRINKING,
                currentRadiusDp = startRadius,
                hitBurstProgress = 0f,
                missRingProgress = 0f
            )
        }
        soundManager.playSpawn()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (_uiState.value.gameState == GameState.PLAYING) {
                val currentTime = System.currentTimeMillis()
                val dt = (currentTime - lastTime).coerceAtLeast(0L)
                lastTime = currentTime

                val currentState = _uiState.value
                val target = currentState.currentTarget

                // 1. Target Shrink Animation & State
                if (target != null && currentState.targetState == TargetState.SHRINKING) {
                    val newElapsed = target.elapsedMs + dt
                    val newRadius = TargetMath.calculateRadius(
                        newElapsed,
                        target.durationMs,
                        target.maxRadius,
                        target.minRadius
                    )

                    if (newElapsed >= target.durationMs || newRadius <= target.minRadius) {
                        // Missed target! Minimum size reached
                        handleMiss()
                        return@launch
                    } else {
                        _uiState.update {
                            it.copy(
                                currentTarget = target.copy(elapsedMs = newElapsed),
                                currentRadiusDp = newRadius
                            )
                        }
                    }
                }

                // 2. Global Survival Timer
                val dtSec = dt / 1000f
                val newGlobalTime = (currentState.remainingGlobalTimeSec - dtSec).coerceAtLeast(0f)

                // Timer warning audio / tick when <= 3.0s
                if (newGlobalTime <= 3.0f && newGlobalTime > 0f) {
                    val currentSecInt = newGlobalTime.toInt()
                    if (currentSecInt != lastTickingSecond) {
                        lastTickingSecond = currentSecInt
                        soundManager.playTimerWarning()
                    }
                }

                if (newGlobalTime <= 0f) {
                    // Time's up!
                    handleTimeout()
                    return@launch
                } else {
                    _uiState.update { it.copy(remainingGlobalTimeSec = newGlobalTime) }
                }

                delay(16L) // ~60 FPS update
            }
        }
    }

    /**
     * Handles tap input on the playfield.
     * Enforces strict input locking: rejects tap if not in SHRINKING state or not PLAYING.
     */
    fun onPlayfieldTap(tapXDp: Float, tapYDp: Float) {
        val state = _uiState.value
        // Input lock: ignore all taps if not active
        if (state.gameState != GameState.PLAYING || state.targetState != TargetState.SHRINKING) {
            return
        }

        val target = state.currentTarget ?: return
        val hitRadius = TargetMath.getHitRadius(state.currentRadiusDp, state.difficulty)
        val isHit = TargetMath.isHit(tapXDp, tapYDp, target.x, target.y, hitRadius)

        if (isHit) {
            handleHit(target, tapXDp, tapYDp)
        }
    }

    private fun handleHit(target: Target, tapXDp: Float, tapYDp: Float) {
        // Immediate input lock
        _uiState.update { it.copy(targetState = TargetState.HIT) }

        val state = _uiState.value
        val precision = TargetMath.calculatePrecision(target.elapsedMs, target.durationMs)
        val newStreak = state.streak + 1
        val newBestStreak = kotlin.math.max(state.bestStreakInSession, newStreak)
        val newTargetsHit = state.targetsHit + 1
        val newAccumPrecision = state.accumulatedPrecision + precision
        val avgPrecisionPct = ((newAccumPrecision / newTargetsHit) * 100.0).roundToInt()

        val points = TargetMath.calculateScore(
            baseScore = 100,
            precision = precision,
            streak = newStreak
        )
        val newScore = state.score + points

        val timeBonus = TargetMath.getTimeBonus(state.difficulty)
        val newTimer = (state.remainingGlobalTimeSec + timeBonus).coerceAtMost(state.maxGlobalTimeSec)

        val milestone = TargetMath.getStreakMilestoneMessage(newStreak)

        _uiState.update {
            it.copy(
                score = newScore,
                streak = newStreak,
                bestStreakInSession = newBestStreak,
                targetsHit = newTargetsHit,
                accumulatedPrecision = newAccumPrecision,
                currentPrecisionPct = avgPrecisionPct,
                remainingGlobalTimeSec = newTimer,
                activeMilestone = milestone,
                hitFeedback = HitFeedback(x = target.x, y = target.y, text = "+$points", timestamp = System.currentTimeMillis()),
                hitBurstProgress = 0f
            )
        }

        soundManager.playHit()
        hapticManager.playHit()

        if (milestone != null) {
            soundManager.playMilestone()
            hapticManager.playMilestone()
        }

        // Quick burst animation and immediate spawn
        transitionJob?.cancel()
        transitionJob = viewModelScope.launch {
            // Animate burst: 150-180ms
            val burstDuration = 160L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < burstDuration) {
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / burstDuration).coerceIn(0f, 1f)
                _uiState.update { it.copy(hitBurstProgress = progress) }
                delay(16L)
            }
            _uiState.update { it.copy(hitBurstProgress = 1f) }

            // Clear milestone banner after brief presentation
            if (milestone != null) {
                delay(200L)
                _uiState.update { it.copy(activeMilestone = null) }
            }

            if (_uiState.value.gameState == GameState.PLAYING) {
                spawnNewTarget()
            }
        }
    }

    private fun handleMiss() {
        stopAllJobs()
        _uiState.update {
            it.copy(
                targetState = TargetState.MISSED,
                gameOverReason = GameOverReason.MISSED_TARGET
            )
        }

        soundManager.playMiss()
        hapticManager.playMiss()

        // Short miss animation (expanding ring, fade out)
        viewModelScope.launch {
            val missAnimDuration = 400L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < missAnimDuration) {
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / missAnimDuration).coerceIn(0f, 1f)
                _uiState.update { it.copy(missRingProgress = progress) }
                delay(16L)
            }
            finalizeGameOver(GameOverReason.MISSED_TARGET)
        }
    }

    private fun handleTimeout() {
        stopAllJobs()
        _uiState.update {
            it.copy(
                targetState = TargetState.MISSED,
                gameOverReason = GameOverReason.TIMEOUT
            )
        }

        soundManager.playGameOver()
        hapticManager.playGameOver()

        viewModelScope.launch {
            delay(350L)
            finalizeGameOver(GameOverReason.TIMEOUT)
        }
    }

    private fun finalizeGameOver(reason: GameOverReason) {
        val s = _uiState.value
        val precision = if (s.targetsHit > 0) s.accumulatedPrecision / s.targetsHit else 0.0

        val (isNewScore, isNewStreak, isNewPrecision) = repository.recordGameResult(
            score = s.score,
            streak = s.bestStreakInSession,
            precision = precision,
            targetsHit = s.targetsHit
        )

        val result = TapTargetResult(
            difficulty = s.difficulty,
            score = s.score,
            targetsHit = s.targetsHit,
            bestStreak = s.bestStreakInSession,
            currentStreak = 0,
            precision = precision,
            reason = reason,
            isNewBestScore = isNewScore,
            isNewBestStreak = isNewStreak,
            isNewBestPrecision = isNewPrecision
        )

        _uiState.update {
            it.copy(
                gameState = GameState.GAME_OVER,
                lastResult = result
            )
        }
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.PLAYING) {
            gameLoopJob?.cancel()
            _uiState.update { it.copy(gameState = GameState.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.update { it.copy(gameState = GameState.PLAYING) }
            startGameLoop()
        }
    }

    fun restartGame() {
        startGame(_uiState.value.difficulty)
    }

    fun exitToHome() {
        stopAllJobs()
        _uiState.update { it.copy(gameState = GameState.READY) }
    }

    fun resetStatistics() {
        repository.resetStatistics()
    }

    private fun stopAllJobs() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        countdownJob?.cancel()
        countdownJob = null
        transitionJob?.cancel()
        transitionJob = null
        lastTickingSecond = -1
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
    }
}
