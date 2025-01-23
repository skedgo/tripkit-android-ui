package com.skedgo.tripkit.ui.routingresults

import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.rxtry.subscribeWithErrorHandling
import io.reactivex.Observable

open class FetchingRealtimeStatusRepository {
    private val cache = BehaviorRelay
        .createDefault<Map<String, Boolean>>(emptyMap<String, Boolean>())
        .toSerialized()

    fun get(tripGroupId: String): Observable<Boolean> =
        cache.hide()
            .map { it.getOrElse(tripGroupId, { false }) }

    fun put(tripGroupId: String, isFetching: Boolean) {
        cache.firstOrError()
            .map { it.plus(Pair(tripGroupId, isFetching)) }
            .subscribeWithErrorHandling { success ->
                cache.accept(success)
            }
    }
}
