package com.gymtracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_health_profiles")
data class UserHealthProfileEntity(
    @PrimaryKey val id: String = "me",
    val primaryGoal: String = "Muscle Hypertrophy",
    val experienceLevel: String = "Intermediate",
    val healthConditions: String = "",
    val injuriesAndLimitations: String = "",
    val targetMuscles: String = "",
    val preferredDurationMinutes: Int = 45,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "gemini_sync_reports", indices = [Index("syncedAt")])
data class GeminiSyncReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val syncedAt: Long = System.currentTimeMillis(),
    val overallSummary: String,
    val consistencyScore: String,
    val volumeAnalysis: String,
    val healthReview: String,
    val nutritionAudit: String,
    val actionableSuggestionsJson: String = "[]",
    val motivationalQuote: String = "",
    val workloadSummary: String = "",
)
