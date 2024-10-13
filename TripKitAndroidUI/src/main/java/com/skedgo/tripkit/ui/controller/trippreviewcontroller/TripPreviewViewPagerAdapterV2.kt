package com.skedgo.tripkit.ui.controller.trippreviewcontroller

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.payment.PaymentData
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.trippreview.Action
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerListener
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import com.skedgo.tripkit.ui.utils.ITEM_DIRECTIONS
import com.skedgo.tripkit.ui.utils.ITEM_EXTERNAL_BOOKING
import com.skedgo.tripkit.ui.utils.ITEM_MODE_LOCATION
import com.skedgo.tripkit.ui.utils.ITEM_NEARBY
import com.skedgo.tripkit.ui.utils.ITEM_SERVICE
import com.skedgo.tripkit.ui.utils.ITEM_TIMETABLE
import com.skedgo.tripkit.ui.utils.ITEM_TIMETABLE_PAYMENT
import com.skedgo.tripkit.ui.utils.correctItemType
import io.reactivex.subjects.PublishSubject

class TripPreviewViewPagerAdapterV2(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    internal var onSwipePage: (Boolean) -> Unit = { _ -> }

    var pages = mutableListOf<TripPreviewPagerAdapterItem>()
    var onCloseButtonListener: View.OnClickListener? = null
    var tripPreviewPagerListener: TripPreviewPagerListener? = null
    var paymentDataStream: PublishSubject<PaymentData>? = null
    var ticketActionStream: PublishSubject<String>? = null

    var segmentActionStream = PublishSubject.create<TripSegment>()
    var bookingActions: List<String>? = null

    internal var externalActionCallback: ((TripSegment?, Action?) -> Unit)? = null
    internal var bottomSheetDragToggleCallback: ((Boolean) -> Unit)? = null

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DIRECTIONS -> {
                DirectionsTripPreviewItemFragment.newInstance(page.tripSegment)
            }

            ITEM_NEARBY -> {
                NearbyTripPreviewItemFragment.newInstance(page.tripSegment)
            }

            ITEM_MODE_LOCATION -> {
                ModeLocationTripPreviewItemFragment.newInstance(page.tripSegment) { segment, url ->
                    externalActionCallback?.invoke(segment, url)
                }
            }

            ITEM_EXTERNAL_BOOKING -> {
                ExternalActionTripPreviewItemFragment.newInstance(page.tripSegment) { segment, action ->
                    externalActionCallback?.invoke(segment, action)
                }
            }

            ITEM_SERVICE -> {
                val fragment = ServiceTripPreviewItemFragment.newInstance(
                    page.tripSegment,
                    position,
                    true
                )
                fragment.onNextPage = {
                    onSwipePage.invoke(true)
                }
                fragment.onPreviousPage = {
                    onSwipePage.invoke(false)
                }
                fragment
            }

            ITEM_TIMETABLE -> {
                val scheduledStop = ScheduledStop(page.tripSegment.to).apply {
                    code = page.tripSegment.startStopCode
                    endStopCode = page.tripSegment.endStopCode
                    modeInfo = page.tripSegment.modeInfo
                    type = StopType.from(page.tripSegment.modeInfo?.localIconName.orEmpty())
                }
                val timetableFragment = TimetableFragment.Builder()
                    .withStop(scheduledStop)
                    .withBookingAction(page.tripSegment.booking?.externalActions)
                    .withSegmentActionStream(segmentActionStream)
                    .withTripSegment(page.tripSegment)
                    .hideSearchBar()
                    .showCloseButton()
                    .isFromPreview(true)
                    .build().apply {
                        _tripSegment = page.tripSegment
                        onNextPage = {
                            onSwipePage.invoke(true)
                        }
                        onPreviousPage = {
                            onSwipePage.invoke(false)
                        }
                    }
                bookingActions = page.tripSegment.booking?.externalActions
                timetableFragment
            }

            else -> StandardTripPreviewItemFragment.newInstance(page.tripSegment)
        }
        fragment.tripPreviewPagerListener = tripPreviewPagerListener
        fragment.onCloseButtonListener = onCloseButtonListener
        return fragment
    }

    fun getSegmentByPosition(position: Int): TripSegment {
        return pages[position].tripSegment
    }

    fun getSegmentPositionById(pair: Pair<Long, String>): Int {
        return pages.indexOfFirst { it.tripSegment.segmentId == pair.first }
    }

    fun setTripSegments(
        activeTripSegmentId: Long,
        tripSegments: List<TripSegment>,
        fromAction: Boolean = false
    ): Int {
        var activeTripSegmentPosition = 0
        var addedCards = 0
        val temp = ArrayList<TripPreviewPagerAdapterItem>()
        tripSegments.forEachIndexed { index, segment ->
            val itemType = segment.correctItemType()

            if (itemType == ITEM_SERVICE) {
                if (activeTripSegmentId == segment.segmentId && itemType == ITEM_SERVICE
                    && activeTripSegmentPosition <= 0
                ) {
                    activeTripSegmentPosition = index + addedCards
                }
                temp.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE, segment))
                addedCards++
            }

            if (itemType == ITEM_NEARBY) {
                if (activeTripSegmentId == segment.segmentId && itemType == ITEM_NEARBY
                    && activeTripSegmentPosition <= 0
                ) {
                    activeTripSegmentPosition = index + addedCards
                }
                temp.add(TripPreviewPagerAdapterItem(ITEM_MODE_LOCATION, segment))
                addedCards++
            } else {
                temp.add(TripPreviewPagerAdapterItem(itemType, segment))

                if (itemType == ITEM_SERVICE && segment.ticket != null && (!segment.booking?.quickBookingsUrl.isNullOrEmpty() || !segment.booking?.confirmation?.tickets()
                        ?.first()?.purchasedTickets().isNullOrEmpty())
                ) {
                    temp.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE_PAYMENT, segment))
                    addedCards++
                }
            }

            if (activeTripSegmentId == segment.segmentId && activeTripSegmentPosition <= 0) {
                activeTripSegmentPosition = index + addedCards
            }
        }

        pages = temp.toMutableList()
        notifyDataSetChanged()

        if (fromAction) {
            activeTripSegmentPosition = pages.indexOfFirst {
                !it.tripSegment.booking?.externalActions.isNullOrEmpty()
            }
        } else {
            if (activeTripSegmentPosition < 0) {
                activeTripSegmentPosition = 0
            }
        }
        return activeTripSegmentPosition
    }
}
