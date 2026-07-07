package com.gymtracker.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gymtracker.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Workout scheduled"
        val body = inputData.getString(KEY_BODY) ?: "Open GymTracker and start your planned session."
        notificationHelper.showWorkoutReminder(title, body)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
