package com.skedgo.tripkit.ui.map.home

import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Try
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.GoToMyLocationRepository
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/**
 * Covers the threading contract of [MapViewModel.myLocationError].
 *
 * `TripKitMapFragment.showMyLocationError()` shows a `Toast`, which only works on the main
 * thread. The signal it reacts to originates on a background thread: [MapViewModel] subscribes
 * to `GoToMyLocationRepository.myLocation` on `Schedulers.io()`, and the 1.5s timeout inside
 * `UserGeoPointRepositoryImpl` fires on the computation scheduler, so the `PublishRelay` behind
 * [MapViewModel.myLocationError] emits on whichever thread called `accept(...)`.
 *
 * These tests pin that down together with the distinction that caused the defect: on a hot
 * relay `subscribeOn` does **not** move delivery to the main thread, only `observeOn` does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MapViewModelMyLocationErrorTest {

    private companion object {
        const val LOCATION_THREAD = "fake-location-thread"
        const val AWAIT_SECONDS = 5L
    }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var locationExecutor: ExecutorService
    private lateinit var viewModel: MapViewModel
    private lateinit var goToMyLocationSignal: PublishSubject<Unit>
    private val disposables = CompositeDisposable()

    @Before
    fun setUp() {
        // AndroidSchedulers caches its main-thread scheduler in a JVM-wide static, and other
        // test classes in this module swap that for an immediate scheduler via MockKTest.initRx().
        // Pin AndroidSchedulers.mainThread() to Robolectric's real main looper for this class so
        // the assertions below do not depend on test execution order.
        RxAndroidPlugins.setMainThreadSchedulerHandler {
            AndroidSchedulers.from(Looper.getMainLooper())
        }
        locationExecutor = Executors.newSingleThreadExecutor { Thread(it, LOCATION_THREAD) }
        viewModel = viewModelFailingToLocate()
    }

    @After
    fun tearDown() {
        disposables.clear()
        RxAndroidPlugins.reset()
        locationExecutor.shutdownNow()
    }

    @Test
    fun `myLocationError emits the underlying failure when the location lookup fails`() {
        val latch = CountDownLatch(1)
        val received = AtomicReference<Throwable>()
        viewModel.myLocationError
            .subscribe { received.set(it); latch.countDown() }
            .let(disposables::add)

        viewModel.goToMyLocation()

        assertThat(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(received.get()).isInstanceOf(TimeoutException::class.java)
    }

    @Test
    fun `myLocationError reaches a raw subscriber off the main thread`() {
        // This is why the fragment cannot touch UI straight from the callback.
        val latch = CountDownLatch(1)
        val observedOn = AtomicReference<Thread>()
        viewModel.myLocationError
            .subscribe { observedOn.set(Thread.currentThread()); latch.countDown() }
            .let(disposables::add)

        viewModel.goToMyLocation()

        assertThat(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(observedOn.get()).isNotEqualTo(Looper.getMainLooper().thread)
    }

    @Test
    fun `subscribeOn does not move a hot relay emission onto the main thread`() {
        // The defect: TripKitMapFragment used subscribeOn here, which only chooses where the
        // subscription is established, never where a hot relay delivers.
        val latch = CountDownLatch(1)
        val observedOn = AtomicReference<Thread>()
        viewModel.myLocationError
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe { observedOn.set(Thread.currentThread()); latch.countDown() }
            .let(disposables::add)
        // Let the scheduled subscription actually attach to the relay.
        shadowOf(Looper.getMainLooper()).idle()

        viewModel.goToMyLocation()

        assertThat(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(observedOn.get()).isNotEqualTo(Looper.getMainLooper().thread)
    }

    @Test
    fun `observeOn delivers the error on the main thread`() {
        // The fix that TripKitMapFragment.onResume() now applies.
        val emitted = CountDownLatch(1)
        val observedOn = AtomicReference<Thread>()
        viewModel.myLocationError
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { observedOn.set(Thread.currentThread()) }
            .let(disposables::add)
        // A raw subscriber tells us when the background emission has happened.
        viewModel.myLocationError.subscribe { emitted.countDown() }.let(disposables::add)

        viewModel.goToMyLocation()

        assertThat(emitted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(observedOn.get()).isEqualTo(Looper.getMainLooper().thread)
    }

    @Test
    fun `a disposed subscription never reaches the error callback`() {
        // TripKitMapFragment subscribes in onResume() and AutoDisposable clears on ON_PAUSE, so
        // an in-flight emission must not run UI work against a torn-down fragment.
        var callbackRan = false
        val emitted = CountDownLatch(1)
        val disposable = viewModel.myLocationError
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { callbackRan = true }
        viewModel.myLocationError.subscribe { emitted.countDown() }.let(disposables::add)

        disposable.dispose()
        viewModel.goToMyLocation()

        assertThat(emitted.await(AWAIT_SECONDS, TimeUnit.SECONDS)).isTrue()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(callbackRan).isFalse()
        assertThat(disposable.isDisposed).isTrue()
    }

    /**
     * A [MapViewModel] whose location lookup always times out, emitting the failure on
     * [LOCATION_THREAD] the way the real `timeout()` operator emits on the computation
     * scheduler rather than on the caller's thread.
     */
    private fun viewModelFailingToLocate(): MapViewModel {
        goToMyLocationSignal = PublishSubject.create()
        val locationScheduler = Schedulers.from(locationExecutor)
        val goToMyLocationRepository = mockk<GoToMyLocationRepository>(relaxed = true)

        every { goToMyLocationRepository.myLocation } returns goToMyLocationSignal
            .switchMap {
                Observable.fromCallable<Try<GeoPoint>> {
                    Failure(TimeoutException("no location fix"))
                }.subscribeOn(locationScheduler)
            }
        every { goToMyLocationRepository.goToMyLocation() } answers {
            goToMyLocationSignal.onNext(Unit)
        }

        return MapViewModel(
            putMapCameraPosition = mockk(relaxed = true),
            getInitialMapCameraPosition = mockk(relaxed = true),
            pinUpdateRepository = mockk(relaxed = true),
            resources = mockk(relaxed = true),
            picasso = mockk(relaxed = true),
            goToMyLocationRepository = goToMyLocationRepository,
            fetchStopsByViewport = mockk(relaxed = true),
            getCellIdsFromViewPort = mockk(relaxed = true),
            loadPOILocationsByViewPort = mockk(relaxed = true),
            scheduledStopRepository = mockk(relaxed = true),
            errorLogger = mockk(relaxed = true)
        )
    }
}
