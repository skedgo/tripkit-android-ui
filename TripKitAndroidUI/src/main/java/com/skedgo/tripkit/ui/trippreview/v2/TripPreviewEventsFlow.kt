package com.skedgo.tripkit.ui.trippreview.v2

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class TripPreviewEvent
object TripPreviewEventsFlow {

    private val _event = MutableSharedFlow<TripPreviewEvent>(replay = 0)
    val event = _event.asSharedFlow()

    suspend fun invokeEvent(event: TripPreviewEvent) = _event.emit(event)
}

data class OnPreviewToolbarStateUpdate(val state: PreviewToolbarState): TripPreviewEvent()