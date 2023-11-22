package com.skedgo.tripkit.ui.routing.settings

interface RemindersRepository {

    suspend fun saveTripNotificationReminderMinutes(minutes: Long)

    suspend fun getTripNotificationReminderMinutes(): Long

}