package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.tripresults.GetTransportIconTintStrategy
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TripPreviewPagerViewModel @Inject constructor(
    private val tripGroupRepository: TripGroupRepository
) : RxViewModel() {

    private val _headers = MutableLiveData<List<TripSegmentSummary>>()
    val headers: LiveData<List<TripSegmentSummary>> = _headers

    private val tripSummaryStream = PublishSubject.create<List<TripSegmentSummary>>()

    private val _tripGroup = MutableLiveData<TripGroup>()
    val tripGroup: LiveData<TripGroup> = _tripGroup

    private val _tripGroupFromPolling = MutableLiveData<TripGroup>()
    val tripGroupFromPolling: LiveData<TripGroup> = _tripGroupFromPolling

    fun generatePreviewHeaders(
        context: Context,
        tripSegments: List<TripSegment>,
        getTransportIconTintStrategy: GetTransportIconTintStrategy
    ) {
        tripSummaryStream
            .debounce(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ headers ->
                _headers.value = headers.sortedBy { it.id }
            }, {
                //This will prevent app from crashing due to OnErrorNotImplementedException
                it.printStackTrace()
            })
            .autoClear()

        val previewHeaders = mutableListOf<TripSegmentSummary>()

        tripSegments.filter { it.visibility == Visibilities.VISIBILITY_IN_SUMMARY }
            .forEach { segment ->
                getSegmentIcon(context, segment, getTransportIconTintStrategy) {
                    if (previewHeaders.none { it.id == segment.id }) {
                        previewHeaders.add(segment.generateTripPreviewHeader(it))
                    }
                    tripSummaryStream.onNext(previewHeaders)
                }
            }
    }

    private fun getSegmentIcon(
        context: Context,
        segment: TripSegment,
        getTransportIconTintStrategy: GetTransportIconTintStrategy,
        icon: (Drawable) -> Unit
    ) {
        segment.getSegmentIconObservable(
            context, getTransportIconTintStrategy
        ).map { bitmapDrawable ->
            segment.createSummaryIcon(context, bitmapDrawable)
        }.subscribe({
            icon.invoke(it)
        },{
            Timber.e(it)
        }).autoClear()
    }

    fun loadTripGroup(
        tripGroupId: String
    ) {
        tripGroupRepository.getTripGroup(tripGroupId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ tripGroup ->
                _tripGroup.postValue(tripGroup)
            }, {
                it.printStackTrace()
            }).autoClear()
    }

    fun startUpdateTripPolling(tripGroupId: String) {
        Observable.interval(10L, TimeUnit.SECONDS, Schedulers.io())
            .subscribe {
                getUpdatedTrip(tripGroupId)
            }.autoClear()
    }

    private fun getUpdatedTrip(
        tripGroupId: String
    ) {
        tripGroupRepository.getTripGroup(tripGroupId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ tripGroup ->
                _tripGroupFromPolling.postValue(tripGroup)
            }, {
                it.printStackTrace()
            }).autoClear()
    }

    fun updateTrip(
        tripGroupId: String, oldTripUuid: String, trip: Trip
    ) {
        tripGroupRepository.updateTrip(tripGroupId, oldTripUuid, trip)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
            .autoClear()
    }

}