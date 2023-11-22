package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import javax.inject.Inject

internal class RemindersRepositoryImpl @Inject constructor(
    resources: Resources,
    private val prefs: SharedPreferences
) : RemindersRepository {

    private val tripNotificationReminderKey = "tripNotificationReminder"
    private val tripNotificationReminderDefaultValue = 30L

    override suspend fun getTripNotificationReminderMinutes(): Long =
        prefs.getLong(tripNotificationReminderKey, tripNotificationReminderDefaultValue)

    override suspend fun saveTripNotificationReminderMinutes(minutes: Long) =
        prefs.edit().putLong(tripNotificationReminderKey, minutes).apply()
}