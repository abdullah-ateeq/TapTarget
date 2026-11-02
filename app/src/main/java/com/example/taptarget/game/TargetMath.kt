package com.example.taptarget.game

import androidx.compose.ui.geometry.Offset
import com.example.taptarget.data.model.Difficulty
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object TargetMath {

    /**
     * Calculates the target radius based on elapsed time and configured bounds.
     * Progress is clamped to [0.0, 1.0].
     */
    fun calculateRadius(
        elapsedMs: Long,
        durationMs: Long,
        maxRadius: Float,
        minRadius: Float
    ): Float {
        if (durationMs <= 0L) return minRadius
        val progress = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        return maxRadius - ((maxRadius - minRadius) * progress)
    }

    /**
     * Calculates Euclidean distance to determine if tap coordinates fall within hitRadius.
     */
    fun isHit(
        tapX: Float,
        tapY: Float,
        targetX: Float,
        targetY: Float,
        hitRadius: Float
    ): Boolean {
        val distance = hypot(tapX - targetX, tapY - targetY)
        return distance <= hitRadius
    }

    /**
     * Generates a safe target position within playable bounds.
     * Avoids spawning too close to the previous position (minimumDistance = targetRadius * 2).
     */
    fun generateTargetPosition(
        width: Float,
        height: Float,
        radius: Float,
        previousX: Float? = null,
        previousY: Float? = null,
        topMargin: Float = 0f,
        bottomMargin: Float = 0f,
        leftMargin: Float = 0f,
        rightMargin: Float = 0f,
        random: Random = Random.Default
    ): Offset {
        val minX = leftMargin + radius
        val maxX = width - rightMargin - radius
        val minY = topMargin + radius
        val maxY = height - bottomMargin - radius

        if (maxX <= minX || maxY <= minY) {
            // Screen is too small or margin is too large; fallback to center
            return Offset(width / 2f, (topMargin + height - bottomMargin) / 2f)
        }

        val minDistance = radius * 2f
        var bestX = minX + random.nextFloat() * (maxX - minX)
        var bestY = minY + random.nextFloat() * (maxY - minY)

        if (previousX != null && previousY != null) {
            for (attempt in 0 until 15) {
                val candidateX = minX + random.nextFloat() * (maxX - minX)
                val candidateY = minY + random.nextFloat() * (maxY - minY)
                val dist = hypot(candidateX - previousX, candidateY - previousY)
                if (dist >= minDistance) {
                    return Offset(candidateX, candidateY)
                }
                bestX = candidateX
                bestY = candidateY
            }
        }

        return Offset(bestX, bestY)
    }

    /**
     * Calculates precision ratio from 0.0 (late/min size) to 1.0 (immediate tap/max size).
     */
    fun calculatePrecision(elapsedMs: Long, durationMs: Long): Double {
        if (durationMs <= 0L) return 0.0
        val ratio = 1.0 - (elapsedMs.toDouble() / durationMs.toDouble())
        return ratio.coerceIn(0.0, 1.0)
    }

    /**
     * Multiplier based on current streak: 1 + min(streak - 1, 20) * 0.05.
     * Maximum multiplier is 2.0x.
     */
    fun getStreakMultiplier(streak: Int): Double {
        if (streak <= 1) return 1.0
        val capped = min(streak - 1, 20)
        return 1.0 + (capped * 0.05)
    }

    /**
     * Calculates score awarded for a hit.
     * Base: 100 points
     * Precision bonus: precision * 100
     * Multiplier applied to base score.
     */
    fun calculateScore(baseScore: Int = 100, precision: Double, streak: Int): Int {
        val multiplier = getStreakMultiplier(streak)
        val precisionBonus = (precision * 100.0).toInt()
        val scaledBase = (baseScore * multiplier).toInt()
        return scaledBase + precisionBonus
    }

    /**
     * Streak milestone celebratory text.
     */
    fun getStreakMilestoneMessage(streak: Int): String? {
        return when (streak) {
            5 -> "NICE!"
            10 -> "GREAT!"
            20 -> "ON FIRE!"
            30 -> "UNSTOPPABLE!"
            50 -> "TAP MASTER!"
            else -> null
        }
    }

    /**
     * Initial target duration by difficulty in milliseconds.
     */
    fun getInitialTargetDuration(difficulty: Difficulty): Long {
        return when (difficulty) {
            Difficulty.EASY -> 1500L
            Difficulty.MEDIUM -> 1200L
            Difficulty.HARD -> 950L
        }
    }

    /**
     * Target duration progression based on hits completed.
     * Every 3 hits: duration -= 80ms, minimum 550ms.
     */
    fun getTargetDurationForHit(difficulty: Difficulty, hitsCompleted: Int): Long {
        val baseDuration = getInitialTargetDuration(difficulty)
        val reductions = hitsCompleted / 3
        val reductionMs = reductions * 80L
        val minDuration = 550L
        return max(minDuration, baseDuration - reductionMs)
    }

    /**
     * Target start radius (in dp) based on difficulty and progression.
     */
    fun getTargetStartRadius(difficulty: Difficulty, hitsCompleted: Int): Float {
        val baseRadius = when (difficulty) {
            Difficulty.EASY -> 70f
            Difficulty.MEDIUM -> 62f
            Difficulty.HARD -> 54f
        }
        val shrinkStep = (hitsCompleted / 5) * 2f
        return max(42f, baseRadius - shrinkStep)
    }

    fun getTargetMinRadius(): Float = 14f

    /**
     * Hit radius multiplier (generous hitbox).
     */
    fun getHitRadius(visibleRadius: Float, difficulty: Difficulty): Float {
        val factor = when (difficulty) {
            Difficulty.EASY -> 1.35f
            Difficulty.MEDIUM -> 1.25f
            Difficulty.HARD -> 1.15f
        }
        return visibleRadius * factor
    }

    /**
     * Global survival timer starting time in seconds.
     */
    fun getInitialTimerSeconds(difficulty: Difficulty): Float {
        return when (difficulty) {
            Difficulty.EASY -> 12f
            Difficulty.MEDIUM -> 10f
            Difficulty.HARD -> 8f
        }
    }

    /**
     * Time bonus added per hit in seconds.
     */
    fun getTimeBonus(difficulty: Difficulty): Float {
        return when (difficulty) {
            Difficulty.EASY -> 0.8f
            Difficulty.MEDIUM -> 0.7f
            Difficulty.HARD -> 0.6f
        }
    }

    /**
     * Maximum timer cap in seconds.
     */
    fun getMaxTimerSeconds(difficulty: Difficulty): Float {
        return when (difficulty) {
            Difficulty.EASY -> 15f
            Difficulty.MEDIUM -> 13f
            Difficulty.HARD -> 11f
        }
    }
}
