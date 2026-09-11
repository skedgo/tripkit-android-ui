package com.skedgo.tripkit.tripplanner

import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DiffTransformer] turns a stream of "everything that should be on the map now" into
 * insert/remove deltas, and it keeps the previous list in a `scan`.
 *
 * That accumulator is **per subscription**. These tests pin that behaviour because the map
 * relies on it: `TripKitMapFragment.loadMarkers()` must keep exactly one live subscription to
 * `MapViewModel.markers`, otherwise two independent diffs mutate the same marker map and one
 * subscriber removes the markers the other just added (#25936).
 */
class DiffTransformerTest {

    private fun transformer() =
        DiffTransformer<String, String>({ it }, { Single.just("marker-$it") })

    @Test
    fun `first emission inserts everything and removes nothing`() {
        val source = PublishSubject.create<List<String>>()
        val observer = source.compose(transformer()).test()

        source.onNext(listOf("a", "b"))

        val (inserted, removed) = observer.values().single()
        assertEquals(listOf("a", "b"), inserted.map { it.second })
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `later emissions only report the delta`() {
        val source = PublishSubject.create<List<String>>()
        val observer = source.compose(transformer()).test()

        source.onNext(listOf("a", "b"))
        source.onNext(listOf("b", "c"))

        val (inserted, removed) = observer.values()[1]
        assertEquals(listOf("c"), inserted.map { it.second })
        assertEquals(setOf("a"), removed)
    }

    @Test
    fun `an unchanged list produces no inserts and no removals`() {
        val source = PublishSubject.create<List<String>>()
        val observer = source.compose(transformer()).test()

        source.onNext(listOf("a", "b"))
        source.onNext(listOf("a", "b"))

        val (inserted, removed) = observer.values()[1]
        assertTrue(inserted.isEmpty())
        assertTrue(removed.isEmpty())
    }

    /**
     * The regression this guards: a second subscriber starts from an empty accumulator, so it
     * re-reports markers that are already on the map as insertions. Two subscribers writing
     * into one marker collection therefore fight, which is what made stops appear and then
     * disappear on the real device.
     */
    @Test
    fun `each subscription keeps its own diff state`() {
        val source = PublishSubject.create<List<String>>()
        val composed = source.compose(transformer())

        val first = composed.test()
        source.onNext(listOf("a", "b"))

        val second = composed.test()
        source.onNext(listOf("a", "b"))

        // The established subscriber correctly sees no change...
        val (firstInserted, firstRemoved) = first.values()[1]
        assertTrue(firstInserted.isEmpty())
        assertTrue(firstRemoved.isEmpty())

        // ...while a second subscriber re-inserts the same markers from scratch.
        val (secondInserted, _) = second.values().single()
        assertEquals(listOf("a", "b"), secondInserted.map { it.second })
    }
}
