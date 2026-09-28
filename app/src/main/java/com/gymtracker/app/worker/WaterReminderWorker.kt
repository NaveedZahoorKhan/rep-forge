package com.gymtracker.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: GymRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val profile = repository.observeUserProfile().firstOrNull()
        val todayEpochDay = LocalDate.now().toEpochDay()
        val todayWater = repository.observeWater(todayEpochDay).firstOrNull()

        val currentMl = todayWater?.milliliters ?: 0
        val goalMl = profile?.waterGoalMl ?: 3000

        notificationHelper.showWaterReminder(currentMl, goalMl)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "gymtracker-water-reminder"
    }
}
