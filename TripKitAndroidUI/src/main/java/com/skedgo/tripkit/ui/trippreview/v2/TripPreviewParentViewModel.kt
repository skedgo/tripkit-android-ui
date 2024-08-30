package com.skedgo.tripkit.ui.trippreview.v2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.correctItemType
import javax.inject.Inject

sealed interface PreviewToolbarState {

    val showBackButton: Boolean
    val backActionLabel: String
    val title: String

    data class OnPreview(
        override val showBackButton: Boolean = false,
        override val backActionLabel: String,
        override val title: String
    ) : PreviewToolbarState

    data class OnBooking(
        override val showBackButton: Boolean = true,
        override val backActionLabel: String,
        override val title: String
    ) : PreviewToolbarState
}

class TripPreviewParentViewModel @Inject constructor() : RxViewModel() {

    private val _toolbarState = MutableLiveData<PreviewToolbarState>()
    val toolbarState: LiveData<PreviewToolbarState> = _toolbarState

    private val _segmentItemType = MutableLiveData<Int>()
    val segmentItemType: LiveData<Int> = _segmentItemType

    private val _tripSegment = MutableLiveData<TripSegment>()
    private val tripSegmentObserver = Observer<TripSegment> {
        _segmentItemType.postValue(it.correctItemType())
    }

    init {
        _tripSegment.observeForever(tripSegmentObserver)
    }

    fun setState(state: PreviewToolbarState) {
        _toolbarState.postValue(state)
    }

    fun setTripSegment(segment: TripSegment) {
        _tripSegment.postValue(segment)
    }
}