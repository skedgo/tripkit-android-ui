package com.skedgo.tripkit.camera

import com.skedgo.tripkit.time.GetNow
import org.joda.time.DateTime
import org.joda.time.Days
import javax.inject.Inject

open class IsCachedMapCameraPositionStale @Inject internal constructor(
    private val getNow: GetNow
) {
    /**
     * @param cachingDateTime Should be [CachedMapCameraPosition.cachingDateTime].
     */
    open fun execute(cachingDateTime: DateTime) =
        Math.abs(
            Days.daysBetween(
                getNow.execute().toLocalDate(),
                cachingDateTime.toLocalDate()
            ).days
        ) > 1
}
