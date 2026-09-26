package com.gymtracker.app.presentation.active

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Persistent countdown timer component for tracking workout rest intervals.
 * Remains docked and accessible throughout the workout session.
 * Supports start, pause, resume, adjustment (+/- seconds), presets, and skip.
 */
@Composable
fun PersistentRestTimerComponent(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    isFinished: Boolean,
    exerciseName: String,
    onStart: (seconds: Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggle: () -> Unit,
    onAddSeconds: (seconds: Int) -> Unit,
    onSubtractSeconds: (seconds: Int) -> Unit,
    onRestart: () -> Unit,
    onSkip: () -> Unit,
    onDismissFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isTimerActive = (remainingSeconds > 0) || isRunning || isPaused

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("persistent_rest_timer"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                // 1. FINISHED STATE: Rest complete notification banner
                isFinished -> {
                    RestTimerFinishedBanner(
                        exerciseName = exerciseName,
                        onDismiss = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismissFinished()
                        },
                        onRestart = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRestart()
                        }
                    )
                }

                // 2. ACTIVE STATE: Countdown timer currently running or paused
                isTimerActive -> {
                    RestTimerActiveView(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        isRunning = isRunning,
                        isPaused = isPaused,
                        exerciseName = exerciseName,
                        onPause = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPause()
                        },
                        onResume = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResume()
                        },
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                        onAddSeconds = { sec ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddSeconds(sec)
                        },
                        onSubtractSeconds = { sec ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSubtractSeconds(sec)
                        },
                        onRestart = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRestart()
                        },
                        onSkip = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkip()
                        }
                    )
                }

                // 3. IDLE / READY STATE: Persistent quick rest interval starter
                else -> {
                    RestTimerIdleView(
                        onStartPreset = { seconds ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStart(seconds)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Displayed when rest timer countdown is active (running or paused).
 */
@Composable
private fun RestTimerActiveView(
    remainingSeconds: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    exerciseName: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggle: () -> Unit,
    onAddSeconds: (Int) -> Unit,
    onSubtractSeconds: (Int) -> Unit,
    onRestart: () -> Unit,
    onSkip: () -> Unit,
) {
    val progressTarget = if (totalSeconds > 0) {
        (1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        label = "restProgress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    // Status color
    val statusColor by animateColorAsState(
        targetValue = if (isPaused) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "statusColor"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header Row: Status badge, Exercise Name, Skip/Dismiss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing dot or status badge
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = if (isPaused) "REST PAUSED" else "REST INTERVAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 1.sp
                )
                if (exerciseName.isNotBlank()) {
                    Text(
                        text = "• $exerciseName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Quick skip / dismiss button
            IconButton(
                onClick = onSkip,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("rest_timer_skip")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Skip Rest Timer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Main Timer Row: Big Monospace Clock + Quick Adjustments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$remainingSeconds of $totalSeconds seconds remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Adjustment Steppers (+30s, +15s, -15s)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onSubtractSeconds(15) },
                    modifier = Modifier.size(36.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("-15", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalIconButton(
                    onClick = { onAddSeconds(15) },
                    modifier = Modifier.size(36.dp),
                    colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("+15", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onAddSeconds(30) },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("rest_timer_add_30"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("30s", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Linear Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Action Buttons Row: Pause/Resume Toggle, Restart, Finish
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Start/Pause/Resume Button
            Button(
                onClick = onToggle,
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
                    .testTag("rest_timer_toggle"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume Timer" else "Pause Timer",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPaused) "Resume Rest" else "Pause Rest",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Restart Button
            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("rest_timer_restart"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Restart Timer",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Restart", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Skip / Done Button
            FilledTonalButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Skip Rest",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Displayed when rest timer countdown reaches 0.
 */
@Composable
private fun RestTimerFinishedBanner(
    exerciseName: String,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Rest Complete! Ready for Next Set",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (exerciseName.isNotBlank()) {
                            Text(
                                text = "Next up: $exerciseName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("dismiss_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Banner",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Start Next Set", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rest More", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Displayed when no timer is active. Offers one-tap interval presets (30s, 60s, 90s, 120s, 180s)
 * so users can start their rest timer easily at any point in the workout session.
 */
@Composable
private fun RestTimerIdleView(
    onStartPreset: (seconds: Int) -> Unit,
) {
    val presets = listOf(30, 60, 90, 120, 180)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Rest Timer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Tap a preset to start interval",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Horizontal Row of Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { seconds ->
                val label = if (seconds >= 60) "${seconds / 60}m" else "${seconds}s"
                FilterChip(
                    selected = false,
                    onClick = { onStartPreset(seconds) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rest_timer_preset_$seconds"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Quick play default 60s
            FilledTonalIconButton(
                onClick = { onStartPreset(60) },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("rest_timer_play_default")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start 60s Rest",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
