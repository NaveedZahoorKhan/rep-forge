package com.gymtracker.app.domain.usecase

import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import java.util.UUID

object ExerciseCatalog {
    fun stableId(key: String): String = UUID.nameUUIDFromBytes(key.lowercase().toByteArray()).toString()

    fun defaultExercises(): List<ExerciseEntity> = listOf(
        exercise("Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, Difficulty.BEGINNER, "Set shoulder blades, lower the bar under control, and press to lockout.", "Chest, triceps, shoulders"),
        exercise("Incline Dumbbell Press", MuscleGroup.CHEST, Equipment.DUMBBELL, Difficulty.INTERMEDIATE, "Press dumbbells from upper chest level while keeping elbows under wrists.", "Upper chest"),
        exercise("Cable Fly", MuscleGroup.CHEST, Equipment.CABLE, Difficulty.BEGINNER, "Sweep handles together with a soft elbow bend and squeeze the chest.", "Chest isolation"),
        exercise("Pull Up", MuscleGroup.BACK, Equipment.BODYWEIGHT, Difficulty.INTERMEDIATE, "Start from a dead hang and pull chest toward the bar.", "Lats, biceps"),
        exercise("Barbell Row", MuscleGroup.BACK, Equipment.BARBELL, Difficulty.INTERMEDIATE, "Hinge, brace, and row the bar toward the lower ribs.", "Back thickness"),
        exercise("Lat Pulldown", MuscleGroup.BACK, Equipment.CABLE, Difficulty.BEGINNER, "Drive elbows down and bring the bar to upper chest.", "Lats"),
        exercise("Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, Difficulty.INTERMEDIATE, "Brace, press from shoulders, and finish with biceps near ears.", "Shoulders, triceps"),
        exercise("Dumbbell Lateral Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, Difficulty.BEGINNER, "Raise dumbbells to shoulder height with control.", "Side delts"),
        exercise("Face Pull", MuscleGroup.SHOULDERS, Equipment.CABLE, Difficulty.BEGINNER, "Pull rope toward face with elbows high.", "Rear delts"),
        exercise("Barbell Curl", MuscleGroup.BICEPS, Equipment.BARBELL, Difficulty.BEGINNER, "Curl without swinging and lower under control.", "Biceps"),
        exercise("Hammer Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, Difficulty.BEGINNER, "Curl with neutral grip and steady elbows.", "Brachialis"),
        exercise("Close Grip Bench Press", MuscleGroup.TRICEPS, Equipment.BARBELL, Difficulty.INTERMEDIATE, "Bench with a narrower grip and elbows tucked.", "Triceps, chest"),
        exercise("Cable Triceps Pushdown", MuscleGroup.TRICEPS, Equipment.CABLE, Difficulty.BEGINNER, "Pin elbows and extend until arms are straight.", "Triceps"),
        exercise("Back Squat", MuscleGroup.LEGS, Equipment.BARBELL, Difficulty.INTERMEDIATE, "Brace, descend to depth, and drive up through midfoot.", "Quads, glutes"),
        exercise("Front Squat", MuscleGroup.LEGS, Equipment.BARBELL, Difficulty.ADVANCED, "Keep elbows high and torso upright through the squat.", "Quads"),
        exercise("Leg Press", MuscleGroup.LEGS, Equipment.MACHINE, Difficulty.BEGINNER, "Lower sled with control and press without locking knees hard.", "Quads, glutes"),
        exercise("Romanian Deadlift", MuscleGroup.GLUTES, Equipment.BARBELL, Difficulty.INTERMEDIATE, "Hinge with a flat back until hamstrings stretch, then stand tall.", "Hamstrings, glutes"),
        exercise("Hip Thrust", MuscleGroup.GLUTES, Equipment.BARBELL, Difficulty.BEGINNER, "Drive hips up and pause at lockout.", "Glutes"),
        exercise("Deadlift", MuscleGroup.FULL_BODY, Equipment.BARBELL, Difficulty.ADVANCED, "Brace, wedge into the bar, and stand with the bar close.", "Posterior chain"),
        exercise("Walking Lunge", MuscleGroup.LEGS, Equipment.DUMBBELL, Difficulty.BEGINNER, "Step forward, lower under control, and drive through the lead leg.", "Legs, glutes"),
        exercise("Standing Calf Raise", MuscleGroup.LEGS, Equipment.MACHINE, Difficulty.BEGINNER, "Rise onto toes and lower to a full stretch.", "Calves"),
        exercise("Plank", MuscleGroup.ABS, Equipment.BODYWEIGHT, Difficulty.BEGINNER, "Hold a straight body line with ribs down.", "Core"),
        exercise("Hanging Leg Raise", MuscleGroup.ABS, Equipment.BODYWEIGHT, Difficulty.INTERMEDIATE, "Raise legs without swinging and control the descent.", "Abs"),
        exercise("Cable Crunch", MuscleGroup.ABS, Equipment.CABLE, Difficulty.BEGINNER, "Crunch ribs toward pelvis against cable resistance.", "Abs"),
        exercise("Treadmill Run", MuscleGroup.CARDIO, Equipment.CARDIO_MACHINE, Difficulty.BEGINNER, "Run at the planned speed and incline.", "Cardio"),
        exercise("Row Ergometer", MuscleGroup.CARDIO, Equipment.CARDIO_MACHINE, Difficulty.BEGINNER, "Drive with legs, swing torso, then pull handle.", "Cardio, back"),
        exercise("Kettlebell Swing", MuscleGroup.FULL_BODY, Equipment.KETTLEBELL, Difficulty.INTERMEDIATE, "Hinge explosively and float the bell to chest height.", "Power conditioning"),
        exercise("Push Up", MuscleGroup.CHEST, Equipment.BODYWEIGHT, Difficulty.BEGINNER, "Lower chest near the floor and press to a straight body plank.", "Chest, triceps"),
        exercise("Bulgarian Split Squat", MuscleGroup.LEGS, Equipment.DUMBBELL, Difficulty.INTERMEDIATE, "Lower with the rear foot elevated and drive through the front leg.", "Quads, glutes"),
        exercise("Seated Cable Row", MuscleGroup.BACK, Equipment.CABLE, Difficulty.BEGINNER, "Pull handle toward torso and squeeze shoulder blades.", "Back"),
    )

    private fun exercise(
        name: String,
        muscle: MuscleGroup,
        equipment: Equipment,
        difficulty: Difficulty,
        instructions: String,
        cues: String,
    ) = ExerciseEntity(
        id = stableId("exercise:$name"),
        name = name,
        primaryMuscle = muscle,
        secondaryMuscles = cues.split(',').map { it.trim() }.filter { it.isNotBlank() },
        equipment = equipment,
        difficulty = difficulty,
        instructions = instructions,
        cues = cues,
    )
}
