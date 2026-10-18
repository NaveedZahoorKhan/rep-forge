package com.gymtracker.app.presentation.warmup

import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WarmUpBadgeType
import com.gymtracker.app.domain.model.WarmUpMovement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicWarmUpPlayerTest {

    private val testMovements = listOf(
        WarmUpMovement(
            id = "m1",
            name = "Arm Circles",
            targetMuscleGroup = MuscleGroup.SHOULDERS,
            jointMobilityFocus = "Shoulder Capsule",
            durationSeconds = 30,
            instructions = "Make smooth forward and backward circles.",
            coachingCue = "Keep shoulders down and relaxed.",
            badgeType = WarmUpBadgeType.MOBILITY,
        ),
        WarmUpMovement(
            id = "m2",
            name = "Band Pull-Aparts",
            targetMuscleGroup = MuscleGroup.BACK,
            jointMobilityFocus = "Scapular Retraction",
            reps = 15,
            instructions = "Pull resistance band apart to chest level.",
            coachingCue = "Squeeze shoulder blades together.",
            badgeType = WarmUpBadgeType.ACTIVATION,
        ),
        WarmUpMovement(
            id = "m3",
            name = "Cat-Cow Stretch",
            targetMuscleGroup = MuscleGroup.FULL_BODY,
            jointMobilityFocus = "Thoracic Spine Spine Mobility",
            durationSeconds = 45,
            instructions = "Alternate arching and rounding back on all fours.",
            coachingCue = "Breathe rhythmically with each position.",
            badgeType = WarmUpBadgeType.JOINT_PREP,
        ),
    )

    private val sampleRoutine = DynamicWarmUpRoutine(
        id = "routine_1",
        title = "Upper Body Mobility",
        primaryMuscleGroup = MuscleGroup.CHEST,
        estimatedDurationMinutes = 5,
        movementsCount = 3,
        benefitsSummary = "Prepares shoulders and thoracic spine.",
        targetWorkoutName = "Push Day A",
        movements = testMovements,
    )

    @Test
    fun `initial state holder has correct defaults`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)

        assertEquals(3, state.totalMovements)
        assertEquals(0, state.currentIndex)
        assertEquals("Arm Circles", state.currentMovement?.name)
        assertEquals(0, state.completedCount)
        assertEquals(0, state.skippedCount)
        assertEquals(0, state.processedCount)
        assertEquals(3, state.remainingCount)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertFalse(state.isCompleted)
        assertFalse(state.showExitConfirmation)
    }

    @Test
    fun `markCurrentDone advances index and records completed step`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)

        state.markCurrentDone()

        assertEquals(1, state.completedCount)
        assertEquals(0, state.skippedCount)
        assertEquals(1, state.currentIndex)
        assertEquals("Band Pull-Aparts", state.currentMovement?.name)
        assertEquals(2, state.remainingCount)
        assertEquals(1f / 3f, state.progressFraction, 0.001f)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `markCurrentSkipped records skipped step and advances to next incomplete`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)

        state.markCurrentSkipped()

        assertEquals(0, state.completedCount)
        assertEquals(1, state.skippedCount)
        assertEquals(1, state.currentIndex)
        assertEquals("Band Pull-Aparts", state.currentMovement?.name)
        assertEquals(2, state.remainingCount)
        assertEquals(1f / 3f, state.progressFraction, 0.001f)
    }

    @Test
    fun `completing all steps marks routine completed`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)

        state.markCurrentDone() // m1
        state.markCurrentSkipped() // m2
        state.markCurrentDone() // m3

        assertEquals(2, state.completedCount)
        assertEquals(1, state.skippedCount)
        assertEquals(3, state.processedCount)
        assertEquals(0, state.remainingCount)
        assertEquals(1.0f, state.progressFraction, 0.001f)
        assertTrue(state.isCompleted)
    }

    @Test
    fun `goToPrevious moves index back down to 0`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine, initialIndex = 2)

        assertEquals(2, state.currentIndex)

        state.goToPrevious()
        assertEquals(1, state.currentIndex)

        state.goToPrevious()
        assertEquals(0, state.currentIndex)

        state.goToPrevious()
        assertEquals(0, state.currentIndex)
    }

    @Test
    fun `jumpToStep jumps directly to target index`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)

        state.jumpToStep(2)
        assertEquals(2, state.currentIndex)
        assertEquals("Cat-Cow Stretch", state.currentMovement?.name)

        // Invalid index ignored
        state.jumpToStep(99)
        assertEquals(2, state.currentIndex)
    }

    @Test
    fun `requestExit triggers confirmation dialog when incomplete`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)
        var exitCalled = false

        state.requestExit(onDirectExit = { exitCalled = true })

        assertTrue(state.showExitConfirmation)
        assertFalse(exitCalled)

        state.confirmExit(onConfirmed = { exitCalled = true })
        assertFalse(state.showExitConfirmation)
        assertTrue(exitCalled)
    }

    @Test
    fun `requestExit exits directly when all steps processed`() {
        val state = WarmUpPlayerStateHolder(sampleRoutine)
        state.markCurrentDone()
        state.markCurrentDone()
        state.markCurrentDone()

        assertTrue(state.isCompleted)

        var exitCalled = false
        state.requestExit(onDirectExit = { exitCalled = true })

        assertFalse(state.showExitConfirmation)
        assertTrue(exitCalled)
    }

    @Test
    fun `empty routine movements handles progress and navigation safely`() {
        val emptyRoutine = sampleRoutine.copy(movements = emptyList(), movementsCount = 0)
        val state = WarmUpPlayerStateHolder(emptyRoutine)

        assertEquals(0, state.totalMovements)
        assertNull(state.currentMovement)
        assertEquals(0, state.remainingCount)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertFalse(state.isCompleted)

        // Safe call on empty movements
        state.markCurrentDone()
        assertEquals(0, state.currentIndex)
    }
}
