package com.skedgo.tripkit.ui.routing.settings

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class UnitsRepositoryImplTest {

    private lateinit var repository: UnitsRepositoryImpl
    private val resources: Resources = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { resources.getString(R.string.pref_distance_unit) } returns "pref_distance_unit"
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = UnitsRepositoryImpl(resources, prefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `putUnit should store unit preference`() {
        repository.putUnit("km")

        verify { editor.putString("pref_distance_unit", "km") }
        verify { editor.apply() }
    }

    @Test
    fun `getUnit should return stored unit preference`() {
        every { prefs.getString("pref_distance_unit", "auto") } returns "miles"

        val result = repository.getUnit()

        assert(result == "miles")
    }

    @Test
    fun `getUnit should return default auto if no preference is set`() {
        every { prefs.getString("pref_distance_unit", "auto") } returns null

        val result = repository.getUnit()

        assert(result == "auto")
    }
}
