package com.example.taptarget.ui.screens.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.taptarget.data.model.TargetState
import com.example.taptarget.ui.theme.StreakGold
import com.example.taptarget.ui.theme.TargetDeepCyan
import com.example.taptarget.ui.theme.TargetElectricCyan
import com.example.taptarget.ui.theme.TargetLightBlue
import com.example.taptarget.ui.theme.TargetVibrantBlue
import com.example.taptarget.ui.theme.UrgentRed
import com.example.taptarget.ui.viewmodel.GameUiState

@Composable
fun GameCanvas(
    uiState: GameUiState,
    onTap: (xDp: Float, yDp: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()

    // Primary target colors based on theme for maximum clarity
    val targetOuterColor = if (isDark) TargetElectricCyan else TargetVibrantBlue
    val targetFillColor = if (isDark) TargetDeepCyan else TargetLightBlue
    val centerDotColor = if (isDark) Color.White else Color.White
    val missColor = UrgentRed
    val bonusTextColor = StreakGold

    val textPaint = remember(density, bonusTextColor) {
        Paint().apply {
            color = bonusTextColor.toArgb()
            textSize = with(density) { 22.dp.toPx() }
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(8f, 0f, 2f, android.graphics.Color.argb(180, 0, 0, 0))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.gameState, uiState.targetState) {
                detectTapGestures { tapOffset ->
                    val tapXDp = tapOffset.x / density.density
                    val tapYDp = tapOffset.y / density.density
                    onTap(tapXDp, tapYDp)
                }
            }
    ) {
        val target = uiState.currentTarget ?: return@Canvas
        val targetCenterPx = Offset(
            x = target.x * density.density,
            y = target.y * density.density
        )
        val currentRadiusPx = uiState.currentRadiusDp * density.density

        when (uiState.targetState) {
            TargetState.SHRINKING -> {
                // 1. Outer Ring (Concentric Layer)
                val outerStrokeWidth = with(density) { 4.dp.toPx() }
                drawCircle(
                    color = targetOuterColor,
                    radius = currentRadiusPx,
                    center = targetCenterPx,
                    style = Stroke(width = outerStrokeWidth)
                )

                // 2. Main Circle (Inner disk, slightly inset from ring)
                val innerRadius = (currentRadiusPx * 0.72f).coerceAtLeast(with(density) { 6.dp.toPx() })
                drawCircle(
                    color = targetFillColor.copy(alpha = 0.85f),
                    radius = innerRadius,
                    center = targetCenterPx
                )

                // 3. Center Dot (High-precision pinpoint)
                val dotRadius = (currentRadiusPx * 0.22f).coerceIn(
                    with(density) { 3.dp.toPx() },
                    with(density) { 7.dp.toPx() }
                )
                drawCircle(
                    color = centerDotColor,
                    radius = dotRadius,
                    center = targetCenterPx
                )
            }

            TargetState.HIT -> {
                // Scale burst: 1.0 -> 1.15 -> 0, fading alpha
                val burst = uiState.hitBurstProgress
                val scale = if (burst < 0.35f) {
                    1.0f + (burst / 0.35f) * 0.15f
                } else {
                    1.15f * (1.0f - ((burst - 0.35f) / 0.65f))
                }.coerceAtLeast(0f)

                val alpha = (1.0f - burst).coerceIn(0f, 1f)

                if (scale > 0.01f && alpha > 0.01f) {
                    // Expanding burst ring
                    drawCircle(
                        color = targetOuterColor.copy(alpha = alpha),
                        radius = currentRadiusPx * scale,
                        center = targetCenterPx,
                        style = Stroke(width = with(density) { 5.dp.toPx() })
                    )

                    // Burst disk
                    drawCircle(
                        color = targetFillColor.copy(alpha = alpha * 0.7f),
                        radius = currentRadiusPx * scale * 0.65f,
                        center = targetCenterPx
                    )
                }
            }

            TargetState.MISSED -> {
                // Miss animation: expanding red ring, target fading out
                val progress = uiState.missRingProgress
                val alpha = (1f - progress).coerceIn(0f, 1f)
                val expandingRadius = currentRadiusPx + (progress * with(density) { 35.dp.toPx() })

                drawCircle(
                    color = missColor.copy(alpha = alpha),
                    radius = expandingRadius,
                    center = targetCenterPx,
                    style = Stroke(width = with(density) { 3.5.dp.toPx() })
                )

                // Fading core
                drawCircle(
                    color = missColor.copy(alpha = alpha * 0.5f),
                    radius = currentRadiusPx * (1f - progress * 0.4f),
                    center = targetCenterPx
                )
            }

            TargetState.SPAWNING -> {
                // Brief spawning flash
                drawCircle(
                    color = targetOuterColor.copy(alpha = 0.5f),
                    radius = currentRadiusPx,
                    center = targetCenterPx,
                    style = Stroke(width = with(density) { 2.dp.toPx() })
                )
            }
        }

        // Floating hit feedback popup (+100, +180)
        uiState.hitFeedback?.let { feedback ->
            val elapsedFeedback = System.currentTimeMillis() - feedback.timestamp
            if (elapsedFeedback in 0..400) {
                val feedbackProgress = (elapsedFeedback / 400f).coerceIn(0f, 1f)
                val floatOffsetPx = feedbackProgress * with(density) { 28.dp.toPx() }
                val alpha = (1f - feedbackProgress).coerceIn(0f, 1f)
                textPaint.alpha = (alpha * 255).toInt()

                val textX = feedback.x * density.density
                val textY = (feedback.y * density.density) - (currentRadiusPx * 1.2f) - floatOffsetPx

                drawContext.canvas.nativeCanvas.drawText(
                    feedback.text,
                    textX,
                    textY,
                    textPaint
                )
            }
        }
    }
}
