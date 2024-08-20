package com.skedgo.tripkit.tripplanner

import org.joda.time.DateTimeZone

sealed class NonCurrentType {
    abstract val lat: Double
    abstract val lng: Double
    abstract val name: String?
    abstract val address: String?
    abstract val dateTimeZone: DateTimeZone?

    data class Normal(
        override val lat: Double,
        override val lng: Double,
        override val name: String? = null,
        override val address: String? = null,
        override val dateTimeZone: DateTimeZone? = null
    ) : NonCurrentType()

    data class Stop(
        override val lat: Double,
        override val lng: Double,
        override val name: String? = null,
        override val address: String? = null,
        override val dateTimeZone: DateTimeZone? = null
    ) : NonCurrentType()
}
