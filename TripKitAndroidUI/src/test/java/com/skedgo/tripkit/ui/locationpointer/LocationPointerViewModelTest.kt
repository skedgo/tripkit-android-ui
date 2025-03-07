package com.skedgo.tripkit.ui.locationpointer

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.base.MockKTest
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository
import io.mockk.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
class LocationPointerViewModelTest: MockKTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: LocationPointerViewModel
    private val mockContext: Context = mockk()
    private val mockLocationHistoryRepository: LocationHistoryRepository = mockk()
    private val mockGeocoder: AndroidGeocoder = mockk()
    
    private val latLng = LatLng(-33.8688, 151.2093)
    private val testAddress = "Test Address"
    private val mockLocation: Location = mockk()

    @Before
    fun setup() {
        initRx()
        // Mock AndroidGeocoder behavior
        every { mockGeocoder.getAddress(any(), any()) } returns Observable.just(testAddress)

        // Spy on ViewModel to inject mockGeocoder
        viewModel = spyk(LocationPointerViewModel(mockContext, mockLocationHistoryRepository), recordPrivateCalls = true)
        viewModel.geocoder = mockGeocoder
    }

    @After
    fun teardown() {
        tearDownRx()
    }

    @Ignore
    @Test
    fun `test geocode result updates location text`() {
        // Initialize TestScheduler for RxJava
        val testScheduler = TestScheduler()
        val testAddress = "Test Address"
        val testLatLng = LatLng(37.7749, -122.4194)

        // Mock LiveData observers
        val locationTextObserver: Observer<String> = mockk(relaxed = true)
        val canChooseObserver: Observer<Boolean> = mockk(relaxed = true)
        val showSpinnerObserver: Observer<Boolean> = mockk(relaxed = true)
        val showInfoObserver: Observer<Boolean> = mockk(relaxed = true)

        viewModel.locationText.observeForever(locationTextObserver)
        viewModel.canChoose.observeForever(canChooseObserver)
        viewModel.showSpinner.observeForever(showSpinnerObserver)
        viewModel.showInfo.observeForever(showInfoObserver)

        // Mock the Geocoder to return the testAddress after delay
        every { viewModel.geocoder.getAddress(testLatLng.latitude, testLatLng.longitude) } returns
            Observable.just(testAddress)
                .delay(500, TimeUnit.MILLISECONDS, testScheduler)

        // Simulate map idle event
        viewModel.mapIdleThrottle.onNext(testLatLng)

        // Move the TestScheduler forward to process debounce and geocoder delay
        testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS) // For debounce
        testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS) // For geocoder delay

        // Capture emitted values
        val capturedValues = mutableListOf<String>()
        verify { locationTextObserver.onChanged(capture(capturedValues)) }

        // Debugging output
        println("Captured values: $capturedValues")

        // Assert that expected address is captured
        assertTrue("Expected $testAddress but got $capturedValues", capturedValues.contains(testAddress))

        // Verify LiveData updates
        verify { canChooseObserver.onChanged(true) }
        verify { showSpinnerObserver.onChanged(false) }
        verify { showInfoObserver.onChanged(true) }
    }

    @Test
    fun `test map move started updates UI state`() {
        // Mock LiveData observers
        val locationTextObserver: Observer<String> = mockk(relaxed = true)
        val canChooseObserver: Observer<Boolean> = mockk(relaxed = true)
        val showSpinnerObserver: Observer<Boolean> = mockk(relaxed = true)

        viewModel.locationText.observeForever(locationTextObserver)
        viewModel.canChoose.observeForever(canChooseObserver)
        viewModel.showSpinner.observeForever(showSpinnerObserver)

        // Call mapMoveStarted()
        viewModel.mapMoveStarted()

        // Verify UI updates
        verify { locationTextObserver.onChanged("") }
        verify { canChooseObserver.onChanged(false) }
        verify { showSpinnerObserver.onChanged(true) }
    }

    @Test
    fun `test saveLocation calls repository`() {
        every { mockLocationHistoryRepository.saveLocationsToHistory(any()) } returns Completable.complete()

        viewModel.saveLocation(mockLocation)

        verify { mockLocationHistoryRepository.saveLocationsToHistory(listOf(mockLocation)) }
    }
}
