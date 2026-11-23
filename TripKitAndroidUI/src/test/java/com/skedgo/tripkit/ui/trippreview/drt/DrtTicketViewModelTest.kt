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
    fun `select starts at supplied minimum and decrement stops at minimum`() = runTest {
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"companion","min":2,"max":5}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        viewModel.setTicket(fare)
        viewModel.onSelect()
        assertEquals(2L, viewModel.value.value)
        assertEquals(2L, fare.value)
        viewModel.onDecrementValue()
        assertEquals(2L, viewModel.value.value)
        assertEquals(2L, fare.value)
        assertEquals(false, viewModel.enableDecrement.value)
    }

    @Test
    fun `quantity controls disable at maximum and allow optional deselection`() = runTest {
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"companion","min":0,"max":3}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        viewModel.setTicket(fare)
        viewModel.setValue(0)
        assertEquals(0L, viewModel.value.value)
        assertEquals(false, viewModel.enableDecrement.value)
        viewModel.onSelect()
        viewModel.onIncrementValue()
        viewModel.onIncrementValue()
        assertEquals(3L, viewModel.value.value)
        assertEquals(false, viewModel.enableIncrement.value)
        viewModel.onIncrementValue()
        assertEquals(3L, fare.value)
        viewModel.onDecrementValue()
        assertEquals(true, viewModel.enableIncrement.value)
        viewModel.onDecrementValue()
        viewModel.onDecrementValue()
        assertEquals(0L, fare.value)
        assertEquals(false, viewModel.enableDecrement.value)
    }

    @Test
    fun `fixed quantity one starts selected and cannot be deselected by updates or actions`() = runTest {
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"opaque-fare","min":1,"max":1}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        viewModel.setTicket(fare)
        assertEquals(1L, viewModel.value.value)
        assertEquals(1L, fare.value)
        assertEquals(false, viewModel.enableIncrement.value)
        assertEquals(false, viewModel.enableDecrement.value)
        repeat(3) { viewModel.onDecrementValue(); viewModel.onIncrementValue() }
        viewModel.setValue(0)
        assertEquals(1L, fare.value)
        assertEquals(1L, viewModel.value.value)
    }

    @Test
    fun `two to five initializes at two and repeated actions stay in bounds`() = runTest {
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"opaque","min":2,"max":5,"value":0}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        viewModel.setValue(0)
        viewModel.setTicket(fare)
        assertEquals(2L, viewModel.value.value)
        repeat(6) { viewModel.onIncrementValue() }
        assertEquals(5L, fare.value)
        assertEquals(false, viewModel.enableIncrement.value)
        repeat(6) { viewModel.onDecrementValue() }
        assertEquals(2L, fare.value)
        assertEquals(false, viewModel.enableDecrement.value)
    }

    @Test
    fun `missing minimum preserves legacy deselection`() = runTest {
        val fare = com.google.gson.Gson().fromJson(
            """{"id":"legacy","max":3,"value":1}""",
            com.skedgo.tripkit.booking.quickbooking.Fare::class.java
        )
        viewModel.setValue(1)
        viewModel.setTicket(fare)
        viewModel.onDecrementValue()
        assertEquals(0L, fare.value)
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
