package com.example.taptarget.ui.screens.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taptarget.data.model.GameOverReason
import com.example.taptarget.ui.theme.HitSuccessGreen
import com.example.taptarget.ui.theme.StreakGold
import com.example.taptarget.ui.theme.UrgentRed
import com.example.taptarget.ui.viewmodel.TapTargetViewModel
import java.util.Locale

@Composable
fun ResultScreen(
    viewModel: TapTargetViewModel,
    onPlayAgain: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val result = uiState.lastResult

    BackHandler {
        onNavigateToHome()
    }

    val reasonText = when (result?.reason) {
        GameOverReason.MISSED_TARGET -> "TOO SLOW"
        GameOverReason.TIMEOUT -> "TIME'S UP"
        null -> "GAME OVER"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TAPTARGET",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "GAME OVER",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Reason Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = UrgentRed.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, UrgentRed.copy(alpha = 0.4f))
        ) {
            Text(
                text = reasonText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = UrgentRed,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FINAL SCORE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${result?.score ?: 0}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                if (result?.isNewBestScore == true) {
                    Spacer(modifier = Modifier.height(6.dp))
                    RecordBadge(text = "NEW BEST SCORE!")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Key metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn(
                        label = "TARGETS HIT",
                        value = "${result?.targetsHit ?: 0}"
                    )
                    MetricColumn(
                        label = "BEST STREAK",
                        value = "${result?.bestStreak ?: 0}",
                        isNewRecord = result?.isNewBestStreak == true
                    )
                    MetricColumn(
                        label = "PRECISION",
                        value = String.format(Locale.US, "%.0f%%", (result?.precision ?: 0.0) * 100),
                        isNewRecord = result?.isNewBestPrecision == true
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn(
                        label = "DIFFICULTY",
                        value = (result?.difficulty ?: uiState.difficulty).name
                    )
                    MetricColumn(
                        label = "ALL-TIME BEST",
                        value = "${stats.bestScore}",
                        isGold = true
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Play Again CTA
        Button(
            onClick = {
                viewModel.restartGame()
                onPlayAgain()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "PLAY AGAIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Home Button
        OutlinedButton(
            onClick = onNavigateToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "HOME",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    isNewRecord: Boolean = false,
    isGold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = when {
                isGold -> StreakGold
                isNewRecord -> HitSuccessGreen
                else -> MaterialTheme.colorScheme.onBackground
            }
        )
        if (isNewRecord) {
            Text(
                text = "NEW!",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = HitSuccessGreen
            )
        }
    }
}

@Composable
private fun RecordBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = StreakGold.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, StreakGold)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = StreakGold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
