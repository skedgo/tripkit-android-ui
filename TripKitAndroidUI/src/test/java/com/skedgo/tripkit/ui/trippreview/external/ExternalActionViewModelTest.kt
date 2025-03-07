package com.skedgo.tripkit.ui.trippreview.external

import com.skedgo.tripkit.ui.trippreview.Action
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExternalActionViewModelTest {

    private lateinit var viewModel: ExternalActionViewModel

    @Before
    fun setUp() {
        viewModel = ExternalActionViewModel()
    }

    @Test
    fun `test setting title updates ObservableField`() {
        viewModel.title.set("Test Title")

        assertEquals("Test Title", viewModel.title.get())
    }

    @Test
    fun `test setting action updates value`() {
        viewModel.action = "test_action"

        assertEquals("test_action", viewModel.action)
    }

    @Test
    fun `test setting external action updates correctly`() {
        val mockAction = mockk<Action>(relaxed = true)
        viewModel.externalAction = mockAction

        assertEquals(mockAction, viewModel.externalAction)
    }
}
