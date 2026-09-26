package com.gymtracker.app.data.local

import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ManualWorkoutEntityAndDaoTest {

    @Test
    fun `ManualWorkoutSessionEntity instantiates with correct defaults and metadata`() {
        val sessionId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val session = ManualWorkoutSessionEntity(
            id = sessionId,
            workoutName = "Leg Day Heavy",
            sessionDate = timestamp,
            durationMinutes = 60,
            notes = "Felt strong on squats",
            rating = 5,
            totalVolumeKg = 4250.0,
            totalReps = 65,
            totalSets = 8,
        )

        assertEquals(sessionId, session.id)
        assertEquals("Leg Day Heavy", session.workoutName)
        assertEquals(timestamp, session.sessionDate)
        assertEquals(60, session.durationMinutes)
        assertEquals("Felt strong on squats", session.notes)
        assertEquals(5, session.rating)
        assertEquals(4250.0, session.totalVolumeKg, 0.01)
        assertEquals(65, session.totalReps)
        assertEquals(8, session.totalSets)
    }

    @Test
    fun `ManualWorkoutSetEntity stores exercise, set number, reps, and weight`() {
        val setId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        val set = ManualWorkoutSetEntity(
            id = setId,
            sessionId = sessionId,
            exerciseName = "Barbell Back Squat",
            exerciseId = "ex_squat",
            muscleGroup = "Legs",
            setNumber = 1,
            reps = 8,
            weightKg = 100.0,
            rpe = 8.5,
            isWarmup = false,
            notes = "Deep squat",
        )

        assertEquals(setId, set.id)
        assertEquals(sessionId, set.sessionId)
        assertEquals("Barbell Back Squat", set.exerciseName)
        assertEquals("ex_squat", set.exerciseId)
        assertEquals(1, set.setNumber)
        assertEquals(8, set.reps)
        assertEquals(100.0, set.weightKg, 0.01)
        assertEquals(8.5, set.rpe)
        assertEquals(false, set.isWarmup)
    }

    @Test
    fun `ManualWorkoutSessionWithSets groups exercises and calculates volume and reps correctly`() {
        val sessionId = "session_test_123"
        val session = ManualWorkoutSessionEntity(
            id = sessionId,
            workoutName = "Push Day",
            durationMinutes = 45,
        )

        val sets = listOf(
            ManualWorkoutSetEntity(
                sessionId = sessionId,
                exerciseName = "Bench Press",
                setNumber = 1,
                reps = 10,
                weightKg = 80.0,
            ),
            ManualWorkoutSetEntity(
                sessionId = sessionId,
                exerciseName = "Bench Press",
                setNumber = 2,
                reps = 8,
                weightKg = 85.0,
            ),
            ManualWorkoutSetEntity(
                sessionId = sessionId,
                exerciseName = "Incline Dumbbell Press",
                setNumber = 1,
                reps = 12,
                weightKg = 24.0,
            ),
            ManualWorkoutSetEntity(
                sessionId = sessionId,
                exerciseName = "Tricep Pushdown",
                setNumber = 1,
                reps = 15,
                weightKg = 30.0,
            ),
        )

        val sessionWithSets = ManualWorkoutSessionWithSets(
            session = session,
            sets = sets,
        )

        // Verify exercises are grouped properly
        val grouped = sessionWithSets.exercisesGrouped
        assertEquals(3, grouped.size)
        assertTrue(grouped.containsKey("Bench Press"))
        assertTrue(grouped.containsKey("Incline Dumbbell Press"))
        assertTrue(grouped.containsKey("Tricep Pushdown"))
        assertEquals(2, grouped["Bench Press"]?.size)

        // Verify distinct exercises count
        assertEquals(3, sessionWithSets.distinctExercisesCount)

        // Volume: (10 * 80) + (8 * 85) + (12 * 24) + (15 * 30) = 800 + 680 + 288 + 450 = 2218.0
        val expectedVolume = (10 * 80.0) + (8 * 85.0) + (12 * 24.0) + (15 * 30.0)
        assertEquals(expectedVolume, sessionWithSets.calculateCalculatedVolumeKg(), 0.01)

        // Total Reps: 10 + 8 + 12 + 15 = 45
        assertEquals(45, sessionWithSets.calculateCalculatedReps())
    }
}
