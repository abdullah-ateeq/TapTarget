package com.example.taptarget.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptarget.data.model.GameState
import com.example.taptarget.game.TargetMath
import com.example.taptarget.ui.theme.StreakGold
import com.example.taptarget.ui.theme.UrgentRed
import com.example.taptarget.ui.viewmodel.TapTargetViewModel
import java.util.Locale

@Composable
fun GameScreen(
    viewModel: TapTargetViewModel,
    onNavigateToResult: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRestartDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back button during game
    BackHandler {
        if (uiState.gameState == GameState.PLAYING) {
            viewModel.pauseGame()
        } else if (uiState.gameState == GameState.PAUSED) {
            showExitDialog = true
        } else {
            viewModel.exitToHome()
            onNavigateToHome()
        }
    }

    // Automatically navigate to Result screen when Game Over
    LaunchedEffect(uiState.gameState) {
        if (uiState.gameState == GameState.GAME_OVER) {
            onNavigateToResult()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value

        // Keep playable bounds synchronized
        LaunchedEffect(widthDp, heightDp) {
            viewModel.setPlayableBounds(
                widthDp = widthDp,
                heightDp = heightDp,
                topMargin = 120f,
                bottomMargin = 60f,
                sideMargin = 20f
            )
        }

        // 1. Compose Canvas for Game Field
        GameCanvas(
            uiState = uiState,
            onTap = { x, y -> viewModel.onPlayfieldTap(x, y) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak & Multiplier
                Column {
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.streak}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (uiState.streak >= 5) StreakGold else MaterialTheme.colorScheme.onBackground
                        )
                        if (uiState.streak > 1) {
                            val mult = TargetMath.getStreakMultiplier(uiState.streak)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StreakGold.copy(alpha = 0.2f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f×", mult),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StreakGold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Pause Button
                IconButton(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${uiState.score}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Prominent Global Timer Display
            val isUrgent = uiState.remainingGlobalTimeSec <= 3.0f && uiState.gameState == GameState.PLAYING
            val infiniteTransition = rememberInfiniteTransition(label = "TimerPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (isUrgent) 1.12f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(280),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUrgent) UrgentRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = if (isUrgent) androidx.compose.foundation.BorderStroke(1.5.dp, UrgentRed) else null,
                    modifier = Modifier.scale(if (isUrgent) pulseScale else 1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIME ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUrgent) UrgentRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1fs", uiState.remainingGlobalTimeSec),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isUrgent) UrgentRed else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // 3. Streak Milestone Notification Banner
        AnimatedVisibility(
            visible = uiState.activeMilestone != null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 170.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = StreakGold,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = uiState.activeMilestone.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }

        // 4. Countdown Overlay (3 - 2 - 1 - GO!)
        if (uiState.gameState == GameState.COUNTDOWN && uiState.countdownText != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.countdownText.orEmpty(),
                    fontSize = if (uiState.countdownText == "GO!") 72.sp else 96.sp,
                    fontWeight = FontWeight.Black,
                    color = if (uiState.countdownText == "GO!") MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }

        // 5. Pause Overlay
        if (uiState.gameState == GameState.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GAME PAUSED",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current Score: ${uiState.score} • Streak: ${uiState.streak}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.resumeGame() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("RESUME", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("RESTART", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("EXIT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restart this game?") },
            text = { Text("Current progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        viewModel.restartGame()
                    }
                ) {
                    Text("RESTART")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit game?") },
            text = { Text("Current progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitToHome()
                        onNavigateToHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("EXIT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
