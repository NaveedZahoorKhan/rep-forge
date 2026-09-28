package com.gymtracker.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WarmUpBadgeType
import com.gymtracker.app.domain.model.WarmUpMovement
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog

@Composable
fun DynamicWarmUpCard(
    suggestedRoutine: DynamicWarmUpRoutine,
    onStartWarmUp: (DynamicWarmUpRoutine) -> Unit,
    modifier: Modifier = Modifier,
    scheduledWorkoutName: String = "",
    onMuscleGroupSelected: ((MuscleGroup) -> Unit)? = null,
) {
    var expandedDetails by remember { mutableStateOf(false) }
    var selectedMuscle by remember(suggestedRoutine) { mutableStateOf(suggestedRoutine.primaryMuscleGroup) }

    val currentRoutine = remember(selectedMuscle, suggestedRoutine) {
        if (selectedMuscle == suggestedRoutine.primaryMuscleGroup) {
            suggestedRoutine
        } else {
            DynamicWarmUpCatalog.getWarmUpRoutine(
                primary = selectedMuscle,
                workoutName = scheduledWorkoutName,
            )
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val amberFire = Color(0xFFFF6F00)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_warmup_card")
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, amberFire.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Fire Icon + Targeted Muscle Routine Label
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(amberFire.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Dynamic Warm-Up",
                            tint = amberFire,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "DYNAMIC WARM-UP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = amberFire,
                                letterSpacing = 1.sp,
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    "${currentRoutine.estimatedDurationMinutes} MINS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }

                        Text(
                            text = if (scheduledWorkoutName.isNotBlank()) {
                                "Targeting ${currentRoutine.primaryMuscleGroup.label()} for $scheduledWorkoutName"
                            } else {
                                "Targeting ${currentRoutine.primaryMuscleGroup.label()} Routine"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Benefits explanation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp),
                    )
                    Text(
                        text = currentRoutine.benefitsSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
            }

            // Muscle Group Selector Pills (if user wants to customize or switch target)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Target Muscle Focus:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val supportedMuscles = listOf(
                        MuscleGroup.CHEST,
                        MuscleGroup.BACK,
                        MuscleGroup.LEGS,
                        MuscleGroup.SHOULDERS,
                        MuscleGroup.GLUTES,
                        MuscleGroup.BICEPS,
                        MuscleGroup.ABS,
                        MuscleGroup.FULL_BODY,
                    )

                    supportedMuscles.forEach { muscle ->
                        val isSelected = muscle == selectedMuscle
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedMuscle = muscle
                                onMuscleGroupSelected?.invoke(muscle)
                            },
                            label = {
                                Text(
                                    when (muscle) {
                                        MuscleGroup.CHEST -> "Chest (Push)"
                                        MuscleGroup.BACK -> "Back (Pull)"
                                        MuscleGroup.LEGS -> "Legs / Squat"
                                        MuscleGroup.SHOULDERS -> "Shoulders"
                                        MuscleGroup.GLUTES -> "Glutes / Hinge"
                                        MuscleGroup.BICEPS -> "Arms"
                                        MuscleGroup.ABS -> "Core / Abs"
                                        MuscleGroup.FULL_BODY -> "Full Body"
                                        else -> muscle.label()
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = amberFire.copy(alpha = 0.2f),
                                selectedLabelColor = amberFire,
                            ),
                            border = if (isSelected) BorderStroke(1.dp, amberFire) else null,
                            modifier = Modifier.testTag("muscle_chip_${muscle.name.lowercase()}"),
                        )
                    }
                }
            }

            // Quick Preview of the 5 Dynamic Movements
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${currentRoutine.movements.size} Dynamic Movements:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Tap to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                currentRoutine.movements.forEachIndexed { index, movement ->
                    MovementPreviewRow(
                        index = index + 1,
                        movement = movement,
                        onClick = { expandedDetails = !expandedDetails },
                    )
                }
            }

            // Expanded Movement Details
            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Detailed Coaching Cues & Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    currentRoutine.movements.forEachIndexed { index, movement ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${index + 1}. ${movement.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            movement.badgeType.name.replace('_', ' '),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                }

                                Text(
                                    "Focus: ${movement.jointMobilityFocus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                Text(
                                    movement.instructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = amberFire.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = amberFire, modifier = Modifier.size(14.dp))
                                        Text(
                                            "Cue: ${movement.coachingCue}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Buttons: "Start Guided Warm-Up" + "Toggle Details"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { expandedDetails = !expandedDetails },
                    modifier = Modifier.weight(1f).testTag("warmup_toggle_details_btn"),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (expandedDetails) "Hide Cues" else "Full Routine", fontSize = 13.sp)
                }

                Button(
                    onClick = { onStartWarmUp(currentRoutine) },
                    modifier = Modifier.weight(1.3f).testTag("start_warmup_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberFire,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Warm-Up", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MovementPreviewRow(
    index: Int,
    movement: WarmUpMovement,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(22.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Column {
                    Text(
                        text = movement.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = movement.jointMobilityFocus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = when {
                        movement.durationSeconds != null -> "${movement.durationSeconds}s"
                        movement.isPerSide -> "${movement.reps ?: 10}/side"
                        else -> "${movement.reps ?: 10} reps"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
