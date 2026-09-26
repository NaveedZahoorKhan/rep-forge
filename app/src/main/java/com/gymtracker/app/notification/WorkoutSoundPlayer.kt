package com.gymtracker.app.notification

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface WorkoutSoundPlayer {
    fun playRestCompleteSound()
    fun vibrateRestComplete()
}

@Singleton
class WorkoutSoundPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkoutSoundPlayer {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Throwable) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Plays an audible countdown completion sound when rest interval expires.
     * Uses a crisp triple gym timer beep pattern (beep... beep... BEEP!).
     */
    override fun playRestCompleteSound() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val tg = toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(180)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(180)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            } catch (e: Throwable) {
                android.util.Log.e("WorkoutSoundPlayer", "Failed to play rest timer tone", e)
            }
        }
    }

    /**
     * Triggers distinct physical vibration feedback for rest completion.
     */
    override fun vibrateRestComplete() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibration = VibrationEffect.createWaveform(
                    longArrayOf(0, 180, 100, 350),
                    -1,
                )
                vibratorManager?.defaultVibrator?.vibrate(vibration)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 350), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 180, 100, 350), -1)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("WorkoutSoundPlayer", "Failed to vibrate", e)
        }
    }
}
