package com.skedgo.tripkit.ui.creditsources

import android.content.Context
import android.content.res.Resources
import com.skedgo.tripkit.routing.Source
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tracking.Event
import com.skedgo.tripkit.ui.tracking.EventTracker
import io.mockk.*
import io.reactivex.observers.TestObserver
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class CreditSourcesOfDataViewModelTest {

    private lateinit var viewModel: CreditSourcesOfDataViewModel
    private val mockContext: Context = mockk(relaxed = true)
    private val mockResources: Resources = mockk()
    private val mockEventTracker: EventTracker = mockk(relaxed = true)
    private var eventTrackerLazy: dagger.Lazy<EventTracker> = mockk()

    @Before
    fun setUp() {
        every { mockContext.resources } returns mockResources
        // Mock dagger.Lazy<EventTracker>
        eventTrackerLazy = mockk()
        every { eventTrackerLazy.get() } returns mockEventTracker
        every { mockResources.getString(2131820700, any<Array<Any>>()) } returns "Data provided by Provider1, Provider2"

        viewModel = CreditSourcesOfDataViewModel(mockContext, eventTrackerLazy)
    }

    @Test
    fun `changeSources should update creditSources field`() {
        // Arrange
        val mockSource1: Source = mockk {
            every { provider()?.name() } returns "Provider1"
        }
        val mockSource2: Source = mockk {
            every { provider()?.name() } returns "Provider2"
        }
        val sourceList = listOf(mockSource1, mockSource2)

        every { mockResources.getString(R.string.data_provided_by__pattern, any()) } answers {
            val providers = secondArg<Array<Any>>() // Extract the second argument
            "Data provided by: ${providers.joinToString(", ")}"
        }


        // Act
        viewModel.changeSources(sourceList)

        // Assert
        assertEquals("Data provided by: Provider1, Provider2", viewModel.creditSources.get())
    }

    @Ignore("Inconsistent, to check later")
    @Test
    fun `tapAction should emit sources and trigger event tracking`() {
        // Arrange
        val mockSource1: Source = mockk {
            every { provider()?.name() } returns "Provider1"
        }
        val mockSource2: Source = mockk {
            every { provider()?.name() } returns "Provider2"
        }
        val sourceList = listOf(mockSource1, mockSource2)

        viewModel.changeSources(sourceList)

        val testObserver = TestObserver<List<Source>>()
        viewModel.tapAction.observable.subscribe(testObserver)

        // Act
        viewModel.tapAction.perform()

        // Assert
        testObserver.assertValue(sourceList) // Ensure the correct list was emitted
        verify { mockEventTracker.log(Event.ViewCreditSources("Provider1, Provider2")) }
    }
}