package com.example.taptarget.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptarget.ui.theme.TargetDeepCyan
import com.example.taptarget.ui.theme.TargetElectricCyan
import com.example.taptarget.ui.theme.TargetLightBlue
import com.example.taptarget.ui.theme.TargetVibrantBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    var startShrink by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val targetOuterColor = if (isDark) TargetElectricCyan else TargetVibrantBlue
    val targetFillColor = if (isDark) TargetDeepCyan else TargetLightBlue

    val scale by animateFloatAsState(
        targetValue = if (startShrink) 0.2f else 1.0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "SplashShrink"
    )

    LaunchedEffect(Unit) {
        startShrink = true
        delay(1250L)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TAPTARGET",
                style = MaterialTheme.typography.headlineLarge,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Shrinking Target
            Canvas(modifier = Modifier.size(120.dp)) {
                val centerOffset = center
                val baseRadius = size.minDimension / 2f
                val radius = baseRadius * scale

                // Outer ring
                drawCircle(
                    color = targetOuterColor,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = 6.dp.toPx())
                )

                // Fill disk
                drawCircle(
                    color = targetFillColor.copy(alpha = 0.85f),
                    radius = radius * 0.72f,
                    center = centerOffset
                )

                // Center dot
                drawCircle(
                    color = Color.White,
                    radius = (radius * 0.22f).coerceAtLeast(3.dp.toPx()),
                    center = centerOffset
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Tap before it disappears.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
