package com.gymtracker.app.presentation.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ProfileSettingsTest {

    @Test
    fun testMetricAndImperialRoundTripConversions() {
        val originalCm = 175.0
        val originalKg = 75.0

        // Metric -> Imperial
        val inches = originalCm / 2.54
        val lbs = originalKg * 2.20462262185

        assertEquals(68.8976, inches, 0.01)
        assertEquals(165.3466, lbs, 0.01)

        // Imperial -> Metric (saving)
        val roundTripCm = inches * 2.54
        val roundTripKg = lbs / 2.20462262185

        assertEquals(originalCm, roundTripCm, 0.0001)
        assertEquals(originalKg, roundTripKg, 0.0001)
    }

    @Test
    fun testCalculateReminderInitialDelayInFutureToday() {
        val now = Calendar.getInstance()
        val targetHour = (now.get(Calendar.HOUR_OF_DAY) + 2) % 24
        val targetMinute = 30

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delayMs = target.timeInMillis - now.timeInMillis
        assertTrue(delayMs > 0)
        assertTrue(delayMs <= 86_400_000)
    }

    @Test
    fun testCalculateReminderInitialDelayInPastSchedulesTomorrow() {
        val now = Calendar.getInstance()
        val pastCalendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -1)
        }
        val targetHour = pastCalendar.get(Calendar.HOUR_OF_DAY)
        val targetMinute = pastCalendar.get(Calendar.MINUTE)

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delayMs = target.timeInMillis - now.timeInMillis
        assertTrue(delayMs > 22 * 3600 * 1000)
        assertTrue(delayMs <= 24 * 3600 * 1000)
    }
}
