package com.gymtracker.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gymtracker.app.data.local.dao.GymDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class LocalBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun snapshot(dao: GymDao): ExportSnapshot = ExportSnapshot(
        exercises = dao.getExercises(),
        workouts = dao.getWorkouts(),
        workoutExercises = dao.getWorkouts().flatMap { dao.getWorkoutExercises(it.id) },
        setTemplates = dao.getWorkouts()
            .flatMap { dao.getWorkoutExercises(it.id) }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { dao.getSetTemplates(it) }
            ?: emptyList(),
        sessions = dao.getSessions(),
        performedSets = dao.getPerformedSets(),
        personalRecords = dao.getPersonalRecords(),
        bodyMeasurements = dao.getMeasurements(),
        progressPhotos = dao.getProgressPhotos(),
        nutritionLogs = dao.getNutritionLogs(),
        meals = dao.getMeals(),
        waterLogs = dao.getWaterLogs(),
        weightLogs = dao.getWeightLogs(),
        userProfile = dao.getUserProfile(),
        weeklySchedule = dao.getWeeklySchedule(),
        reminders = dao.getReminders(),
    )

    suspend fun exportJson(dao: GymDao): String = json.encodeToString(snapshot(dao))

    suspend fun exportCsv(dao: GymDao): String {
        val sessions = dao.getSessions()
        val sets = dao.getPerformedSets().groupBy { it.sessionId }
        return buildString {
            appendLine("session_id,workout_name,started_at,ended_at,exercise,set_number,set_type,weight,reps,rpe,rir,is_pr,volume")
            sessions.forEach { session ->
                sets[session.id].orEmpty().forEach { set ->
                    appendLine(
                        listOf(
                            session.id,
                            session.workoutName.csv(),
                            session.startedAt,
                            session.endedAt ?: "",
                            set.exerciseName.csv(),
                            set.setNumber,
                            set.setType.name,
                            set.weight,
                            set.reps,
                            set.rpe ?: "",
                            set.rir ?: "",
                            set.isPr,
                            set.weight * set.reps,
                        ).joinToString(",")
                    )
                }
            }
        }
    }

    suspend fun restore(jsonText: String, dao: GymDao) {
        val snapshot = json.decodeFromString<ExportSnapshot>(jsonText)
        dao.deleteAllUserData()
        dao.upsertExercises(snapshot.exercises)
        dao.upsertWorkouts(snapshot.workouts)
        dao.upsertWorkoutExercises(snapshot.workoutExercises)
        dao.upsertSetTemplates(snapshot.setTemplates)
        snapshot.userProfile?.let { dao.upsertUserProfile(it) }
        dao.upsertSessions(snapshot.sessions)
        dao.upsertPerformedSets(snapshot.performedSets)
        snapshot.personalRecords.forEach { dao.upsertPersonalRecord(it) }
        dao.upsertMeasurements(snapshot.bodyMeasurements)
        dao.upsertPhotos(snapshot.progressPhotos)
        dao.upsertNutritionLogs(snapshot.nutritionLogs)
        dao.upsertMeals(snapshot.meals)
        dao.upsertWaterLogs(snapshot.waterLogs)
        dao.upsertWeightLogs(snapshot.weightLogs)
        dao.upsertSchedules(snapshot.weeklySchedule)
        dao.upsertReminders(snapshot.reminders)
    }

    fun writeShareFile(fileName: String, content: String): Uri {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun String.csv(): String = "\"${replace("\"", "\"\"")}\""
}
