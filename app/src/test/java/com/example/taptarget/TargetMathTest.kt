package com.example.taptarget

import androidx.compose.ui.geometry.Offset
import com.example.taptarget.data.model.Difficulty
import com.example.taptarget.game.TargetMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TargetMathTest {

    @Test
    fun testCalculateRadius_formulaVerification() {
        // As per prompt test specification:
        // maxRadius = 70, minRadius = 14, duration = 1000ms
        // elapsed = 0 -> 70
        // elapsed = 500 -> 42
        // elapsed = 1000 -> 14
        val maxRadius = 70f
        val minRadius = 14f
        val durationMs = 1000L

        val radiusAt0 = TargetMath.calculateRadius(0L, durationMs, maxRadius, minRadius)
        assertEquals(70f, radiusAt0, 0.001f)

        val radiusAt500 = TargetMath.calculateRadius(500L, durationMs, maxRadius, minRadius)
        assertEquals(42f, radiusAt500, 0.001f)

        val radiusAt1000 = TargetMath.calculateRadius(1000L, durationMs, maxRadius, minRadius)
        assertEquals(14f, radiusAt1000, 0.001f)
    }

    @Test
    fun testCalculateRadius_clamping() {
        val maxRadius = 70f
        val minRadius = 14f
        val durationMs = 1000L

        // Over duration should clamp to minRadius
        val radiusOver = TargetMath.calculateRadius(1500L, durationMs, maxRadius, minRadius)
        assertEquals(14f, radiusOver, 0.001f)

        // Negative elapsed should clamp to maxRadius
        val radiusUnder = TargetMath.calculateRadius(-100L, durationMs, maxRadius, minRadius)
        assertEquals(70f, radiusUnder, 0.001f)
    }

    @Test
    fun testHitDetection_exactCases() {
        // Target: center = (500, 800), hitRadius = 30
        // Tap: (510, 805) -> distance = sqrt(100 + 25) = ~11.18 <= 30 -> HIT
        val isHitClose = TargetMath.isHit(
            tapX = 510f,
            tapY = 805f,
            targetX = 500f,
            targetY = 800f,
            hitRadius = 30f
        )
        assertTrue("Tap inside hitRadius must be detected as hit", isHitClose)

        // Tap far away: (700, 900) -> distance = sqrt(40000 + 10000) = ~223.6 > 30 -> MISS
        val isHitFar = TargetMath.isHit(
            tapX = 700f,
            tapY = 900f,
            targetX = 500f,
            targetY = 800f,
            hitRadius = 30f
        )
        assertFalse("Tap outside hitRadius must not be detected as hit", isHitFar)

        // Exact boundary: (530, 800) -> distance = 30 <= 30 -> HIT
        val isHitBoundary = TargetMath.isHit(
            tapX = 530f,
            tapY = 800f,
            targetX = 500f,
            targetY = 800f,
            hitRadius = 30f
        )
        assertTrue("Tap on boundary must be hit", isHitBoundary)
    }

    @Test
    fun testTargetGeneration_100RandomTargets() {
        val width = 1080f
        val height = 2400f
        val radius = 70f
        val topMargin = 200f
        val bottomMargin = 150f
        val leftMargin = 32f
        val rightMargin = 32f

        var previousPos: Offset? = null
        val random = Random(42)

        for (i in 0 until 100) {
            val pos = TargetMath.generateTargetPosition(
                width = width,
                height = height,
                radius = radius,
                previousX = previousPos?.x,
                previousY = previousPos?.y,
                topMargin = topMargin,
                bottomMargin = bottomMargin,
                leftMargin = leftMargin,
                rightMargin = rightMargin,
                random = random
            )

            // 1. Target fully fits inside bounds
            val left = pos.x - radius
            val right = pos.x + radius
            val top = pos.y - radius
            val bottom = pos.y + radius

            assertTrue("Left edge $left must be >= leftMargin $leftMargin", left >= leftMargin - 0.01f)
            assertTrue("Right edge $right must be <= width - rightMargin", right <= (width - rightMargin) + 0.01f)
            assertTrue("Top edge $top must be >= topMargin $topMargin", top >= topMargin - 0.01f)
            assertTrue("Bottom edge $bottom must be <= height - bottomMargin", bottom <= (height - bottomMargin) + 0.01f)

            // 2. Radius is positive
            assertTrue("Radius must be positive", radius > 0f)

            previousPos = pos
        }
    }

    @Test
    fun testStreakMultiplierAndMilestones() {
        assertEquals(1.0, TargetMath.getStreakMultiplier(0), 0.001)
        assertEquals(1.0, TargetMath.getStreakMultiplier(1), 0.001)
        assertEquals(1.05, TargetMath.getStreakMultiplier(2), 0.001)
        assertEquals(1.20, TargetMath.getStreakMultiplier(5), 0.001)
        assertEquals(1.50, TargetMath.getStreakMultiplier(11), 0.001)
        assertEquals(2.0, TargetMath.getStreakMultiplier(21), 0.001)
        assertEquals(2.0, TargetMath.getStreakMultiplier(50), 0.001) // Capped at 2.0x

        // Milestone messages
        assertEquals("NICE!", TargetMath.getStreakMilestoneMessage(5))
        assertEquals("GREAT!", TargetMath.getStreakMilestoneMessage(10))
        assertEquals("ON FIRE!", TargetMath.getStreakMilestoneMessage(20))
        assertEquals("UNSTOPPABLE!", TargetMath.getStreakMilestoneMessage(30))
        assertEquals("TAP MASTER!", TargetMath.getStreakMilestoneMessage(50))
        assertNull(TargetMath.getStreakMilestoneMessage(4))
        assertNull(TargetMath.getStreakMilestoneMessage(6))
    }

    @Test
    fun testScoreCalculation() {
        // Immediate tap at start (precision ~1.0) with streak 1
        val scoreImmediate = TargetMath.calculateScore(baseScore = 100, precision = 1.0, streak = 1)
        // base = 100 * 1.0 = 100, bonus = 100 -> 200
        assertEquals(200, scoreImmediate)

        // Halfway tap (precision = 0.5) with streak 1
        val scoreHalfway = TargetMath.calculateScore(baseScore = 100, precision = 0.5, streak = 1)
        // base = 100, bonus = 50 -> 150
        assertEquals(150, scoreHalfway)

        // Immediate tap with max streak multiplier (2.0x)
        val scoreMaxStreak = TargetMath.calculateScore(baseScore = 100, precision = 1.0, streak = 25)
        // base = 100 * 2.0 = 200, bonus = 100 -> 300
        assertEquals(300, scoreMaxStreak)
    }

    @Test
    fun testDifficultyProgression() {
        // EASY
        assertEquals(1500L, TargetMath.getTargetDurationForHit(Difficulty.EASY, 0))
        // After 3 hits: -80ms -> 1420ms
        assertEquals(1420L, TargetMath.getTargetDurationForHit(Difficulty.EASY, 3))
        // After 6 hits: -160ms -> 1340ms
        assertEquals(1340L, TargetMath.getTargetDurationForHit(Difficulty.EASY, 6))
        // Minimum cap is 550ms
        assertEquals(550L, TargetMath.getTargetDurationForHit(Difficulty.EASY, 100))

        // Timer specs
        assertEquals(12f, TargetMath.getInitialTimerSeconds(Difficulty.EASY), 0.01f)
        assertEquals(10f, TargetMath.getInitialTimerSeconds(Difficulty.MEDIUM), 0.01f)
        assertEquals(8f, TargetMath.getInitialTimerSeconds(Difficulty.HARD), 0.01f)

        assertEquals(0.8f, TargetMath.getTimeBonus(Difficulty.EASY), 0.01f)
        assertEquals(0.7f, TargetMath.getTimeBonus(Difficulty.MEDIUM), 0.01f)
        assertEquals(0.6f, TargetMath.getTimeBonus(Difficulty.HARD), 0.01f)

        assertEquals(15f, TargetMath.getMaxTimerSeconds(Difficulty.EASY), 0.01f)
        assertEquals(13f, TargetMath.getMaxTimerSeconds(Difficulty.MEDIUM), 0.01f)
        assertEquals(11f, TargetMath.getMaxTimerSeconds(Difficulty.HARD), 0.01f)
    }
}
