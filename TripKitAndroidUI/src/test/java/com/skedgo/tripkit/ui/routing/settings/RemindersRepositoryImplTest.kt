package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class RemindersRepositoryImplTest {

    private lateinit var repository: RemindersRepositoryImpl
    private val resources: Resources = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { prefs.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = RemindersRepositoryImpl(resources, prefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getTripNotificationReminderMinutes should return stored value`() = runBlocking {
        every { prefs.getLong("tripNotificationReminder", 10L) } returns 15L

        val result = repository.getTripNotificationReminderMinutes()

        assert(result == 15L)
    }

    @Test
    fun `getTripNotificationReminderMinutes should return default value if not set`() = runBlocking {
        every { prefs.getLong("tripNotificationReminder", 10L) } returns 10L

        val result = repository.getTripNotificationReminderMinutes()

        assert(result == 10L)
    }

    @Test
    fun `saveTripNotificationReminderMinutes should store value`() = runBlocking {
        repository.saveTripNotificationReminderMinutes(20L)

        verify { editor.putLong("tripNotificationReminder", 20L) }
        verify { editor.apply() }
    }
}
