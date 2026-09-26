package com.gymtracker.app.presentation.active

import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutTimerTest {

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
    fun `initial timer state is idle and zero`() {
        val (viewModel, _) = createViewModel()

        assertEquals(0, viewModel.restRemaining)
        assertEquals(0, viewModel.restTotal)
        assertFalse(viewModel.isTimerRunning)
        assertFalse(viewModel.isTimerPaused)
        assertFalse(viewModel.isTimerFinished)
        assertEquals("", viewModel.timerExercise)
    }

    @Test
    fun `startRest sets total, remaining, and running state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(90, "Incline Dumbbell Press")
        runCurrent()

        assertEquals(90, viewModel.restRemaining)
        assertEquals(90, viewModel.restTotal)
        assertTrue(viewModel.isTimerRunning)
        assertFalse(viewModel.isTimerPaused)
        assertFalse(viewModel.isTimerFinished)
        assertEquals("Incline Dumbbell Press", viewModel.timerExercise)

        // Advance 2 seconds
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(88, viewModel.restRemaining)
    }

    @Test
    fun `pauseTimer stops decrementing countdown and marks isTimerPaused true`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(60, "Barbell Squat")
        runCurrent()
        advanceTimeBy(3000)
        runCurrent()
        assertEquals(57, viewModel.restRemaining)

        viewModel.pauseTimer()
        assertTrue(viewModel.isTimerPaused)
        assertTrue(viewModel.isTimerRunning)

        // Advancing time while paused should NOT decrease remaining seconds
        advanceTimeBy(5000)
        runCurrent()
        assertEquals(57, viewModel.restRemaining)

        // Resume timer
        viewModel.resumeTimer()
        runCurrent()
        assertFalse(viewModel.isTimerPaused)

        // Advancing time now resumes decrementing
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(55, viewModel.restRemaining)
    }

    @Test
    fun `togglePauseTimer toggles between pause and resume`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(45, "Pull Ups")
        runCurrent()
        assertFalse(viewModel.isTimerPaused)

        viewModel.togglePauseTimer()
        assertTrue(viewModel.isTimerPaused)

        viewModel.togglePauseTimer()
        assertFalse(viewModel.isTimerPaused)
    }

    @Test
    fun `addRestTime increases remaining and total duration`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(60, "Deadlift")
        runCurrent()
        viewModel.addRestTime(30)

        assertEquals(90, viewModel.restRemaining)
        assertEquals(90, viewModel.restTotal)
    }

    @Test
    fun `subtractRestTime decreases remaining seconds but stays above minimum`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(60, "Bench Press")
        runCurrent()
        viewModel.subtractRestTime(15)

        assertEquals(45, viewModel.restRemaining)

        viewModel.subtractRestTime(100)
        assertEquals(1, viewModel.restRemaining)
    }

    @Test
    fun `skipTimer completely clears timer state`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(90, "Overhead Press")
        runCurrent()
        viewModel.skipTimer()
        runCurrent()

        assertEquals(0, viewModel.restRemaining)
        assertEquals(0, viewModel.restTotal)
        assertFalse(viewModel.isTimerRunning)
        assertFalse(viewModel.isTimerPaused)
        assertFalse(viewModel.isTimerFinished)
        assertEquals("", viewModel.timerExercise)
    }

    @Test
    fun `restartTimer restarts the previous total interval`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.startRest(120, "Leg Press")
        runCurrent()
        advanceTimeBy(30000)
        runCurrent()
        assertEquals(90, viewModel.restRemaining)

        viewModel.restartTimer()
        runCurrent()
        assertEquals(120, viewModel.restRemaining)
        assertEquals(120, viewModel.restTotal)
        assertTrue(viewModel.isTimerRunning)
    }

    private fun createViewModel(): Pair<ActiveWorkoutViewModel, GymRepository> {
        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java)
        ) { _, method, args ->
            when (method.name) {
                "observeUserProfile" -> flowOf(UserProfileEntity(displayName = "Athlete", soundEnabled = false, vibrationEnabled = false))
                "observeActiveSession" -> flowOf(null)
                "observeSessionSets" -> flowOf(emptyList<PerformedSetEntity>())
                else -> null
            }
        } as GymRepository

        val helperInstance = object : NotificationHelper {
            override fun ensureChannels() {}
            override fun showRestComplete(exerciseName: String, sound: Boolean, vibration: Boolean) {}
            override fun showWorkoutReminder(title: String, body: String) {}
        }

        val soundPlayerInstance = object : com.gymtracker.app.notification.WorkoutSoundPlayer {
            override fun playRestCompleteSound() {}
            override fun vibrateRestComplete() {}
        }

        val vm = ActiveWorkoutViewModel(dummyRepo, helperInstance, soundPlayerInstance)
        return Pair(vm, dummyRepo)
    }
}
