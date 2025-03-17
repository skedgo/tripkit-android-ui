package com.skedgo.tripkit.ui.tripresults

import android.view.View
import com.skedgo.tripkit.common.model.TransportMode
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.reactivex.observers.TestObserver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TripResultTransportItemViewModelTest {

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
        assertEquals("bus", viewModel.modeId.get())
        assertEquals("bus_icon", viewModel.modeIconId.get())
    }

    @Test
    fun `onItemClick should toggle checked state and emit clicked event`() {
        // Arrange
        viewModel.modeId.set("train")
        viewModel.checked.set(false)
        val testObserver = TestObserver<Pair<String, Boolean>>()
        viewModel.clicked.subscribe(testObserver)

        // Act
        viewModel.onItemClick(mockView)

        // Assert
        assertTrue(viewModel.checked.get())
        testObserver.assertValue("train" to true)

        // Act again (clicking again should toggle it back)
        viewModel.onItemClick(mockView)

        // Assert
        assertFalse(viewModel.checked.get())
        testObserver.assertValues("train" to true, "train" to false)
    }
}
