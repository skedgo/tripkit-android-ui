package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import me.tatarka.bindingcollectionadapter2.ItemBinding
import androidx.databinding.library.baseAdapters.BR
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.util.TripSegmentUtils
import com.skedgo.tripkit.routing.SegmentType
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider


class TripPreviewPagerFragmentViewModel
/*@Inject internal constructor(private val context: Context,
                                                                     private val genericItemProvider: Provider<TripPreviewPagerItemViewModel>,
                                                                     private val directionsItemProvider: Provider<TripPreviewPagerItemDirectionsViewModel>,
                                                                     private val nearbyItemProvider: Provider<TripPreviewPagerItemNearbyViewModel>,
                                                                     private val tripGroupRepository: TripGroupRepository)*/
    : RxViewModel() {

//    val currentPage: ObservableInt = ObservableInt()
//    val itemBinding = ItemBinding.of(OnItemBindClass<Any>()
//            .map(TripPreviewPagerItemDirectionsViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_directions_item)
//            .map(TripPreviewPagerItemViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_item)
////            .map(TripPreviewPagerItemNearbyViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_item)
//            .map(TripPreviewPagerItemModeLocationViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_item)
////            .map(TripPreviewPagerItemTimetableViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_item)
////            .map(TripPreviewPagerItemQuickBookingViewModel::class.java, BR.viewModel, R.layout.trip_preview_pager_item)
//    )
//    val items = ObservableArrayList<TripPreviewPagerItemViewModel>()
//
//    val tripSegments = ObservableField<List<TripSegment>>(emptyList())
//    val closeClick = PublishRelay.create<Unit>()
//    fun load(tripGroupId: String, tripId: String, tripSegmentId: Long) {
//        tripGroupRepository.getTripGroup(tripGroupId)
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { tripGroup ->
//                    val trip = tripGroup.trips?.find { it.uuid() == tripId }
//                    trip?.let {
//                        items.clear()
//                        tripSegments.set(trip.segments)
//                        trip.segments
//                                .filter { !it.isContinuation }
//                                .filter { it.type != SegmentType.DEPARTURE && it.type != SegmentType.ARRIVAL }
//                                .forEachIndexed { i, segment ->
//                                    val vms = getViewModels(segment)
//                                    vms.forEach { vm ->
//                                        vm.setSegment(context, segment)
//                                        vm.closeClicked.observable.subscribe {
//                                            closeClick.accept(Unit)
//                                        }.autoClear()
//                                    }
//                                    if (tripSegmentId == segment.templateHashCode) {
//                                        currentPage.set(i)
//                                    }
//                                    items.addAll(vms)
//                        }
//                    }
//                }
//                .autoClear()
//    }

//    private fun getViewModels(segment: TripSegment): List<TripPreviewPagerItemViewModel> {
//        if (segment.turnByTurn != null) {
//            return listOf(directionsItemProvider.get())
//        } else if (segment.mode?.isPublicTransport == true) {
//            // TripPreviewPagerItemTimetableViewModel
//            return listOf(genericItemProvider.get())
//        } else if (segment.modeInfo?.id == "stationary_vehicle-collect" || segment.hasCarParks()) {
////            return nearbyItemProvider.get()
//            return listOf(genericItemProvider.get())
//
//        } else if (segment.booking?.quickBookingsUrl != null || segment.booking?.confirmation != null) {
//            // TripPreviewPagerItemQuickBookingViewModel
//            return listOf(genericItemProvider.get())
//        } else if (segment.booking?.externalActions != null && segment.booking.externalActions!!.count() > 0) {
//            // External booking
//            return listOf(genericItemProvider.get())
//        } else {
//            return listOf(genericItemProvider.get())
//        }
//        // else if segment.getMode().isPublicTransport -> load TripPreviewPagerItemTimetableViewModel
//        // else if segment.booking.quickInternalUrl != null || bookingConfirmation !- null -> load QuickBooking
//        // // else if segment stationaryType == vehicleCollecT || hasCarParks -> load Nearby
//    }
}