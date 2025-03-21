package com.skedgo.tripkit.ui.tripresults

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.common.model.TransportMode
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TripResultTransportItemViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: TripResultTransportItemViewModel

    @RelaxedMockK
    lateinit var mockView: View

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = TripResultTransportItemViewModel()
    }

    @Test
    fun `setup should initialize modeId and modeIconId`() {
        // Arrange
        val mode = TransportMode().apply {
            id = "bus"
            iconId = "bus_icon"
        }

        // Act
        viewModel.setup(mode)

        // Assert
        assertEquals("bus", viewModel.modeId.value)
        assertEquals("bus_icon", viewModel.modeIconId.value)
    }

    @Test
    fun `onItemClick should toggle checked state and emit clicked event`() {
        // Arrange
        viewModel.modeId.value = "train"
        viewModel.checked.value = false
        val testObserver = TestObserver<Pair<String, Boolean>>()
        viewModel.clicked.subscribe(testObserver)

        // Act
        viewModel.onItemClick(mockView)

        // Assert
        assertTrue(viewModel.checked.value ?: false)
        testObserver.assertValue("train" to true)

        // Act again (clicking again should toggle it back)
        viewModel.onItemClick(mockView)

        // Assert
        assertFalse(viewModel.checked.value ?: false)
        testObserver.assertValues("train" to true, "train" to false)
    }
}
