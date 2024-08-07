package com.skedgo.tripkit.tripplanner

import com.gojuno.koptional.Optional
import io.reactivex.Observable

interface PinUpdateRepository {
    fun getDestinationPinUpdate(): Observable<PinUpdate>
    fun getOriginPinUpdate(): Observable<PinUpdate>
    fun setDestinationPinUpdate(type: Optional<NonCurrentType>)
    fun setOriginPinUpdate(type: Optional<NonCurrentType>)
    fun selectDestination()
    fun selectOrigin()
}
