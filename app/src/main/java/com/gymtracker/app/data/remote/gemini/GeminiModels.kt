package com.gymtracker.app.data.remote.gemini

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// REST payload models following gemini-api skill
@Serializable
data class GenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null,
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null,
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String,
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

// Gemini Custom Workout Builder domain models
@Serializable
data class GeminiWorkoutBuilderResult(
    val identifiedMachine: String = "",
    val machineDescription: String = "",
    val healthSafetyCues: List<String> = emptyList(),
    val options: List<GeminiWorkoutOption> = emptyList(),
)

@Serializable
data class GeminiWorkoutOption(
    val title: String,
    val splitType: String,
    val estimatedMinutes: Int = 45,
    val healthFocusNote: String = "",
    val exercises: List<GeminiExerciseItem> = emptyList(),
)

@Serializable
data class GeminiExerciseItem(
    val exerciseName: String,
    val primaryMuscle: String,
    val equipment: String,
    val sets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val restSeconds: Int = 90,
    val targetRpe: Double? = null,
    val healthModification: String = "",
    val instructions: String = "",
)

// Gemini Daily Progress Sync Report models
@Serializable
data class GeminiProgressSyncResult(
    val syncTimestamp: Long = System.currentTimeMillis(),
    val overallSummary: String = "",
    val consistencyScore: String = "",
    val volumeAndLoadAnalysis: String = "",
    val healthAndRecoveryReview: String = "",
    val nutritionAndHydrationAudit: String = "",
    val actionableSuggestions: List<String> = emptyList(),
    val motivationalQuote: String = "",
    val workloadSummary: String = "",
)
