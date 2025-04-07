package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TKUIDateTimePickerDialogViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TKUIDateTimePickerDialogViewModel

    @Before
    fun setUp() {
        viewModel = TKUIDateTimePickerDialogViewModel()
    }

    @Test
    fun `setup should set config correctly`() {
        // Arrange
        val mockConfig = mockk<DateTimePickerConfig>(relaxed = true)

        // Act
        viewModel.setup(mockConfig)

        // Assert
        assertEquals(mockConfig, viewModel.config.value)
    }

    @Test
    fun `setState should update state and handle errors`() {
        // Arrange
        val mockObserver: Observer<String?> = mockk(relaxed = true)
        viewModel.error.observeForever(mockObserver)

        val errorState = DateTimePickerState.OnError(Exception("Test error"))

        // Act
        viewModel.setState(errorState)

        // Assert
        assertEquals("Test error", viewModel.error.value)
        verify { mockObserver.onChanged("Test error") }

        viewModel.error.removeObserver(mockObserver)
    }

    @Test
    fun `setTime should update time correctly`() {
        // Act
        viewModel.setTime("12:30 PM")

        // Assert
        assertEquals("12:30 PM", viewModel.time.value)
    }

    @Test
    fun `setSelectedDate should update selected date correctly`() {
        // Act
        viewModel.setSelectedDate(1640995200000L)

        // Assert
        assertEquals(1640995200000L, viewModel.selectedDate.value)
    }

    @Test
    fun `combineDateTime should return correct Calendar instance`() {
        // Arrange
        val mockConfig = mockk<DateTimePickerConfig>(relaxed = true)
        every { mockConfig.timeZone } returns TimeZone.getTimeZone("Australia/Sydney")

        viewModel.setup(mockConfig)

        val expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Australia/Sydney")).apply {
            timeInMillis = 1640995200000L
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }

        // Act
        val result = viewModel.combineDateTime(1640995200000L, 10, 30)

        // Assert
        assertEquals(expectedCalendar.timeInMillis, result.timeInMillis)
        assertEquals(expectedCalendar.get(Calendar.HOUR_OF_DAY), result.get(Calendar.HOUR_OF_DAY))
        assertEquals(expectedCalendar.get(Calendar.MINUTE), result.get(Calendar.MINUTE))
    }
}
