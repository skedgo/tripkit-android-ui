package com.skedgo.tripkit.ui.dialog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GenericWebViewViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: GenericWebViewViewModel

    @Before
    fun setUp() {
        viewModel = GenericWebViewViewModel()
    }

    @Test
    fun `setTitle should update title correctly`() {
        // Act
        viewModel.setTitle("Web Page Title")

        // Assert
        assertEquals("Web Page Title", viewModel.title.value)
    }

    @Test
    fun `setAcceptShown should update acceptShown correctly`() {
        // Act
        viewModel.setAcceptShown(true)

        // Assert
        assertEquals(true, viewModel.acceptShown.value)
    }
}
