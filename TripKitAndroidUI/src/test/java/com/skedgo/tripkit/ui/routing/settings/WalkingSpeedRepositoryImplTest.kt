package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class WalkingSpeedRepositoryImplTest {

    private lateinit var repository: WalkingSpeedRepositoryImpl
    private val resources: Resources = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { resources.getString(R.string.pref_walking_speed) } returns "pref_walking_speed"
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = WalkingSpeedRepositoryImpl(resources, prefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `putWalkingSpeed should store walking speed`() = runBlocking {
        val walkingSpeed = WalkingSpeed.Fast

        repository.putWalkingSpeed(walkingSpeed)

        verify { editor.putString("pref_walking_speed", walkingSpeed.value.toString()) }
        verify { editor.apply() }
    }

    @Test
    fun `getWalkingSpeed should return stored walking speed`() = runBlocking {
        every { prefs.getString("pref_walking_speed", null) } returns "2"

        val result = repository.getWalkingSpeed()

        assert(result == WalkingSpeed.Fast)
    }

    @Test
    fun `getWalkingSpeed should return default Medium if no value is stored`() = runBlocking {
        every { prefs.getString("pref_walking_speed", null) } returns null

        val result = repository.getWalkingSpeed()

        assert(result == WalkingSpeed.Medium)
    }
}
