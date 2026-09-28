package com.gymtracker.app.presentation.warmup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WarmUpMovement
import com.gymtracker.app.notification.WorkoutSoundPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicWarmUpPlayerModal(
    routine: DynamicWarmUpRoutine,
    soundPlayer: WorkoutSoundPlayer,
    onDismiss: () -> Unit,
    onStartWorkout: ((workoutId: String) -> Unit)? = null,
    targetWorkoutId: String = "",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("warmup_player_modal"),
    ) {
        DynamicWarmUpPlayerContent(
            routine = routine,
            soundPlayer = soundPlayer,
            onDismiss = onDismiss,
            onStartWorkout = onStartWorkout,
            targetWorkoutId = targetWorkoutId,
        )
    }
}

@Composable
fun DynamicWarmUpPlayerContent(
    routine: DynamicWarmUpRoutine,
    soundPlayer: WorkoutSoundPlayer,
    onDismiss: () -> Unit,
    onStartWorkout: ((workoutId: String) -> Unit)? = null,
    targetWorkoutId: String = "",
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    val amberFire = Color(0xFFFF6F00)

    val currentMovement = routine.movements.getOrNull(currentIndex)
    val totalMovements = routine.movements.size
    val progress = if (totalMovements > 0) (currentIndex + 1).toFloat() / totalMovements else 1f

    // Timer state for timed movements
    val totalSeconds = currentMovement?.durationSeconds ?: 0
    var secondsRemaining by remember(currentMovement) { mutableIntStateOf(totalSeconds) }
    var isTimerRunning by remember(currentMovement) { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning, secondsRemaining, currentMovement) {
        if (isTimerRunning && secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
            if (secondsRemaining == 0) {
                isTimerRunning = false
                soundPlayer.playRestCompleteSound()
                soundPlayer.vibrateRestComplete()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header: Routine Title & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(amberFire.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = amberFire,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Targeting ${routine.primaryMuscleGroup.label()} • ~${routine.estimatedDurationMinutes} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("warmup_player_close_btn"),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Warm-Up")
            }
        }

        // Progress bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isCompleted) "Completed!" else "Movement ${currentIndex + 1} of $totalMovements",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = amberFire,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = amberFire,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        if (isCompleted) {
            // Completion Celebration View
            WarmUpCompletionCelebration(
                routine = routine,
                onDismiss = onDismiss,
                onStartWorkout = onStartWorkout,
                targetWorkoutId = targetWorkoutId,
            )
        } else if (currentMovement != null) {
            // Active Movement View
            MovementActiveCard(
                movement = currentMovement,
                index = currentIndex + 1,
                total = totalMovements,
                secondsRemaining = secondsRemaining,
                totalSeconds = totalSeconds,
                isTimerRunning = isTimerRunning,
                onToggleTimer = { isTimerRunning = !isTimerRunning },
                onResetTimer = {
                    secondsRemaining = totalSeconds
                    isTimerRunning = false
                },
            )

            // Step Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex -= 1
                            isTimerRunning = false
                        }
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    Spacer(Modifier.width(4.dp))
                    Text("Prev")
                }

                Button(
                    onClick = {
                        if (currentIndex < totalMovements - 1) {
                            currentIndex += 1
                            isTimerRunning = false
                        } else {
                            isCompleted = true
                            soundPlayer.playRestCompleteSound()
                            soundPlayer.vibrateRestComplete()
                        }
                    },
                    modifier = Modifier.weight(1.5f).testTag("warmup_next_done_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberFire,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (currentIndex < totalMovements - 1) {
                        Text("Done • Next", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Finish Warm-Up", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Jump Carousel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "All Routine Movements:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    routine.movements.forEachIndexed { i, m ->
                        val isCurr = i == currentIndex
                        Surface(
                            onClick = {
                                currentIndex = i
                                isTimerRunning = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurr) amberFire.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isCurr) BorderStroke(1.dp, amberFire) else null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    fontWeight = if (isCurr) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isCurr) amberFire else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementActiveCard(
    movement: WarmUpMovement,
    index: Int,
    total: Int,
    secondsRemaining: Int,
    totalSeconds: Int,
    isTimerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
) {
    val amberFire = Color(0xFFFF6F00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Movement Name and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = amberFire.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = movement.badgeType.name.replace('_', ' '),
                            color = amberFire,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = movement.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = movement.equipment,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Target Focus
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Focus: ${movement.jointMobilityFocus}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Timed or Rep Counter Component
            if (movement.durationSeconds != null && movement.durationSeconds > 0) {
                // Interactive Countdown Timer
                val timerProgress by animateFloatAsState(
                    targetValue = if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds else 0f,
                    label = "timerProgress",
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, amberFire.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { timerProgress },
                                modifier = Modifier.size(100.dp),
                                color = amberFire,
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$secondsRemaining",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (secondsRemaining == 0) MaterialTheme.colorScheme.error else amberFire,
                                )
                                Text("sec", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onToggleTimer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerRunning) MaterialTheme.colorScheme.secondary else amberFire,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isTimerRunning) "Pause" else "Start",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isTimerRunning) "Pause" else "Start Timer", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onResetTimer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            } else {
                // Rep-Based Movement
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "TARGET PRESCRIPTION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = amberFire,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                text = if (movement.isPerSide) {
                                    "${movement.reps ?: 10} Reps (Each Side)"
                                } else {
                                    "${movement.reps ?: 10} Dynamic Repetitions"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = amberFire.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${movement.reps ?: 10}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = amberFire,
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Step-by-Step Instructions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "How to Execute:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = movement.instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                )
            }

            // Coach's Cue Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = amberFire.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, amberFire.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = amberFire,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                    )
                    Column {
                        Text(
                            text = "COACH'S KEY CUE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = amberFire,
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            text = movement.coachingCue,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarmUpCompletionCelebration(
    routine: DynamicWarmUpRoutine,
    onDismiss: () -> Unit,
    onStartWorkout: ((workoutId: String) -> Unit)?,
    targetWorkoutId: String,
) {
    val amberFire = Color(0xFFFF6F00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, amberFire),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(amberFire.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = amberFire,
                    modifier = Modifier.size(38.dp),
                )
            }

            Text(
                text = "🔥 Dynamic Warm-Up Completed!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Your core temperature is elevated, joint capsules are bathed in synovial fluid, and your nervous system is primed to recruit maximal motor units for ${routine.primaryMuscleGroup.label()}!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (targetWorkoutId.isNotBlank() && onStartWorkout != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onStartWorkout(targetWorkoutId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("warmup_start_workout_now_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberFire,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (routine.targetWorkoutName.isNotBlank()) {
                            "⚡ Start ${routine.targetWorkoutName} Now"
                        } else {
                            "⚡ Start Today's Scheduled Workout"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Done")
            }
        }
    }
}
