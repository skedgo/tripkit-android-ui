package com.skedgo.tripkit.ui.trippreview.drt

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.*
import org.amshove.kluent.internal.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrtTicketViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: DrtTicketViewModel

    @Before
    fun setUp() {
        initDispatchers()
        viewModel = DrtTicketViewModel()
    }

    @After
    fun tearDown() {
        tearDownDispatchers()
    }

    @Test
    fun `test onChange emits correct event`() = runTest {
        val mockFlow = mockk<MutableSharedFlow<DrtTicketViewModel>>(relaxed = true)
        viewModel.onChangeStream = mockFlow

        viewModel.onChange()

        advanceUntilIdle()

        coVerify { mockFlow.emit(viewModel) }
    }

    @Test
    fun `test setLabel updates LiveData correctly`() {
        viewModel.setLabel("Test Label")
        assertEquals("Test Label", viewModel.label.value)
    }

    @Test
    fun `test setCurrency updates LiveData correctly`() {
        viewModel.setCurrency("USD")
        assertEquals("USD", viewModel.currency.value)
    }

    @Test
    fun `test setPrice updates description correctly`() {
        viewModel.setPrice(1000.0, "$")
        assertEquals("$10.00", viewModel.description.value)

        viewModel.setPrice(0.0, "$")
        assertEquals("FREE", viewModel.description.value)
    }

    @Test
    fun `test setValue updates LiveData correctly`() {
        viewModel.setValue(5)
        assertEquals(5, viewModel.value.value)
    }

    @Test
    fun `test onIncrementValue increases ticket count`() = runTest {
        viewModel.setValue(2)
        viewModel.onIncrementValue()
        assertEquals(3, viewModel.value.value)
    }

    @Test
    fun `test onDecrementValue decreases ticket count`() = runTest {
        viewModel.setValue(2)
        viewModel.onDecrementValue()
        assertEquals(1, viewModel.value.value)
    }

    @Test
    fun `test onSelect updates value and triggers onChange`() = runTest {
        val mockFlow = mockk<MutableSharedFlow<DrtTicketViewModel>>(relaxed = true)
        viewModel.onChangeStream = mockFlow

        viewModel.onSelect()
        advanceUntilIdle()
        assertEquals(1, viewModel.value.value)
        coVerify { mockFlow.emit(viewModel) }
    }
}
