package com.gymtracker.app.presentation.history

import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.ExerciseVolumePoint
import com.gymtracker.app.domain.model.ExerciseVolumeSummary
import com.gymtracker.app.domain.model.PastExerciseDraft
import com.gymtracker.app.domain.model.PastSetDraft
import com.gymtracker.app.domain.model.PastWorkoutLogDraft
import com.gymtracker.app.domain.model.SetDetailSummary
import com.gymtracker.app.domain.model.WorkoutSessionSummary
import com.gymtracker.app.domain.repository.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WorkoutSessionSummary calculates volume and sets correctly`() {
        val session = WorkoutSessionEntity(
            id = "sess_1",
            workoutId = "w_1",
            workoutName = "Upper Body Strength",
            startedAt = 1000000L,
            endedAt = 1003600L,
            status = SessionStatus.COMPLETED,
            totalVolume = 3200.0,
            durationSeconds = 3600,
        )

        val setDetails = listOf(
            SetDetailSummary(setNumber = 1, weightKg = 80.0, reps = 10, volumeKg = 800.0),
            SetDetailSummary(setNumber = 2, weightKg = 85.0, reps = 8, volumeKg = 680.0),
            SetDetailSummary(setNumber = 3, weightKg = 90.0, reps = 6, volumeKg = 540.0),
        )

        val exerciseSummary = ExerciseVolumeSummary(
            exerciseId = "ex_bench",
            exerciseName = "Barbell Bench Press",
            completedSets = 3,
            totalReps = 24,
            maxWeightKg = 90.0,
            totalVolumeKg = 2020.0,
            setsDetail = setDetails,
        )

        val summary = WorkoutSessionSummary(
            session = session,
            completedAt = 1003600L,
            durationFormatted = "1h 0m",
            totalVolumeKg = 3200.0,
            completedSetsCount = 3,
            totalRepsCount = 24,
            exerciseSummaries = listOf(exerciseSummary),
        )

        assertEquals("Upper Body Strength", summary.session.workoutName)
        assertEquals(3200.0, summary.totalVolumeKg, 0.01)
        assertEquals(1, summary.exerciseSummaries.size)
        assertEquals("Barbell Bench Press", summary.exerciseSummaries[0].exerciseName)
        assertEquals(90.0, summary.exerciseSummaries[0].maxWeightKg, 0.01)
        assertEquals(2020.0, summary.exerciseSummaries[0].totalVolumeKg, 0.01)
    }

    @Test
    fun `ExerciseVolumePoint tracks volume across sessions`() {
        val points = listOf(
            ExerciseVolumePoint("s1", "Push Day", 100000L, 2000.0, 80.0, 25, 3),
            ExerciseVolumePoint("s2", "Upper Power", 200000L, 2400.0, 85.0, 28, 3),
            ExerciseVolumePoint("s3", "Chest Hypertrophy", 300000L, 2750.0, 90.0, 30, 4),
        )

        assertEquals(3, points.size)
        assertTrue(points[1].volumeKg > points[0].volumeKg)
        assertTrue(points[2].volumeKg > points[1].volumeKg)
        assertEquals(90.0, points.maxOf { it.maxWeightKg }, 0.01)
        assertEquals(2750.0, points.maxOf { it.volumeKg }, 0.01)
    }

    @Test
    fun `WorkoutHistoryViewModel computes total volume and time filtering`() = runTest(testDispatcher) {
        val mockSummaries = listOf(
            WorkoutSessionSummary(
                session = WorkoutSessionEntity(id = "s1", workoutId = "w1", workoutName = "Push Day", startedAt = System.currentTimeMillis() - 1000000, endedAt = System.currentTimeMillis() - 500000, totalVolume = 4000.0),
                completedAt = System.currentTimeMillis() - 500000,
                durationFormatted = "45m",
                totalVolumeKg = 4000.0,
                completedSetsCount = 10,
                totalRepsCount = 80,
                exerciseSummaries = emptyList(),
            ),
            WorkoutSessionSummary(
                session = WorkoutSessionEntity(id = "s2", workoutId = "w2", workoutName = "Pull Day", startedAt = System.currentTimeMillis() - 2000000, endedAt = System.currentTimeMillis() - 1500000, totalVolume = 5000.0),
                completedAt = System.currentTimeMillis() - 1500000,
                durationFormatted = "50m",
                totalVolumeKg = 5000.0,
                completedSetsCount = 12,
                totalRepsCount = 96,
                exerciseSummaries = emptyList(),
            ),
        )

        val mockExercises = listOf(
            ExerciseEntity(id = "e1", name = "Barbell Bench Press", primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.BARBELL, difficulty = Difficulty.INTERMEDIATE, instructions = "Press bar"),
        )

        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "observeCompletedSessionSummaries" -> flowOf(mockSummaries)
                "observeExercises" -> flowOf(mockExercises)
                "observeExerciseVolumeOverTime" -> flowOf(emptyList<ExerciseVolumePoint>())
                else -> null
            }
        } as GymRepository

        val viewModel = WorkoutHistoryViewModel(dummyRepo)
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect { }
        }
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.summaries.size)
        assertEquals(9000.0, state.totalVolumeAllTimeKg, 0.01)
        assertEquals(4500.0, state.averageVolumePerWorkoutKg, 0.01)
        assertEquals(2, state.totalWorkoutsCount)

        // Filter by search query
        viewModel.setSearchQuery("Push")
        runCurrent()
        assertEquals(1, viewModel.uiState.value.filteredSummaries.size)
        assertEquals("Push Day", viewModel.uiState.value.filteredSummaries[0].session.workoutName)
        collectJob.cancel()
    }
}
