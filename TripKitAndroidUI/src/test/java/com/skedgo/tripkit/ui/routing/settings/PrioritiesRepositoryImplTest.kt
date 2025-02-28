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

class PrioritiesRepositoryImplTest {

    private lateinit var repository: PrioritiesRepositoryImpl
    private val resources: Resources = mockk()
    private val prefs: SharedPreferences = mockk()
    private val editor: SharedPreferences.Editor = mockk()

    @Before
    fun setUp() {
        every { resources.getString(R.string.pref_budget) } returns "pref_budget"
        every { resources.getString(R.string.pref_time) } returns "pref_time"
        every { resources.getString(R.string.pref_carbon) } returns "pref_carbon"
        every { resources.getString(R.string.pref_hassle) } returns "pref_hassle"
        every { resources.getString(R.string.pref_exercise) } returns "pref_exercise"

        every { prefs.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.apply() } just Runs

        repository = PrioritiesRepositoryImpl(resources, prefs)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getBudgetPriority should return stored budget priority`() = runBlocking {
        every { prefs.getInt("pref_budget", 50) } returns 75

        val result = repository.getBudgetPriority()

        assert(result.value == 75)
    }

    @Test
    fun `putBudgetPriority should store budget priority`() = runBlocking {
        val budgetPriority = Priority.Budget(80)

        repository.putBudgetPriority(budgetPriority)

        verify { editor.putInt("pref_budget", 80) }
        verify { editor.apply() }
    }

    @Test
    fun `getTimePriority should return stored time priority`() = runBlocking {
        every { prefs.getInt("pref_time", 50) } returns 60

        val result = repository.getTimePriority()

        assert(result.value == 60)
    }

    @Test
    fun `putTimePriority should store time priority`() = runBlocking {
        val timePriority = Priority.Time(70)

        repository.putTimePriority(timePriority)

        verify { editor.putInt("pref_time", 70) }
        verify { editor.apply() }
    }

    @Test
    fun `getEnvironmentPriority should return stored environment priority`() = runBlocking {
        every { prefs.getInt("pref_carbon", 50) } returns 40

        val result = repository.getEnvironmentPriority()

        assert(result.value == 40)
    }

    @Test
    fun `putEnvironmentPriority should store environment priority`() = runBlocking {
        val environmentPriority = Priority.Environment(55)

        repository.putEnvironmentPriority(environmentPriority)

        verify { editor.putInt("pref_carbon", 55) }
        verify { editor.apply() }
    }

    @Test
    fun `getConveniencePriority should return stored convenience priority`() = runBlocking {
        every { prefs.getInt("pref_hassle", 50) } returns 45

        val result = repository.getConveniencePriority()

        assert(result.value == 45)
    }

    @Test
    fun `putConveniencePriority should store convenience priority`() = runBlocking {
        val conveniencePriority = Priority.Convenience(65)

        repository.putConveniencePriority(conveniencePriority)

        verify { editor.putInt("pref_hassle", 65) }
        verify { editor.apply() }
    }

    @Test
    fun `getExercisePriority should return stored exercise priority`() = runBlocking {
        every { prefs.getInt("pref_exercise", 50) } returns 30

        val result = repository.getExercisePriority()

        assert(result.value == 30)
    }

    @Test
    fun `putExercisePriority should store exercise priority`() = runBlocking {
        val exercisePriority = Priority.Exercise(85)

        repository.putExercisePriority(exercisePriority)

        verify { editor.putInt("pref_exercise", 85) }
        verify { editor.apply() }
    }
}
