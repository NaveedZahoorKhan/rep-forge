package com.gymtracker.app.domain.usecase

import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicWarmUpCatalogTest {

    @Test
    fun `suggestRoutineForWorkout returns Full Body routine when workout is null`() {
        val routine = DynamicWarmUpCatalog.suggestRoutineForWorkout(null, emptyList())
        assertEquals(MuscleGroup.FULL_BODY, routine.primaryMuscleGroup)
        assertTrue(routine.movements.isNotEmpty())
        assertTrue(routine.benefitsSummary.isNotBlank())
    }

    @Test
    fun `resolveTargetMuscleGroups resolves accurately from associated exercises`() {
        val workout = WorkoutEntity(id = "w1", name = "Hypertrophy Push", splitType = "Custom")
        val benchPress = ExerciseEntity(
            id = "ex1",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            difficulty = Difficulty.BEGINNER,
            instructions = "Press the bar",
        )
        val overheadPress = ExerciseEntity(
            id = "ex2",
            name = "Overhead Press",
            primaryMuscle = MuscleGroup.SHOULDERS,
            equipment = Equipment.BARBELL,
            difficulty = Difficulty.INTERMEDIATE,
            instructions = "Press overhead",
        )
        val inclinePress = ExerciseEntity(
            id = "ex3",
            name = "Incline Dumbbell Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            difficulty = Difficulty.BEGINNER,
            instructions = "Press on incline",
        )

        val workoutExercises = listOf(
            WorkoutExerciseEntity(id = "we1", workoutId = "w1", exerciseId = "ex1", orderIndex = 0),
            WorkoutExerciseEntity(id = "we2", workoutId = "w1", exerciseId = "ex2", orderIndex = 1),
            WorkoutExerciseEntity(id = "we3", workoutId = "w1", exerciseId = "ex3", orderIndex = 2),
        )

        val (primary, secondaries) = DynamicWarmUpCatalog.resolveTargetMuscleGroups(
            workout = workout,
            exercises = listOf(benchPress, overheadPress, inclinePress),
            workoutExercises = workoutExercises,
        )

        assertEquals(MuscleGroup.CHEST, primary)
        assertTrue(secondaries.contains(MuscleGroup.SHOULDERS))
    }

    @Test
    fun `resolveTargetMuscleGroups falls back to workout name parsing when no exercises mapped`() {
        val pushWorkout = WorkoutEntity(id = "w_push", name = "Push Day A", splitType = "Push/Pull/Legs")
        val (pushPrimary, _) = DynamicWarmUpCatalog.resolveTargetMuscleGroups(pushWorkout, emptyList(), emptyList())
        assertEquals(MuscleGroup.CHEST, pushPrimary)

        val pullWorkout = WorkoutEntity(id = "w_pull", name = "Heavy Pull Day", splitType = "PPL")
        val (pullPrimary, _) = DynamicWarmUpCatalog.resolveTargetMuscleGroups(pullWorkout, emptyList(), emptyList())
        assertEquals(MuscleGroup.BACK, pullPrimary)

        val legWorkout = WorkoutEntity(id = "w_leg", name = "Leg Day (Quad Focus)", splitType = "PPL")
        val (legPrimary, _) = DynamicWarmUpCatalog.resolveTargetMuscleGroups(legWorkout, emptyList(), emptyList())
        assertEquals(MuscleGroup.LEGS, legPrimary)

        val shoulderWorkout = WorkoutEntity(id = "w_sh", name = "Shoulder & Delt Destruction", splitType = "Bro Split")
        val (shoulderPrimary, _) = DynamicWarmUpCatalog.resolveTargetMuscleGroups(shoulderWorkout, emptyList(), emptyList())
        assertEquals(MuscleGroup.SHOULDERS, shoulderPrimary)
    }

    @Test
    fun `all supported routines contain non-empty coaching cues and instructions`() {
        val allRoutines = DynamicWarmUpCatalog.getAllTargetRoutines()
        assertTrue(allRoutines.size >= 8)

        for (routine in allRoutines) {
            assertTrue("Routine ${routine.title} has movements", routine.movements.isNotEmpty())
            assertTrue("Routine ${routine.title} benefits are present", routine.benefitsSummary.isNotBlank())
            assertTrue("Routine ${routine.title} estimated duration > 0", routine.estimatedDurationMinutes > 0)

            for (movement in routine.movements) {
                assertNotNull("Movement ${movement.name} has target muscle", movement.targetMuscleGroup)
                assertTrue("Movement ${movement.name} has coaching cue", movement.coachingCue.isNotBlank())
                assertTrue("Movement ${movement.name} has instructions", movement.instructions.isNotBlank())
                assertTrue("Movement ${movement.name} has mobility focus", movement.jointMobilityFocus.isNotBlank())
                assertTrue(
                    "Movement ${movement.name} has either duration or reps",
                    movement.durationSeconds != null || movement.reps != null,
                )
            }
        }
    }
}
