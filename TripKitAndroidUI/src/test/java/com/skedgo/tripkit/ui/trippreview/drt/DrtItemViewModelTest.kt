package com.skedgo.tripkit.ui.trippreview.drt

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import okhttp3.internal.wait
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class DrtItemViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: DrtItemViewModel

    @Before
    fun setUp() {
        initDispatchers()
        viewModel = DrtItemViewModel()
    }

    @After
    fun tearDown() {
        tearDownDispatchers()
    }

    @Test
    fun `test initial values`() {
        assertNull(viewModel.icon.value)
        assertNull(viewModel.label.value)
        assertNull(viewModel.values.value)
        assertNull(viewModel.ids.value)
        assertFalse(viewModel.viewMode.value ?: false)
        assertTrue(viewModel.showChangeButton.value ?: false)
    }

    @Test
    fun `test set values correctly`() {
        viewModel.setIcon(123)
        viewModel.setLabel("Test Label")
        viewModel.setValue(listOf("Option 1", "Option 2"))
        viewModel.setRequired(true)

        assertEquals(123, viewModel.icon.value)
        assertEquals("Test Label", viewModel.label.value)
        assertEquals(listOf("Option 1", "Option 2"), viewModel.values.value)
        assertTrue(viewModel.required.value!!)
    }

    @Test
    fun `test increment and decrement values`() {
        viewModel.setValue(listOf("2"))
        viewModel.setMinValue(1)
        viewModel.setMaxValue(5)

        viewModel.onIncrementValue()
        assertEquals("3", viewModel.values.value?.first())

        viewModel.onDecrementValue()
        assertEquals("2", viewModel.values.value?.first())
    }

    @Test
    fun `test onChange emits correct event`() = runTest {
        val mockFlow = mockk<MutableSharedFlow<DrtItemViewModel>>(relaxed = true)
        viewModel.onChangeStream = mockFlow

        viewModel.onChange()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { mockFlow.emit(viewModel) }
    }

    @Test
    fun `test setViewMode updates correctly`() {
        viewModel.setViewMode(true)
        assertTrue(viewModel.viewMode.value!!)

        viewModel.setViewMode(false)
        assertFalse(viewModel.viewMode.value!!)
    }

    @Test
    fun `test content description updates correctly`() {
        val observer = mockk<Observer<String?>>(relaxed = true)
        viewModel.contentDescription.observeForever(observer)

        viewModel.setContentDescription("Sample Description")
        verify { observer.onChanged("Sample Description") }

        viewModel.setContentDescriptionWithAppendingLabel("Extra Info")

        testDispatcher.scheduler.advanceUntilIdle()

        verify { observer.onChanged("null, Extra Info") }
    }

    @Test
    fun `test setting RETURN_TRIP updates isReturnTrip`() {
        viewModel.setType("RETURN_TRIP")
        assertTrue(viewModel.isReturnTrip.value!!)
    }
}
