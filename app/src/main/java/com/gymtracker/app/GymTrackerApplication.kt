package com.gymtracker.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gymtracker.app.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GymTrackerApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .apply {
                if (::workerFactory.isInitialized) {
                    setWorkerFactory(workerFactory)
                }
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        if (::notificationHelper.isInitialized) {
            notificationHelper.ensureChannels()
        }
    }
}
