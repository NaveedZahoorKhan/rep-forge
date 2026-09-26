package com.gymtracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.dao.ManualWorkoutDao
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.GeminiSyncReportEntity
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.UserHealthProfileEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetTemplateEntity::class,
        WorkoutSessionEntity::class,
        PerformedSetEntity::class,
        PersonalRecordEntity::class,
        BodyMeasurementEntity::class,
        ProgressPhotoEntity::class,
        NutritionLogEntity::class,
        MealEntity::class,
        WaterLogEntity::class,
        WeightLogEntity::class,
        UserProfileEntity::class,
        WeeklyScheduleEntity::class,
        ReminderEntity::class,
        UserHealthProfileEntity::class,
        GeminiSyncReportEntity::class,
        GymEquipmentEntity::class,
        ManualWorkoutSessionEntity::class,
        ManualWorkoutSetEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GymTrackerDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao
    abstract fun manualWorkoutDao(): ManualWorkoutDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE user_profile ADD COLUMN analyticsOptIn INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `user_health_profiles` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `primaryGoal` TEXT NOT NULL,
                            `experienceLevel` TEXT NOT NULL,
                            `healthConditions` TEXT NOT NULL,
                            `injuriesAndLimitations` TEXT NOT NULL,
                            `targetMuscles` TEXT NOT NULL,
                            `preferredDurationMinutes` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `gemini_sync_reports` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `syncedAt` INTEGER NOT NULL,
                            `overallSummary` TEXT NOT NULL,
                            `consistencyScore` TEXT NOT NULL,
                            `volumeAnalysis` TEXT NOT NULL,
                            `healthReview` TEXT NOT NULL,
                            `nutritionAudit` TEXT NOT NULL,
                            `actionableSuggestionsJson` TEXT NOT NULL,
                            `motivationalQuote` TEXT NOT NULL,
                            `workloadSummary` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_gemini_sync_reports_syncedAt` ON `gemini_sync_reports` (`syncedAt`)")
                }
            }

        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `gym_equipments` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `photoUri` TEXT,
                            `notes` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_equipments_name` ON `gym_equipments` (`name`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_equipments_createdAt` ON `gym_equipments` (`createdAt`)")
                }
            }

        val MIGRATION_4_5: Migration =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `gym_equipments` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `photoUri` TEXT,
                            `notes` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_equipments_name` ON `gym_equipments` (`name`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_equipments_createdAt` ON `gym_equipments` (`createdAt`)")
                }
            }

        val MIGRATION_5_6: Migration =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `manual_workout_sessions` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `workoutName` TEXT NOT NULL,
                            `sessionDate` INTEGER NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL,
                            `rating` INTEGER,
                            `totalVolumeKg` REAL NOT NULL,
                            `totalReps` INTEGER NOT NULL,
                            `totalSets` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sessions_sessionDate` ON `manual_workout_sessions` (`sessionDate`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sessions_createdAt` ON `manual_workout_sessions` (`createdAt`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sessions_workoutName` ON `manual_workout_sessions` (`workoutName`)")

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `manual_workout_sets` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `sessionId` TEXT NOT NULL,
                            `exerciseName` TEXT NOT NULL,
                            `exerciseId` TEXT NOT NULL,
                            `muscleGroup` TEXT NOT NULL,
                            `setNumber` INTEGER NOT NULL,
                            `reps` INTEGER NOT NULL,
                            `weightKg` REAL NOT NULL,
                            `rpe` REAL,
                            `isWarmup` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sets_sessionId` ON `manual_workout_sets` (`sessionId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sets_exerciseName` ON `manual_workout_sets` (`exerciseName`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sets_exerciseId` ON `manual_workout_sets` (`exerciseId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_manual_workout_sets_sessionId_exerciseName` ON `manual_workout_sets` (`sessionId`, `exerciseName`)")
                }
            }
    }
}
