package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class CyclingSpeedRepositoryImplTest {

    private lateinit var repository: CyclingSpeedRepositoryImpl
    private val resources: Resources = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { resources.getString(R.string.pref_cycling_speed) } returns "pref_cycling_speed"
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = CyclingSpeedRepositoryImpl(resources, prefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `putCyclingSpeed should store cycling speed`() = runBlocking {
        val cyclingSpeed = CyclingSpeed.Fast

        repository.putCyclingSpeed(cyclingSpeed)

        verify { editor.putString("pref_cycling_speed", cyclingSpeed.value.toString()) }
        verify { editor.apply() }
    }

    @Test
    fun `getCyclingSpeed should return stored cycling speed`() = runBlocking {
        every { prefs.getString("pref_cycling_speed", null) } returns "2"

        val result = repository.getCyclingSpeed()

        assert(result == CyclingSpeed.Fast)
    }

    @Test
    fun `getCyclingSpeed should return default Medium if no value is stored`() = runBlocking {
        every { prefs.getString("pref_cycling_speed", null) } returns null

        val result = repository.getCyclingSpeed()

        assert(result == CyclingSpeed.Medium)
    }
}