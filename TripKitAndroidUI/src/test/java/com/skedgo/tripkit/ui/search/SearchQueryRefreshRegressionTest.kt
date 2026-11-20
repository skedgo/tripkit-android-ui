package com.skedgo.tripkit.ui.search

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.TripKit
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.geocoding.AutoCompleteResult
import com.skedgo.tripkit.ui.geocoding.NoResult
import io.mockk.*
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@LooperMode(LooperMode.Mode.PAUSED)
class SearchQueryRefreshRegressionTest {
    private val clock = TestScheduler()
    private val dispatcher = StandardTestDispatcher()
    private val fetcher = mockk<FetchSuggestions>()
    private val provider = mockk<LocationSearchProvider>()
    private val errors = mockk<LocationSearchErrorViewModel>(relaxed = true)
    private val responses = mutableMapOf<String, PublishSubject<AutoCompleteResult>>()
    private lateinit var model: LocationSearchViewModel

    @Before fun setUp() {
        RxJavaPlugins.setComputationSchedulerHandler { clock }
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        Dispatchers.setMain(dispatcher)
        val configs = mockk<Configs>(relaxed = true)
        every { configs.locationFilter } returns null
        val tripKit = mockk<TripKit>(relaxed = true)
        every { tripKit.configs() } returns configs
        mockkObject(TripKit)
        every { TripKit.getInstance() } returns tripKit
        every { fetcher.query(any()) } answers {
            responses.getOrPut(firstArg<FetchLocationsParameters>().term()) { PublishSubject.create() }
        }
        every { errors.retryObservable } returns Observable.never()
        coEvery { provider.query(any(), any(), any()) } returns emptyList()
        val fixed = mockk<FixedSuggestionsProvider>(relaxed = true)
        every { fixed.specificSuggestions(any(), any(), any()) } returns emptyList()
        every { fixed.fixedSuggestions(any(), any()) } returns emptyList()
        every { fixed.locationsToSuggestion(any(), any(), any()) } returns emptyList()
        val schedulers = mockk<SchedulerFactory>()
        every { schedulers.ioScheduler } returns Schedulers.trampoline()
        every { schedulers.mainScheduler } returns Schedulers.trampoline()
        every { schedulers.computationScheduler } returns Schedulers.trampoline()
        val history = mockk<com.skedgo.tripkit.ui.database.location_history.LocationHistoryRepository>(relaxed = true)
        every { history.getLatestLocationHistory(any()) } returns io.reactivex.Single.just(emptyList())
        model = LocationSearchViewModel(ApplicationProvider.getApplicationContext<Context>(), mockk(relaxed = true),
            mockk(relaxed = true), fetcher, mockk(relaxed = true), mockk(relaxed = true),
            schedulers, history, errors, mockk(relaxed = true)).apply {
            locationSearchProvider = provider
            fixedSuggestionsProvider = fixed
            handleArgs(Bundle().apply {
                putParcelable(KEY_BOUNDS, LatLngBounds(LatLng(-34.0, 150.0), LatLng(-33.0, 152.0)))
                putParcelable(KEY_CENTER, LatLng(-33.8, 151.2))
            })
        }
    }

    @After fun tearDown() {
        model.onCleared()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
        unmockkAll()
    }

    private fun advance(millis: Long) {
        clock.advanceTimeBy(millis, TimeUnit.MILLISECONDS)
        dispatcher.scheduler.runCurrent()
    }

    @Test fun `unchanged query neither repeats requests nor resets its result rows`() {
        val queries = model.queries.test()
        model.onQueryTextChanged("Town", true)
        advance(800)
        model.onQueryTextChanged("Town", true)
        advance(2000)
        queries.assertValueCount(1)
        verify(exactly = 1) { fetcher.query(any()) }
        coVerify(exactly = 1) { provider.query(any(), any(), "Town") }
    }

    @Test fun `old API result cannot replace a newly typed query during debounce`() {
        model.onQueryTextChanged("Town", true)
        advance(800)
        model.onQueryTextChanged("Town Hall", true)
        clearMocks(errors, answers = false)
        responses.getValue("Town").onNext(NoResult("Town"))
        advance(500)
        verify(exactly = 0) { errors.updateError(any()) }
        advance(300)
        responses.getValue("Town Hall").onNext(NoResult("Town Hall"))
        advance(500)
        verify(exactly = 1) { errors.updateError(SearchErrorType.NoResults("Town Hall")) }
    }

    @Test fun `out of order provider completion retains newest suggestions and selection works`() {
        val old = CompletableDeferred<List<SearchSuggestion>>()
        val newest = CompletableDeferred<List<SearchSuggestion>>()
        coEvery { provider.query(any(), any(), "Town") } coAnswers { withContext(NonCancellable) { old.await() } }
        coEvery { provider.query(any(), any(), "Town Hall") } coAnswers { newest.await() }
        model.onQueryTextChanged("Town", true)
        advance(800)
        model.onQueryTextChanged("Town Hall", true)
        advance(800)
        val location = Location(-33.87, 151.20).apply { name = "Town Hall" }
        val suggestion = DefaultSearchSuggestion("new", "Town Hall", null, R.color.labelPrimary,
            R.color.labelSecondary, ColorDrawable(0xff000000.toInt()), location)
        newest.complete(listOf(suggestion))
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("Town Hall"), model.providedSuggestions.map { it.title })
        old.complete(emptyList())
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf("Town Hall"), model.providedSuggestions.map { it.title })
        val selected = model.locationChosen.test()
        model.providedSuggestions.single().onItemClicked.perform()
        selected.assertValue(location)
    }

    @Test fun `queued query is consumed once when list attachment is reported repeatedly`() {
        val wrapper = TKUILocationSearchViewControllerFragment()
        val child = mockk<LocationSearchFragment>(relaxed = true)
        var added = false
        every { child.isAdded } answers { added }
        TKUILocationSearchViewControllerFragment::class.java.getDeclaredField("locationSearchFragment")
            .apply { isAccessible = true }.set(wrapper, child)
        wrapper.setQuery("Town Hall", true)
        added = true
        val deliver = wrapper.javaClass.getDeclaredMethod("deliverPendingQuery").apply { isAccessible = true }
        repeat(3) { deliver.invoke(wrapper) }
        verify(exactly = 1) { child.setQuery("Town Hall", true) }
    }
}
