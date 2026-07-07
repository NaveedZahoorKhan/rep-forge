package com.gymtracker.app.domain.usecase

import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.PeriodizationType
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import java.util.UUID

data class SeedWorkoutGraph(
    val workouts: List<WorkoutEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
    val setTemplates: List<SetTemplateEntity>,
    val schedules: List<WeeklyScheduleEntity>,
)

object WorkoutTemplateCatalog {
    fun defaultTemplates(exercises: List<ExerciseEntity>): SeedWorkoutGraph {
        val byName = exercises.associateBy { it.name }
        val workouts = mutableListOf<WorkoutEntity>()
        val workoutExercises = mutableListOf<WorkoutExerciseEntity>()
        val sets = mutableListOf<SetTemplateEntity>()

        fun stableId(key: String): String = UUID.nameUUIDFromBytes(key.lowercase().toByteArray()).toString()

        fun addWorkout(
            name: String,
            split: String,
            description: String,
            prescriptions: List<Prescription>,
        ) {
            val workoutId = stableId("template:$name")
            workouts += WorkoutEntity(
                id = workoutId,
                name = name,
                description = description,
                splitType = split,
                isTemplate = true,
            )
            prescriptions.forEachIndexed { exerciseIndex, prescription ->
                val exercise = byName[prescription.exerciseName] ?: return@forEachIndexed
                val workoutExerciseId = stableId("$name:${prescription.exerciseName}:$exerciseIndex")
                workoutExercises += WorkoutExerciseEntity(
                    id = workoutExerciseId,
                    workoutId = workoutId,
                    exerciseId = exercise.id,
                    orderIndex = exerciseIndex,
                    notes = prescription.notes,
                    restSeconds = prescription.restSeconds,
                    supersetGroup = prescription.supersetGroup,
                    amrapLastSet = prescription.amrapLastSet,
                )
                repeat(prescription.sets) { setIndex ->
                    val type = when {
                        setIndex == 0 && prescription.warmup -> SetType.WARM_UP
                        prescription.failureLastSet && setIndex == prescription.sets - 1 -> SetType.FAILURE
                        prescription.dropLastSet && setIndex == prescription.sets - 1 -> SetType.DROP_SET
                        else -> SetType.NORMAL
                    }
                    sets += SetTemplateEntity(
                        id = stableId("$workoutExerciseId:set:$setIndex"),
                        workoutExerciseId = workoutExerciseId,
                        orderIndex = setIndex,
                        targetRepsMin = prescription.repsMin,
                        targetRepsMax = prescription.repsMax,
                        targetWeight = prescription.weight,
                        setType = type,
                        targetRpe = prescription.rpe,
                        targetRir = prescription.rir,
                    )
                }
            }
        }

        addWorkout(
            "Push Day",
            "Push/Pull/Legs",
            "Chest, shoulders, and triceps with a strength-first compound lift.",
            listOf(
                p("Barbell Bench Press", 4, 5, 8, 90.0, 120, warmup = true),
                p("Overhead Press", 3, 6, 10, 45.0, 120),
                p("Incline Dumbbell Press", 3, 8, 12, 30.0, 90),
                p("Dumbbell Lateral Raise", 3, 12, 20, 10.0, 60, failureLastSet = true),
                p("Cable Triceps Pushdown", 3, 10, 15, 25.0, 60, dropLastSet = true),
            ),
        )
        addWorkout(
            "Pull Day",
            "Push/Pull/Legs",
            "Back and biceps with vertical and horizontal pulling.",
            listOf(
                p("Deadlift", 3, 3, 5, 120.0, 180, warmup = true),
                p("Pull Up", 4, 6, 10, 0.0, 120, failureLastSet = true),
                p("Barbell Row", 4, 6, 10, 70.0, 120),
                p("Face Pull", 3, 12, 20, 15.0, 60),
                p("Barbell Curl", 3, 8, 12, 30.0, 60),
            ),
        )
        addWorkout(
            "Leg Day",
            "Push/Pull/Legs",
            "Squat-centered lower-body training.",
            listOf(
                p("Back Squat", 5, 5, 8, 100.0, 180, warmup = true),
                p("Romanian Deadlift", 4, 6, 10, 80.0, 120),
                p("Leg Press", 3, 10, 15, 180.0, 90),
                p("Walking Lunge", 3, 10, 12, 20.0, 90),
                p("Standing Calf Raise", 4, 12, 20, 60.0, 60),
            ),
        )
        addWorkout(
            "Upper Body",
            "Upper/Lower",
            "Balanced upper day for pressing and pulling.",
            listOf(
                p("Barbell Bench Press", 4, 6, 8, 85.0, 150),
                p("Barbell Row", 4, 6, 10, 70.0, 120),
                p("Overhead Press", 3, 6, 10, 45.0, 120),
                p("Lat Pulldown", 3, 8, 12, 55.0, 90),
                p("Cable Triceps Pushdown", 2, 12, 15, 25.0, 60, supersetGroup = "A"),
                p("Hammer Curl", 2, 10, 15, 15.0, 60, supersetGroup = "A"),
            ),
        )
        addWorkout(
            "Lower Body",
            "Upper/Lower",
            "Lower day with squat, hinge, and unilateral work.",
            listOf(
                p("Back Squat", 4, 5, 8, 95.0, 180),
                p("Romanian Deadlift", 4, 8, 10, 75.0, 120),
                p("Bulgarian Split Squat", 3, 8, 12, 20.0, 90),
                p("Hip Thrust", 3, 8, 12, 100.0, 120),
                p("Hanging Leg Raise", 3, 8, 15, 0.0, 60),
            ),
        )
        addWorkout(
            "Full Body Strength",
            "Full Body",
            "Three-day full-body strength template.",
            listOf(
                p("Back Squat", 3, 5, 5, 100.0, 180),
                p("Barbell Bench Press", 3, 5, 5, 85.0, 180),
                p("Barbell Row", 3, 6, 8, 70.0, 120),
                p("Plank", 3, 45, 60, 0.0, 60),
            ),
        )
        addWorkout(
            "Bro Split Chest",
            "Bro Split",
            "High-volume chest day with pressing and fly patterns.",
            listOf(
                p("Barbell Bench Press", 5, 5, 10, 85.0, 150),
                p("Incline Dumbbell Press", 4, 8, 12, 30.0, 90),
                p("Cable Fly", 4, 12, 20, 15.0, 60, dropLastSet = true),
                p("Push Up", 3, 12, 25, 0.0, 45, failureLastSet = true),
            ),
        )
        addWorkout(
            "Arnold Split Torso",
            "Arnold Split",
            "Chest and back supersets inspired by classic high-frequency training.",
            listOf(
                p("Barbell Bench Press", 4, 8, 12, 75.0, 90, supersetGroup = "A"),
                p("Pull Up", 4, 8, 12, 0.0, 90, supersetGroup = "A"),
                p("Incline Dumbbell Press", 4, 10, 12, 26.0, 90, supersetGroup = "B"),
                p("Seated Cable Row", 4, 10, 12, 55.0, 90, supersetGroup = "B"),
                p("Cable Fly", 3, 12, 20, 15.0, 60, dropLastSet = true),
            ),
        )
        addWorkout(
            "PHUL Power Upper",
            "PHUL",
            "Power hypertrophy upper day.",
            listOf(
                p("Barbell Bench Press", 4, 3, 5, 90.0, 180),
                p("Barbell Row", 4, 3, 5, 80.0, 180),
                p("Overhead Press", 3, 5, 8, 50.0, 150),
                p("Pull Up", 3, 6, 10, 0.0, 120),
            ),
        )
        addWorkout(
            "PHAT Lower Power",
            "PHAT",
            "Power lower day with low reps and accessory volume.",
            listOf(
                p("Back Squat", 3, 3, 5, 110.0, 180),
                p("Deadlift", 3, 3, 5, 130.0, 180),
                p("Leg Press", 3, 10, 15, 180.0, 120),
                p("Standing Calf Raise", 4, 10, 15, 70.0, 60),
            ),
        )
        addWorkout(
            "StrongLifts 5x5 A",
            "5x5 StrongLifts",
            "Classic 5x5 A day.",
            listOf(
                p("Back Squat", 5, 5, 5, 100.0, 180),
                p("Barbell Bench Press", 5, 5, 5, 80.0, 180),
                p("Barbell Row", 5, 5, 5, 70.0, 180, amrapLastSet = true),
            ),
        )
        addWorkout(
            "Starting Strength A",
            "Starting Strength",
            "Novice linear progression A day.",
            listOf(
                p("Back Squat", 3, 5, 5, 95.0, 180),
                p("Barbell Bench Press", 3, 5, 5, 80.0, 180),
                p("Deadlift", 1, 5, 5, 120.0, 180),
            ),
        )

        val schedules = listOf(
            WeeklyScheduleEntity(
                id = stableId("schedule:monday"),
                weekDay = WeekDay.MONDAY,
                workoutId = stableId("template:Push Day"),
                workoutName = "Push Day",
                periodizationType = PeriodizationType.LINEAR,
                reminderTimeMinutes = 18 * 60,
            ),
            WeeklyScheduleEntity(
                id = stableId("schedule:wednesday"),
                weekDay = WeekDay.WEDNESDAY,
                workoutId = stableId("template:Pull Day"),
                workoutName = "Pull Day",
                periodizationType = PeriodizationType.DUP,
                reminderTimeMinutes = 18 * 60,
            ),
            WeeklyScheduleEntity(
                id = stableId("schedule:friday"),
                weekDay = WeekDay.FRIDAY,
                workoutId = stableId("template:Leg Day"),
                workoutName = "Leg Day",
                periodizationType = PeriodizationType.BLOCK,
                reminderTimeMinutes = 18 * 60,
            ),
        )

        return SeedWorkoutGraph(workouts, workoutExercises, sets, schedules)
    }

    private data class Prescription(
        val exerciseName: String,
        val sets: Int,
        val repsMin: Int,
        val repsMax: Int,
        val weight: Double,
        val restSeconds: Int,
        val warmup: Boolean = false,
        val failureLastSet: Boolean = false,
        val dropLastSet: Boolean = false,
        val supersetGroup: String? = null,
        val amrapLastSet: Boolean = false,
        val rpe: Double? = null,
        val rir: Int? = null,
        val notes: String = "",
    )

    private fun p(
        exerciseName: String,
        sets: Int,
        repsMin: Int,
        repsMax: Int,
        weight: Double,
        restSeconds: Int,
        warmup: Boolean = false,
        failureLastSet: Boolean = false,
        dropLastSet: Boolean = false,
        supersetGroup: String? = null,
        amrapLastSet: Boolean = false,
        rpe: Double? = null,
        rir: Int? = null,
        notes: String = "",
    ) = Prescription(
        exerciseName = exerciseName,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax,
        weight = weight,
        restSeconds = restSeconds,
        warmup = warmup,
        failureLastSet = failureLastSet,
        dropLastSet = dropLastSet,
        supersetGroup = supersetGroup,
        amrapLastSet = amrapLastSet,
        rpe = rpe,
        rir = rir,
        notes = notes,
    )
}
