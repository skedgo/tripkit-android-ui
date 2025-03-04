package com.skedgo.tripkit.ui.dialog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GenericListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: GenericListViewModel

    @Before
    fun setUp() {
        viewModel = GenericListViewModel()
    }

    @Test
    fun `setViewModeOnly should update LiveData correctly`() {
        // Act
        viewModel.setViewModeOnly(true)

        // Assert
        assertEquals(true, viewModel.viewModeOnly.value)
    }

    @Test
    fun `setTitle should update title correctly`() {
        // Act
        viewModel.setTitle("Test Title")

        // Assert
        assertEquals("Test Title", viewModel.title.value)
    }

    @Test
    fun `setDescriptionTitle should update descriptionTitle correctly`() {
        // Act
        viewModel.setDescriptionTitle("Description Title")

        // Assert
        assertEquals("Description Title", viewModel.descriptionTitle.value)
    }

    @Test
    fun `setDescription should update description correctly`() {
        // Act
        viewModel.setDescription("This is a description.")

        // Assert
        assertEquals("This is a description.", viewModel.description.value)
    }

    @Test
    fun `setListSelection should update selection correctly`() {
        // Arrange
        val mockItem1 =
            GenericListItem(label = "Item 1", selected = false, date = null, subLabel = null)
        val mockItem2 =
            GenericListItem(label = "Item 2", selected = false, date = null, subLabel = null)
        val itemList = listOf(mockItem1, mockItem2)

        // Act
        viewModel.setListSelection(itemList)

        // Assert
        assertEquals(itemList, viewModel.selection.value)
    }

    @Test
    fun `setSelectedItems should update selected items correctly`() {
        // Arrange
        val item1 =
            GenericListItem(label = "Item 1", selected = false, date = null, subLabel = null)
        val item2 =
            GenericListItem(label = "Item 2", selected = false, date = null, subLabel = null)
        val itemList = listOf(item1, item2)

        viewModel.setListSelection(itemList)

        // Act
        viewModel.setSelectedItems(listOf("Item 1"))

        // Assert
        assertEquals(true, viewModel.selection.value?.first { it.label == "Item 1" }?.selected)
        assertEquals(false, viewModel.selection.value?.first { it.label == "Item 2" }?.selected)
    }
}
