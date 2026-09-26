package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for manual entry of workout sessions, exercises, sets, reps, and weights.
 */
@Dao
interface ManualWorkoutDao {

    // -------------------------------------------------------------
    // Reactive Observables (Flows)
    // -------------------------------------------------------------

    /**
     * Observes all manual workout sessions joined with their exercises and sets,
     * ordered chronologically newest first.
     */
    @Transaction
    @Query("SELECT * FROM manual_workout_sessions ORDER BY sessionDate DESC, createdAt DESC")
    fun observeAllManualSessionsWithSets(): Flow<List<ManualWorkoutSessionWithSets>>

    /**
     * Observes a single manual workout session with all its sets by session ID.
     */
    @Transaction
    @Query("SELECT * FROM manual_workout_sessions WHERE id = :sessionId LIMIT 1")
    fun observeManualSessionWithSets(sessionId: String): Flow<ManualWorkoutSessionWithSets?>

    /**
     * Observes the list of manual workout session headers.
     */
    @Query("SELECT * FROM manual_workout_sessions ORDER BY sessionDate DESC, createdAt DESC")
    fun observeManualSessions(): Flow<List<ManualWorkoutSessionEntity>>

    /**
     * Observes all sets logged for a specific session ID, ordered by exercise and set number.
     */
    @Query("SELECT * FROM manual_workout_sets WHERE sessionId = :sessionId ORDER BY exerciseName ASC, setNumber ASC")
    fun observeSetsForSession(sessionId: String): Flow<List<ManualWorkoutSetEntity>>

    /**
     * Observes all sets logged across history for a specific exercise name.
     */
    @Query("SELECT * FROM manual_workout_sets WHERE exerciseName = :exerciseName ORDER BY createdAt DESC")
    fun observeSetsForExercise(exerciseName: String): Flow<List<ManualWorkoutSetEntity>>

    // -------------------------------------------------------------
    // Direct One-shot Queries
    // -------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM manual_workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getManualSessionWithSets(sessionId: String): ManualWorkoutSessionWithSets?

    @Query("SELECT * FROM manual_workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getManualSession(sessionId: String): ManualWorkoutSessionEntity?

    @Query("SELECT * FROM manual_workout_sessions ORDER BY sessionDate DESC, createdAt DESC")
    suspend fun getAllManualSessions(): List<ManualWorkoutSessionEntity>

    @Query("SELECT * FROM manual_workout_sets WHERE sessionId = :sessionId ORDER BY exerciseName ASC, setNumber ASC")
    suspend fun getSetsForSession(sessionId: String): List<ManualWorkoutSetEntity>

    @Query("SELECT * FROM manual_workout_sets WHERE exerciseName = :exerciseName ORDER BY weightKg DESC, reps DESC LIMIT 1")
    suspend fun getBestSetForExercise(exerciseName: String): ManualWorkoutSetEntity?

    @Query("SELECT COUNT(*) FROM manual_workout_sessions")
    suspend fun getManualSessionCount(): Int

    // -------------------------------------------------------------
    // Mutations (Upsert / Insert / Update / Delete)
    // -------------------------------------------------------------

    @Upsert
    suspend fun upsertSession(session: ManualWorkoutSessionEntity)

    @Upsert
    suspend fun upsertSessions(sessions: List<ManualWorkoutSessionEntity>)

    @Upsert
    suspend fun upsertSet(set: ManualWorkoutSetEntity)

    @Upsert
    suspend fun upsertSets(sets: List<ManualWorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ManualWorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ManualWorkoutSetEntity>)

    @Update
    suspend fun updateSession(session: ManualWorkoutSessionEntity)

    @Update
    suspend fun updateSet(set: ManualWorkoutSetEntity)

    @Query("DELETE FROM manual_workout_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: String)

    @Query("DELETE FROM manual_workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: String)

    @Query("DELETE FROM manual_workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM manual_workout_sets")
    suspend fun clearAllSets()

    @Query("DELETE FROM manual_workout_sessions")
    suspend fun clearAllSessions()

    // -------------------------------------------------------------
    // Atomic Compound Transactions
    // -------------------------------------------------------------

    /**
     * Atomically saves a manual workout session and all of its recorded sets.
     * Calculates and updates total volume, sets, and reps if not already provided.
     */
    @Transaction
    suspend fun saveManualWorkoutSessionWithSets(
        session: ManualWorkoutSessionEntity,
        sets: List<ManualWorkoutSetEntity>,
    ) {
        val totalVolume = sets.sumOf { it.weightKg * it.reps }
        val totalReps = sets.sumOf { it.reps }
        val totalSets = sets.size

        val finalizedSession = session.copy(
            totalVolumeKg = if (session.totalVolumeKg <= 0.0) totalVolume else session.totalVolumeKg,
            totalReps = if (session.totalReps <= 0) totalReps else session.totalReps,
            totalSets = if (session.totalSets <= 0) totalSets else session.totalSets,
        )

        upsertSession(finalizedSession)
        // Remove existing sets if updating an existing session to avoid stale sets
        deleteSetsForSession(finalizedSession.id)
        if (sets.isNotEmpty()) {
            upsertSets(sets.map { it.copy(sessionId = finalizedSession.id) })
        }
    }

    /**
     * Atomically deletes a manual workout session and all sets belonging to it.
     */
    @Transaction
    suspend fun deleteManualWorkoutWithSets(sessionId: String) {
        deleteSetsForSession(sessionId)
        deleteSessionById(sessionId)
    }

    /**
     * Clears all manual workout records from the local Room database.
     */
    @Transaction
    suspend fun clearAllManualWorkoutData() {
        clearAllSets()
        clearAllSessions()
    }
}
