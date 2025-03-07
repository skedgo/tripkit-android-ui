package com.skedgo.tripkit.ui.trippreview.nearby

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InfoGroupViewModelTest {

    private lateinit var viewModel: InfoGroupViewModel

    @Before
    fun setUp() {
        viewModel = InfoGroupViewModel()
    }

    @Test
    fun `test setting icon updates ObservableField`() {
        viewModel.icon.set(123)

        assertEquals(123, viewModel.icon.get())
    }

    @Test
    fun `test setting title updates ObservableField`() {
        viewModel.title.set(456)

        assertEquals(456, viewModel.title.get())
    }

    @Test
    fun `test setting value updates ObservableField`() {
        viewModel.value.set("Test Value")

        assertEquals("Test Value", viewModel.value.get())
    }
}
