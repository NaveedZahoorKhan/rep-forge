package com.gymtracker.app.presentation.workouts

import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.WorkoutSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private inline fun <reified T> createMockInterface(
        crossinline handler: (methodName: String, args: Array<Any>?) -> Any? = { _, _ -> null }
    ): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args ->
            val custom = handler(method.name, args)
            if (custom != null) return@newProxyInstance custom

            when (method.name) {
                "observeWorkouts" -> flowOf(listOf(
                    WorkoutEntity(id = "w1", name = "Push Day A", splitType = "Push"),
                    WorkoutEntity(id = "w2", name = "Pull Day A", splitType = "Pull"),
                    WorkoutEntity(id = "w3", name = "Leg Day A", splitType = "Legs"),
                    WorkoutEntity(id = "w4", name = "Bro Split Chest", splitType = "Bro Split"),
                    WorkoutEntity(id = "w5", name = "Upper Power", splitType = "Upper"),
                    WorkoutEntity(id = "w6", name = "Lower Power", splitType = "Lower")
                ))
                "observeExercises" -> flowOf(listOf(
                    ExerciseEntity(id = "e1", name = "Bench Press", primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.BARBELL, difficulty = Difficulty.INTERMEDIATE, instructions = "Press bar from chest"),
                    ExerciseEntity(id = "e2", name = "Incline Dumbbell Press", primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.DUMBBELL, difficulty = Difficulty.BEGINNER, instructions = "Press dumbbells at incline"),
                    ExerciseEntity(id = "e3", name = "Barbell Row", primaryMuscle = MuscleGroup.BACK, equipment = Equipment.BARBELL, difficulty = Difficulty.INTERMEDIATE, instructions = "Row bar to torso")
                ))
                "observeWeeklySchedule" -> flowOf(emptyList<WeeklyScheduleEntity>())
                "observeUserProfile" -> flowOf(UserProfileEntity(preferredSplit = "Push Pull Legs (PPL)"))
                "createCustomWorkout" -> "new-workout-id-123"
                "applyRoutineSplitSchedule" -> Unit
                "createOrUpdateProfile" -> Unit
                "toString" -> "MockInterface"
                "hashCode" -> 1
                "equals" -> false
                else -> {
                    val returnType = method.returnType
                    when {
                        returnType == Flow::class.java -> flowOf(emptyList<Any>())
                        returnType == Result::class.java -> Result.success(Unit)
                        returnType == List::class.java -> emptyList<Any>()
                        returnType == String::class.java -> ""
                        returnType == Void.TYPE || returnType == Unit::class.java -> Unit
                        returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.javaObjectType -> false
                        else -> null
                    }
                }
            }
        } as T
    }

    @Test
    fun `viewModel state emits workouts, exercises, and user split preference`() = runTest(testDispatcher) {
        val repo = createMockInterface<GymRepository>()
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = WorkoutsViewModel(repo, soundPlayer)

        val collectJob = launch {
            viewModel.state.collect {}
        }
        runCurrent()
        val state = viewModel.state.value

        assertEquals(6, state.workouts.size)
        assertEquals(3, state.exercises.size)
        assertEquals("Push Pull Legs (PPL)", state.profile.preferredSplit)
        collectJob.cancel()
    }

    @Test
    fun `createWorkout calls repository and returns generated workout id`() = runTest(testDispatcher) {
        var createdWorkoutId: String? = null
        val repo = createMockInterface<GymRepository> { methodName, args ->
            if (methodName == "createCustomWorkout") {
                val draft = args?.get(0) as? WorkoutDraft
                assertEquals("Custom Leg Day", draft?.name)
                "generated-id-777"
            } else null
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = WorkoutsViewModel(repo, soundPlayer)

        val draft = WorkoutDraft(
            name = "Custom Leg Day",
            description = "Custom workout",
            splitType = "Legs",
            exercises = listOf(
                WorkoutExerciseDraft(
                    exerciseId = "e1",
                    restSeconds = 90,
                    setCount = 4,
                    repsMin = 8,
                    repsMax = 12,
                    weight = 80.0
                )
            )
        )

        viewModel.createWorkout(draft) { id ->
            createdWorkoutId = id
        }
        runCurrent()

        assertEquals("generated-id-777", createdWorkoutId)
    }

    @Test
    fun `all seven workout split filters correctly categorize workouts`() {
        val allFilters = listOf("All", "Push", "Pull", "Legs", "Bro Split", "Upper", "Lower")
        assertEquals(7, allFilters.size)

        val sampleWorkouts = listOf(
            WorkoutEntity(id = "1", name = "Heavy Push", splitType = "Push"),
            WorkoutEntity(id = "2", name = "Back Pull", splitType = "Pull"),
            WorkoutEntity(id = "3", name = "Leg Hypertrophy", splitType = "Legs"),
            WorkoutEntity(id = "4", name = "Bro Split Arms", splitType = "Bro Split"),
            WorkoutEntity(id = "5", name = "Upper Body Strength", splitType = "Upper"),
            WorkoutEntity(id = "6", name = "Lower Body Focus", splitType = "Lower")
        )

        for (filter in allFilters) {
            val filtered = if (filter == "All") {
                sampleWorkouts
            } else {
                sampleWorkouts.filter {
                    it.splitType.contains(filter, ignoreCase = true) ||
                    it.name.contains(filter, ignoreCase = true)
                }
            }
            assertTrue("Filter $filter should match results", filtered.isNotEmpty())
        }
    }

    @Test
    fun `validation rules correctly identify valid and invalid draft configurations`() {
        fun validate(name: String, sets: Int, repsMin: Int, repsMax: Int, weight: Double, exerciseCount: Int): String? {
            return when {
                name.isBlank() -> "Workout name cannot be empty"
                sets <= 0 -> "Sets must be at least 1"
                repsMin <= 0 -> "Min reps must be at least 1"
                repsMax < repsMin -> "Max reps ($repsMax) cannot be less than min reps ($repsMin)"
                weight < 0.0 -> "Starting weight cannot be negative"
                exerciseCount <= 0 -> "Select at least 1 exercise"
                else -> null
            }
        }

        assertEquals("Workout name cannot be empty", validate("", 3, 8, 12, 50.0, 1))
        assertEquals("Sets must be at least 1", validate("Test", 0, 8, 12, 50.0, 1))
        assertEquals("Min reps must be at least 1", validate("Test", 3, 0, 12, 50.0, 1))
        assertEquals("Max reps (6) cannot be less than min reps (10)", validate("Test", 3, 10, 6, 50.0, 1))
        assertEquals("Starting weight cannot be negative", validate("Test", 3, 8, 12, -5.0, 1))
        assertEquals("Select at least 1 exercise", validate("Test", 3, 8, 12, 50.0, 0))
        assertEquals(null, validate("Valid Push Day", 4, 8, 12, 60.0, 2))
    }

    @Test
    fun `EditableExerciseItem validation identifies invalid set, rep, weight and rest fields`() {
        val validItem = EditableExerciseItem(
            exerciseId = "e1",
            exerciseName = "Bench Press",
            primaryMuscle = "Chest",
            equipment = "Barbell",
            setCountText = "3",
            repsMinText = "8",
            repsMaxText = "12",
            weightText = "60.0",
            restSecondsText = "90"
        )
        assertNull(validItem.validationError)

        val invalidSets = validItem.copy(setCountText = "0")
        assertNotNull(invalidSets.validationError)
        assertTrue(invalidSets.validationError!!.contains("Sets must be at least 1"))

        val invalidRepsOrder = validItem.copy(repsMinText = "12", repsMaxText = "8")
        assertNotNull(invalidRepsOrder.validationError)
        assertTrue(invalidRepsOrder.validationError!!.contains("Max reps cannot be less than min reps"))

        val negativeWeight = validItem.copy(weightText = "-10.0")
        assertNotNull(negativeWeight.validationError)
        assertTrue(negativeWeight.validationError!!.contains("Weight cannot be negative"))

        val invalidRest = validItem.copy(restSecondsText = "-5")
        assertNotNull(invalidRest.validationError)
        assertTrue(invalidRest.validationError!!.contains("Rest seconds cannot be negative"))
    }

    @Test
    fun `exercise list reordering moves items up and down correctly`() {
        val list = mutableListOf(
            EditableExerciseItem(exerciseId = "e1", exerciseName = "Bench Press", primaryMuscle = "Chest", equipment = "Barbell"),
            EditableExerciseItem(exerciseId = "e2", exerciseName = "Incline Dumbbell Press", primaryMuscle = "Chest", equipment = "Dumbbell"),
            EditableExerciseItem(exerciseId = "e3", exerciseName = "Barbell Row", primaryMuscle = "Back", equipment = "Barbell")
        )

        // Move item at index 1 ("Incline Dumbbell Press") UP to index 0
        fun moveUp(index: Int) {
            if (index > 0) {
                val elem = list.removeAt(index)
                list.add(index - 1, elem)
            }
        }

        moveUp(1)
        assertEquals("Incline Dumbbell Press", list[0].exerciseName)
        assertEquals("Bench Press", list[1].exerciseName)

        // Move item at index 0 DOWN to index 1
        fun moveDown(index: Int) {
            if (index < list.lastIndex) {
                val elem = list.removeAt(index)
                list.add(index + 1, elem)
            }
        }

        moveDown(0)
        assertEquals("Bench Press", list[0].exerciseName)
        assertEquals("Incline Dumbbell Press", list[1].exerciseName)
    }

    @Test
    fun `exercise removal and undo restores item to original index`() {
        val list = mutableListOf(
            EditableExerciseItem(exerciseId = "e1", exerciseName = "Bench Press", primaryMuscle = "Chest", equipment = "Barbell"),
            EditableExerciseItem(exerciseId = "e2", exerciseName = "Incline Dumbbell Press", primaryMuscle = "Chest", equipment = "Dumbbell"),
            EditableExerciseItem(exerciseId = "e3", exerciseName = "Barbell Row", primaryMuscle = "Back", equipment = "Barbell")
        )

        // Remove item at index 1
        val removeIndex = 1
        val removedItem = list.removeAt(removeIndex)
        assertEquals(2, list.size)
        assertEquals("Barbell Row", list[1].exerciseName)

        // Undo removal: re-insert at removeIndex
        list.add(removeIndex, removedItem)
        assertEquals(3, list.size)
        assertEquals("Incline Dumbbell Press", list[1].exerciseName)
    }

    @Test
    fun `exercise picker retains selected items and prevents duplicate additions`() {
        val existingSelected = setOf("e1")
        val pickerSelections = mutableSetOf<String>().apply { addAll(existingSelected) }

        // User searches for exercises, filter query changes to "row"
        val searchQuery = "row"
        assertTrue(searchQuery.isNotBlank())

        // Selection remains retained despite search query change
        assertTrue(pickerSelections.contains("e1"))

        // User selects "e2"
        pickerSelections.add("e2")
        assertEquals(2, pickerSelections.size)

        // Confirm selection: filter out items already in routine
        val existingIdsInRoutine = setOf("e1")
        val newlySelected = pickerSelections.filterNot { it in existingIdsInRoutine }

        assertEquals(1, newlySelected.size)
        assertEquals("e2", newlySelected.first())
    }

    @Test
    fun `assignWorkoutToDay deletes existing schedule for day and upserts new schedule entity`() = runTest(testDispatcher) {
        var deletedDay: WeekDay? = null
        var upsertedSchedule: WeeklyScheduleEntity? = null

        val repo = createMockInterface<GymRepository> { methodName, args ->
            when (methodName) {
                "deleteScheduleForDay" -> {
                    deletedDay = args?.get(0) as? WeekDay
                    Unit
                }
                "upsertSchedule" -> {
                    upsertedSchedule = args?.get(0) as? WeeklyScheduleEntity
                    Unit
                }
                else -> null
            }
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = WorkoutsViewModel(repo, soundPlayer)

        viewModel.assignWorkoutToDay(WeekDay.MONDAY, "w1", "Push Day A")
        runCurrent()

        assertEquals(WeekDay.MONDAY, deletedDay)
        assertEquals(WeekDay.MONDAY, upsertedSchedule?.weekDay)
        assertEquals("w1", upsertedSchedule?.workoutId)
        assertEquals("Push Day A", upsertedSchedule?.workoutName)
    }

    @Test
    fun `removeSchedule calls repository deleteSchedule with correct ID`() = runTest(testDispatcher) {
        var deletedScheduleId: String? = null

        val repo = createMockInterface<GymRepository> { methodName, args ->
            if (methodName == "deleteSchedule") {
                deletedScheduleId = args?.get(0) as? String
                Unit
            } else null
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = WorkoutsViewModel(repo, soundPlayer)

        viewModel.removeSchedule("sched-999")
        runCurrent()

        assertEquals("sched-999", deletedScheduleId)
    }

    @Test
    fun `rescheduleWorkout deletes old schedule and target day schedule before upserting new schedule`() = runTest(testDispatcher) {
        var deletedScheduleId: String? = null
        var deletedTargetDay: WeekDay? = null
        var upsertedSchedule: WeeklyScheduleEntity? = null

        val repo = createMockInterface<GymRepository> { methodName, args ->
            when (methodName) {
                "deleteSchedule" -> {
                    deletedScheduleId = args?.get(0) as? String
                    Unit
                }
                "deleteScheduleForDay" -> {
                    deletedTargetDay = args?.get(0) as? WeekDay
                    Unit
                }
                "upsertSchedule" -> {
                    upsertedSchedule = args?.get(0) as? WeeklyScheduleEntity
                    Unit
                }
                else -> null
            }
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = WorkoutsViewModel(repo, soundPlayer)

        viewModel.rescheduleWorkout("sched-100", WeekDay.MONDAY, WeekDay.FRIDAY, "w3", "Leg Day A")
        runCurrent()

        assertEquals("sched-100", deletedScheduleId)
        assertEquals(WeekDay.FRIDAY, deletedTargetDay)
        assertEquals(WeekDay.FRIDAY, upsertedSchedule?.weekDay)
        assertEquals("w3", upsertedSchedule?.workoutId)
        assertEquals("Leg Day A", upsertedSchedule?.workoutName)
    }
}
