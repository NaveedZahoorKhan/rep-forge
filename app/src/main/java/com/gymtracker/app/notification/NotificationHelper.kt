package com.gymtracker.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymtracker.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REST_CHANNEL, "Rest timers", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun showRestComplete(exerciseName: String, sound: Boolean, vibration: Boolean) {
        show(
            id = 1001,
            channelId = REST_CHANNEL,
            title = "Rest complete",
            body = "Next set: $exerciseName",
            sound = sound,
            vibration = vibration,
        )
    }

    fun showWorkoutReminder(title: String, body: String) {
        show(
            id = 2001,
            channelId = REMINDER_CHANNEL,
            title = title,
            body = body,
            sound = true,
            vibration = true,
        )
    }

    private fun show(id: Int, channelId: String, title: String, body: String, sound: Boolean, vibration: Boolean) {
        ensureChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val defaults = buildList {
            if (sound) add(NotificationCompat.DEFAULT_SOUND)
            if (vibration) add(NotificationCompat.DEFAULT_VIBRATE)
        }.fold(0) { acc, value -> acc or value }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(defaults)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val REST_CHANNEL = "rest_timers"
        const val REMINDER_CHANNEL = "workout_reminders"
    }
}
