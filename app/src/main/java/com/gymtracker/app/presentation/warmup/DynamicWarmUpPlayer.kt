package com.gymtracker.app.presentation.warmup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WarmUpMovement
import com.gymtracker.app.notification.WorkoutSoundPlayer
import kotlinx.coroutines.delay

class WarmUpPlayerStateHolder(
    val routine: DynamicWarmUpRoutine,
    initialIndex: Int = 0,
    initialCompleted: Set<Int> = emptySet(),
    initialSkipped: Set<Int> = emptySet(),
) {
    var currentIndex by mutableIntStateOf(initialIndex)
    var completedIndices by mutableStateOf(initialCompleted)
    var skippedIndices by mutableStateOf(initialSkipped)
    var showExitConfirmation by mutableStateOf(false)

    val totalMovements: Int get() = routine.movements.size
    val currentMovement: WarmUpMovement? get() = routine.movements.getOrNull(currentIndex)

    val completedCount: Int get() = completedIndices.size
    val skippedCount: Int get() = skippedIndices.size
    val processedCount: Int get() = (completedIndices + skippedIndices).size
    val remainingCount: Int get() = (totalMovements - processedCount).coerceAtLeast(0)

    val isCompleted: Boolean get() = totalMovements > 0 && processedCount == totalMovements
    val progressFraction: Float get() = if (totalMovements > 0) processedCount.toFloat() / totalMovements else 0f

    val nextIncompleteIndex: Int?
        get() {
            val processed = completedIndices + skippedIndices
            return ((currentIndex + 1 until totalMovements) + (0 until currentIndex))
                .firstOrNull { it !in processed }
        }

    val prevMovement: WarmUpMovement? get() = routine.movements.getOrNull(currentIndex - 1)
    val nextMovement: WarmUpMovement? get() = nextIncompleteIndex?.let { routine.movements.getOrNull(it) }

    fun markCurrentDone() {
        if (currentIndex in 0 until totalMovements) {
            completedIndices = completedIndices + currentIndex
            skippedIndices = skippedIndices - currentIndex
            val next = nextIncompleteIndex
            if (next != null) {
                currentIndex = next
            }
        }
    }

    fun markCurrentSkipped() {
        if (currentIndex in 0 until totalMovements) {
            skippedIndices = skippedIndices + currentIndex
            completedIndices = completedIndices - currentIndex
            val next = nextIncompleteIndex
            if (next != null) {
                currentIndex = next
            }
        }
    }

    fun goToPrevious() {
        if (currentIndex > 0) {
            currentIndex -= 1
        }
    }

    fun jumpToStep(index: Int) {
        if (index in 0 until totalMovements) {
            currentIndex = index
        }
    }

    fun requestExit(onDirectExit: () -> Unit) {
        if (!isCompleted && processedCount < totalMovements && totalMovements > 0) {
            showExitConfirmation = true
        } else {
            onDirectExit()
        }
    }

    fun confirmExit(onConfirmed: () -> Unit) {
        showExitConfirmation = false
        onConfirmed()
    }

    fun dismissExitConfirmation() {
        showExitConfirmation = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicWarmUpPlayerModal(
    routine: DynamicWarmUpRoutine,
    soundPlayer: WorkoutSoundPlayer,
    onDismiss: () -> Unit,
    onStartWorkout: ((workoutId: String) -> Unit)? = null,
    targetWorkoutId: String = "",
    isFromActiveWorkout: Boolean = false,
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
            isFromActiveWorkout = isFromActiveWorkout,
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
    isFromActiveWorkout: Boolean = false,
) {
    var currentIndex by rememberSaveable(routine.id) { mutableIntStateOf(0) }
    var completedIndices by rememberSaveable(routine.id) { mutableStateOf(emptySet<Int>()) }
    var skippedIndices by rememberSaveable(routine.id) { mutableStateOf(emptySet<Int>()) }
    var showExitConfirmation by rememberSaveable(routine.id) { mutableStateOf(false) }

    val amberFire = Color(0xFFFF6F00)

    val state = remember(routine, currentIndex, completedIndices, skippedIndices, showExitConfirmation) {
        WarmUpPlayerStateHolder(
            routine = routine,
            initialIndex = currentIndex,
            initialCompleted = completedIndices,
            initialSkipped = skippedIndices,
        ).apply {
            this.showExitConfirmation = showExitConfirmation
        }
    }

    // Sync back state changes
    LaunchedEffect(state.currentIndex, state.completedIndices, state.skippedIndices, state.showExitConfirmation) {
        currentIndex = state.currentIndex
        completedIndices = state.completedIndices
        skippedIndices = state.skippedIndices
        showExitConfirmation = state.showExitConfirmation
    }

    val totalMovements = state.totalMovements
    val currentMovement = state.currentMovement

    // Exit Confirmation Dialog
    if (state.showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { state.dismissExitConfirmation() },
            title = {
                Text(
                    text = "Exit Warm-Up?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have ${state.remainingCount} remaining movements out of $totalMovements in this dynamic warm-up.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "• Completed: ${state.completedCount}\n• Skipped: ${state.skippedCount}\n• Remaining: ${state.remainingCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.confirmExit(onDismiss)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_exit_warmup_btn"),
                ) {
                    Text("Exit Warm-Up", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { state.dismissExitConfirmation() },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Return to Warm-Up")
                }
            },
            modifier = Modifier.testTag("warmup_exit_dialog"),
        )
    }

    // Timer state for timed movements
    val totalSeconds = currentMovement?.durationSeconds ?: 0
    var secondsRemaining by rememberSaveable(currentMovement?.id) { mutableIntStateOf(totalSeconds) }
    var isTimerRunning by rememberSaveable(currentMovement?.id) { mutableStateOf(false) }

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
                modifier = Modifier.weight(1f, fill = false),
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
                        contentDescription = "Warm-up Routine",
                        tint = amberFire,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Targeting ${routine.primaryMuscleGroup.label()} • ~${routine.estimatedDurationMinutes} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = { state.requestExit(onDismiss) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("warmup_player_close_btn")
                    .semantics {
                        contentDescription = "Close Warm-Up"
                        role = Role.Button
                    },
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Warm-Up")
            }
        }

        // Empty Warm-Up State
        if (totalMovements == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "No Warm-Up Movements Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "This routine does not contain any warm-up movements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Close Warm-Up", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        // Overall Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.isCompleted) {
                        "All Movements Processed!"
                    } else {
                        "Step ${state.currentIndex + 1} of $totalMovements"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = amberFire,
                )
                Text(
                    text = "${(state.progressFraction * 100).toInt()}% Done",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = amberFire,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Breakdown badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Completed: ${state.completedCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Skipped: ${state.skippedCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Remaining: ${state.remainingCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = amberFire,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (state.isCompleted) {
            // Completion View
            WarmUpCompletionCelebration(
                routine = routine,
                completedCount = state.completedCount,
                skippedCount = state.skippedCount,
                isFromActiveWorkout = isFromActiveWorkout,
                onDismiss = onDismiss,
                onStartWorkout = onStartWorkout,
                targetWorkoutId = targetWorkoutId,
            )
        } else if (currentMovement != null) {
            // Active Movement View
            MovementActiveCard(
                movement = currentMovement,
                index = state.currentIndex + 1,
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

            // Next Movement Preview
            NextMovementPreviewCard(
                nextMovement = state.nextMovement,
                nextIndex = state.nextIncompleteIndex,
                totalMovements = totalMovements,
            )

            // Step Navigation Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        state.goToPrevious()
                        isTimerRunning = false
                    },
                    enabled = state.currentIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Previous step"
                            role = Role.Button
                        },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Prev",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                OutlinedButton(
                    onClick = {
                        state.markCurrentSkipped()
                        isTimerRunning = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("warmup_skip_btn")
                        .semantics {
                            contentDescription = "Skip step ${state.currentIndex + 1}"
                            role = Role.Button
                        },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = {
                        state.markCurrentDone()
                        isTimerRunning = false
                        if (state.isCompleted) {
                            soundPlayer.playRestCompleteSound()
                            soundPlayer.vibrateRestComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("warmup_next_done_btn")
                        .semantics {
                            contentDescription = if (state.nextIncompleteIndex != null) "Complete movement and advance to next step" else "Finish warm-up routine"
                            role = Role.Button
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberFire,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (state.nextIncompleteIndex != null) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (state.nextIncompleteIndex != null) "Done • Next" else "Finish Warm-Up",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // Quick Jump Step Indicators
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Routine Steps Progress:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    routine.movements.forEachIndexed { i, m ->
                        val isCurr = i == state.currentIndex
                        val isDone = i in state.completedIndices
                        val isSkipped = i in state.skippedIndices

                        val statusLabel = when {
                            isDone -> "Completed"
                            isSkipped -> "Skipped"
                            isCurr -> "Current"
                            else -> "Pending"
                        }

                        Surface(
                            onClick = {
                                state.jumpToStep(i)
                                isTimerRunning = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isCurr -> amberFire.copy(alpha = 0.2f)
                                isDone -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                isSkipped -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = if (isCurr) BorderStroke(1.5.dp, amberFire) else null,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics {
                                    contentDescription = "Step ${i + 1}: ${m.name}, $statusLabel"
                                    role = Role.Button
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                when {
                                    isDone -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    isSkipped -> {
                                        Icon(
                                            imageVector = Icons.Default.FastForward,
                                            contentDescription = "Skipped",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isCurr) amberFire else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${i + 1}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurr) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = m.name,
                                    fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurr) amberFire else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    maxLines = 1,
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
private fun NextMovementPreviewCard(
    nextMovement: WarmUpMovement?,
    nextIndex: Int?,
    totalMovements: Int,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (nextIndex != null) "NEXT MOVEMENT (Step ${nextIndex + 1} of $totalMovements)" else "FINAL MOVEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = nextMovement?.name ?: "All remaining movements completed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (nextMovement != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = when {
                            nextMovement.durationSeconds != null -> "${nextMovement.durationSeconds}s"
                            nextMovement.isPerSide -> "${nextMovement.reps ?: 10}/side"
                            else -> "${nextMovement.reps ?: 10} reps"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = amberFire.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "STEP $index OF $total • ${movement.badgeType.name.replace('_', ' ')}",
                                color = amberFire,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
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
                                    fontFamily = FontFamily.Monospace,
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
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = if (isTimerRunning) "Pause countdown timer" else "Start countdown timer"
                                        role = Role.Button
                                    },
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isTimerRunning) "Pause" else "Start Timer", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onResetTimer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = "Reset timer"
                                        role = Role.Button
                                    },
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
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
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }

            // Step-by-Step Instructions (Useful Text Instructions, No Decorative Image Needed)
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
    completedCount: Int,
    skippedCount: Int,
    isFromActiveWorkout: Boolean,
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
                text = "Your core temperature is elevated, joint capsules are bathed in synovial fluid, and your nervous system is primed for ${routine.primaryMuscleGroup.label()}!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )

            // Progress Summary Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMPLETED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("$completedCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SKIPPED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("$skippedCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (isFromActiveWorkout) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("warmup_return_to_workout_btn")
                        .semantics {
                            contentDescription = "Return to workout"
                            role = Role.Button
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberFire,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "⚡ Return to Active Workout",
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (targetWorkoutId.isNotBlank() && onStartWorkout != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onStartWorkout(targetWorkoutId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("warmup_start_workout_now_btn")
                        .semantics {
                            contentDescription = "Start workout"
                            role = Role.Button
                        },
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
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "Return to Workouts"
                        role = Role.Button
                    },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (isFromActiveWorkout) "Close" else "Return to Routines")
            }
        }
    }
}
