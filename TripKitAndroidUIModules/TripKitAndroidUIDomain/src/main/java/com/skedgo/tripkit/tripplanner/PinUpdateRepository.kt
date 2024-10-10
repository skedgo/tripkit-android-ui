package com.skedgo.tripkit.tripplanner

import com.skedgo.tripkit.utils.OptionalCompat
import io.reactivex.Observable

interface PinUpdateRepository {
    fun getDestinationPinUpdate(): Observable<PinUpdate>
    fun getOriginPinUpdate(): Observable<PinUpdate>
    fun setDestinationPinUpdate(type: OptionalCompat<NonCurrentType>)
    fun setOriginPinUpdate(type: OptionalCompat<NonCurrentType>)
    fun selectDestination()
    fun selectOrigin()
}
