package com.skedgo.tripkit.ui.search

import android.content.Context
import androidx.core.content.ContextCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.base.MockKTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocationSearchErrorViewModelTest: MockKTest() {

    private lateinit var context: Context
    private lateinit var viewModel: LocationSearchErrorViewModel

    @Before
    fun setUp() {
        context = mockk(relaxed = true) // Mock context
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getDrawable(any(), any()) } returns mockk()

        viewModel = LocationSearchErrorViewModel(context)
    }

    @Test
    fun `updateError should set correct values for NoResults`() {
        val searchText = "Test Location"
        val errorType = SearchErrorType.NoResults(searchText)
        every { context.getString(R.string._apost_pattern_apost_not_found_dot, searchText) } returns "Mocked NoResults Title"
        every { context.getString(R.string.drop_new_pin) } returns "Mocked Drop Pin"

        viewModel.updateError(errorType)

        assertEquals("Mocked NoResults Title", viewModel.title.get())
        assertEquals("Mocked Drop Pin", viewModel.actionText.get())
        assertNotNull(viewModel.iconSrc.get())
    }

    @Test
    fun `updateError should set correct values for NoConnection`() {
        val errorType = SearchErrorType.NoConnection
        every { context.getString(R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot) } returns "Mocked NoConnection Title"
        every { context.getString(R.string.retry) } returns "Mocked Retry"

        viewModel.updateError(errorType)

        assertEquals("Mocked NoConnection Title", viewModel.title.get())
        assertEquals("Mocked Retry", viewModel.actionText.get())
        assertNotNull(viewModel.iconSrc.get())
    }

    @Test
    fun `updateError should set correct values for OtherError`() {
        val errorType = SearchErrorType.OtherError
        every { context.getString(R.string.error_encountered) } returns "Mocked OtherError Title"
        every { context.getString(R.string.retry) } returns "Mocked Retry"

        viewModel.updateError(errorType)

        assertEquals("Mocked OtherError Title", viewModel.title.get())
        assertEquals("Mocked Retry", viewModel.actionText.get())
        assertNotNull(viewModel.iconSrc.get())
    }

    @Test
    fun `performAction should emit chooseOnMap when error is NoResults`() {
        val searchText = "Test Location"
        val errorType = SearchErrorType.NoResults(searchText)
        viewModel.updateError(errorType)
        
        val testObserver = viewModel.chooseOnMapObservable.test()

        viewModel.performAction()

        testObserver.assertValue(Unit)
    }

    @Test
    fun `performAction should emit retry when error is not NoResults`() {
        val errorType = SearchErrorType.NoConnection
        viewModel.updateError(errorType)

        val testObserver = viewModel.retryObservable.test()

        viewModel.performAction()

        testObserver.assertValue(Unit)
    }
}
