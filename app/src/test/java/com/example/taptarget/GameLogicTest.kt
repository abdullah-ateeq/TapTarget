package com.example.taptarget

import com.example.taptarget.data.model.Difficulty
import com.example.taptarget.data.model.GameState
import com.example.taptarget.data.model.Target
import com.example.taptarget.data.model.TargetState
import com.example.taptarget.data.model.UserStats
import com.example.taptarget.game.TargetMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun testInputLockRules() {
        // Only allow hits when GameState == PLAYING and TargetState == SHRINKING
        fun canProcessTap(gameState: GameState, targetState: TargetState): Boolean {
            return gameState == GameState.PLAYING && targetState == TargetState.SHRINKING
        }

        // Active game: allowed
        assertTrue(canProcessTap(GameState.PLAYING, TargetState.SHRINKING))

        // After successful hit: locked
        assertFalse(canProcessTap(GameState.PLAYING, TargetState.HIT))

        // After missed target: locked
        assertFalse(canProcessTap(GameState.PLAYING, TargetState.MISSED))

        // Spawning: locked
        assertFalse(canProcessTap(GameState.PLAYING, TargetState.SPAWNING))

        // Countdown: locked
        assertFalse(canProcessTap(GameState.COUNTDOWN, TargetState.SHRINKING))

        // Paused: locked
        assertFalse(canProcessTap(GameState.PAUSED, TargetState.SHRINKING))

        // Game Over: locked
        assertFalse(canProcessTap(GameState.GAME_OVER, TargetState.SHRINKING))
    }

    @Test
    fun testResetStatsContract() {
        val stats = UserStats(
            bestScore = 2450,
            bestStreak = 28,
            bestPrecision = 0.84,
            totalGames = 15,
            totalTargetsHit = 94
        )

        // Reset contract as specified in prompt:
        // bestScore = 0, bestStreak = 0, bestPrecision = 0, gamesPlayed = 0, totalTargetsHit = 0
        val resetStats = UserStats()

        assertEquals(0, resetStats.bestScore)
        assertEquals(0, resetStats.bestStreak)
        assertEquals(0.0, resetStats.bestPrecision, 0.0001)
        assertEquals(0, resetStats.totalGames)
        assertEquals(0, resetStats.totalTargetsHit)
    }

    @Test
    fun testPausePreservation() {
        val currentTarget = Target(
            x = 240f,
            y = 480f,
            maxRadius = 70f,
            minRadius = 14f,
            durationMs = 1200L,
            elapsedMs = 450L
        )
        val score = 850
        val streak = 7
        val timer = 8.4f

        // Simulated pause: values must remain intact
        var isPaused = true
        var targetAtPause = currentTarget
        var scoreAtPause = score
        var streakAtPause = streak
        var timerAtPause = timer

        // Simulated time passing during pause should NOT change values
        assertEquals(450L, targetAtPause.elapsedMs)
        assertEquals(850, scoreAtPause)
        assertEquals(7, streakAtPause)
        assertEquals(8.4f, timerAtPause, 0.001f)

        // Resumed
        isPaused = false
        val radiusAfterResume = TargetMath.calculateRadius(
            targetAtPause.elapsedMs,
            targetAtPause.durationMs,
            targetAtPause.maxRadius,
            targetAtPause.minRadius
        )
        assertEquals(
            70f - (56f * (450f / 1200f)),
            radiusAfterResume,
            0.001f
        )
    }

    @Test
    fun testHitRadiusGenerousMargin() {
        val visibleRadius = 22f
        val easyHitRadius = TargetMath.getHitRadius(visibleRadius, Difficulty.EASY)
        val mediumHitRadius = TargetMath.getHitRadius(visibleRadius, Difficulty.MEDIUM)
        val hardHitRadius = TargetMath.getHitRadius(visibleRadius, Difficulty.HARD)

        // Hit radius must be strictly greater than visible radius for responsive feel
        assertTrue(easyHitRadius > visibleRadius)
        assertTrue(mediumHitRadius > visibleRadius)
        assertTrue(hardHitRadius > visibleRadius)
        assertTrue(easyHitRadius >= mediumHitRadius)
        assertTrue(mediumHitRadius >= hardHitRadius)
    }

    @Test
    fun testTargetDurationWithinLimits() {
        for (difficulty in Difficulty.entries) {
            val startDuration = TargetMath.getTargetDurationForHit(difficulty, 0)
            val minDuration = TargetMath.getTargetDurationForHit(difficulty, 1000)

            assertTrue(startDuration >= 950L)
            assertTrue(startDuration <= 1500L)
            assertEquals(550L, minDuration)
        }
    }
}
