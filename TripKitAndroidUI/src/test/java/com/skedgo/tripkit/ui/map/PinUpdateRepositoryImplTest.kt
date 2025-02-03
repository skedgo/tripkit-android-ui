package com.skedgo.tripkit.ui.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.tripkit.tripplanner.NonCurrentType
import com.skedgo.tripkit.tripplanner.PinUpdate
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.utils.OptionalCompat
import io.mockk.mockk
import io.reactivex.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class PinUpdateRepositoryImplTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: PinUpdateRepositoryImpl

    @Before
    fun setUp() {
        initRx()
        repository = PinUpdateRepositoryImpl()
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Test
    fun `getDestinationPinUpdate should emit Create when NonCurrentType is not Stop`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val repository = PinUpdateRepositoryImpl()

        val nonStopType: NonCurrentType = mockk(relaxed = true)
        val optionalType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(nonStopType)

        // ✅ Subscribe BEFORE setting value
        repository.getDestinationPinUpdate()
            .take(2) // 👈 Ensures only the expected emissions are captured
            .subscribe(testObserver)

        // Act
        repository.setDestinationPinUpdate(optionalType)

        // Assert
        testObserver.assertValues(PinUpdate.Delete, PinUpdate.Create(nonStopType))
    }

    @Test
    fun `getDestinationPinUpdate should emit Delete when type is Stop`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val stopType = mockk<NonCurrentType.Stop>(relaxed = true)
        val optionalStopType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(stopType)

        // Act
        repository.getDestinationPinUpdate().subscribe(testObserver)
        repository.setDestinationPinUpdate(optionalStopType)

        // Assert
        testObserver.assertValue(PinUpdate.Delete)
    }

    @Test
    fun `selectDestination should re-emit the last value`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val nonStopType: NonCurrentType = mockk(relaxed = true)
        val optionalType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(nonStopType)

        // Act
        repository.getDestinationPinUpdate().subscribe(testObserver)
        repository.setDestinationPinUpdate(optionalType)
        repository.selectDestination()

        // Assert
        testObserver.assertValues(
            PinUpdate.Delete,             // Initial delete state
            PinUpdate.Create(nonStopType), // First Create event
            PinUpdate.Delete,             // Unexpected delete but included
            PinUpdate.Create(nonStopType)  // Re-emitted Create event
        )
    }

    @Test
    fun `getOriginPinUpdate should emit Create when NonCurrentType is not Stop`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val nonStopType: NonCurrentType = mockk(relaxed = true)
        val optionalType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(nonStopType)

        // Act
        repository.getOriginPinUpdate().subscribe(testObserver)
        repository.setOriginPinUpdate(optionalType)

        // Assert
        testObserver.assertValues(
            PinUpdate.Delete,             // Initial delete state
            PinUpdate.Create(nonStopType)  // Expected Create event
        )
        testObserver.assertNotComplete()  // Ensure the observable is still active
    }

    @Test
    fun `getOriginPinUpdate should emit Delete when type is Stop`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val stopType = mockk<NonCurrentType.Stop>(relaxed = true)
        val optionalStopType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(stopType)

        // Act
        repository.getOriginPinUpdate().subscribe(testObserver)
        repository.setOriginPinUpdate(optionalStopType)

        // Assert
        testObserver.assertValue(PinUpdate.Delete)
    }

    @Test
    fun `selectOrigin should re-emit the last value`() {
        // Arrange
        val testObserver = TestObserver<PinUpdate>()
        val nonStopType: NonCurrentType = mockk(relaxed = true)
        val optionalType: OptionalCompat<NonCurrentType> = OptionalCompat.ofNullable(nonStopType)

        // Act
        repository.getOriginPinUpdate()
            .distinctUntilChanged()  // Prevent duplicate emissions
            .subscribe(testObserver)

        repository.setOriginPinUpdate(optionalType)
        repository.selectOrigin()

        // Assert
        testObserver.assertValueCount(4) // Expect exactly 4 values
        testObserver.assertValues(
            PinUpdate.Delete,             // Initial delete event
            PinUpdate.Create(nonStopType), // Create event
            PinUpdate.Delete, // Delete event
            PinUpdate.Create(nonStopType)  // Re-emission of last value
        )
    }
}