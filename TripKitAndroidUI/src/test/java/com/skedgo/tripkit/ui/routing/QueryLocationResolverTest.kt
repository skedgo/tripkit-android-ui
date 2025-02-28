package com.skedgo.tripkit.ui.routing

import android.location.Location
import com.skedgo.tripkit.common.model.Query
import io.mockk.*
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class QueryLocationResolverTest {

    private lateinit var resolver: QueryLocationResolver
    private val provider: Provider<Observable<Location>> = mockk()

    @Before
    fun setUp() {
        resolver = QueryLocationResolver(provider)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `apply should resolve origin location when origin is current and destination is not`() {
        val query = spyk(Query()) // Use a real instance instead of a mock
        val location: Location = mockk {
            every { latitude } returns 10.0
            every { longitude } returns 20.0
        }

        every { query.originIsCurrentLocation() } returns true
        every { query.destinationIsCurrentLocation() } returns false
        every { provider.get() } returns Observable.just(location)

        val testObserver = resolver.apply(query).test()

        testObserver.assertValue {
            it.fromLocation?.lat == 10.0 &&
                it.fromLocation?.lon == 20.0
        }
        testObserver.assertComplete()
    }

    @Test
    fun `apply should resolve destination location when destination is current and origin is not`() {
        val query: Query = spyk(Query())
        val location: Location = mockk {
            every { latitude } returns 30.0
            every { longitude } returns 40.0
        }

        every { query.originIsCurrentLocation() } returns false
        every { query.destinationIsCurrentLocation() } returns true
        every { provider.get() } returns Observable.just(location)

        val testObserver = resolver.apply(query).test()

        testObserver.assertValue {
            it.toLocation?.lat == 30.0 &&
            it.toLocation?.lon == 40.0
        }
        testObserver.assertComplete()
    }

    @Test
    fun `apply should return the same query when neither origin nor destination are current location`() {
        val query: Query = mockk(relaxed = true)

        every { query.originIsCurrentLocation() } returns false
        every { query.destinationIsCurrentLocation() } returns false

        val testObserver = resolver.apply(query).test()

        testObserver.assertValue(query)
        testObserver.assertComplete()
    }

    @Test
    fun `apply should emit error when both origin and destination are current location`() {
        val query: Query = mockk(relaxed = true)

        every { query.originIsCurrentLocation() } returns true
        every { query.destinationIsCurrentLocation() } returns true

        val testObserver = resolver.apply(query).test()

        testObserver.assertError(NullPointerException::class.java)
    }
}
