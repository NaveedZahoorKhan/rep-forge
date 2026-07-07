package com.gymtracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SetTemplateEntity
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
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymTrackerDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE user_profile ADD COLUMN analyticsOptIn INTEGER NOT NULL DEFAULT 0")
                }
            }
    }
}
