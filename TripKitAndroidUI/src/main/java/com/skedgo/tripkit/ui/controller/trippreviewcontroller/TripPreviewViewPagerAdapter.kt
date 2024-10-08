package com.skedgo.tripkit.ui.controller.trippreviewcontroller

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
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

data class TripPreviewPagerAdapterItem(val type: Int, var tripSegment: TripSegment)

// TODO for code refactoring
class TripPreviewPagerAdapter(fragmentManager: FragmentManager) :
    FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    internal var onSwipePage: (Boolean) -> Unit = { _ -> }

    var pages = mutableListOf<TripPreviewPagerAdapterItem>()
    var onCloseButtonListener: View.OnClickListener? = null
    var tripPreviewPagerListener: TripPreviewPagerListener? = null
    var paymentDataStream: PublishSubject<PaymentData>? = null
    var ticketActionStream: PublishSubject<String>? = null

    //To emit booking actions updates to TimetableFragment instead of getting and using the fragments instance
    var segmentActionStream = PublishSubject.create<TripSegment>()

    var bookingActions: List<String>? = null

    /**
     * Adding callback for handling external actions (3rd party applications booking) handling.
     * To also unload the handling using TripPreviewPagerFragment.Listener since code doesn't
     * have to go through a lot of classes just to handle the action since components being
     * used for handling such as BookingV2TrackingService, bookingResolver, etc. (as per tracking the code
     * from previous implementation) can be injected and be used in TripPreviewPagerFragment.
     */
    internal var externalActionCallback: ((TripSegment?, Action?) -> Unit)? = null

    internal var bottomSheetDragToggleCallback: ((Boolean) -> Unit)? = null

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DIRECTIONS -> {
                DirectionsTripPreviewItemFragment.newInstance(page.tripSegment) // Directions showing miles per item
            }

            ITEM_NEARBY -> {
                NearbyTripPreviewItemFragment.newInstance(page.tripSegment) // Happening nearby (collect bicycle, walk, etc)
            }

            ITEM_MODE_LOCATION -> {
                ModeLocationTripPreviewItemFragment.newInstance(page.tripSegment) { segment, url ->
                    externalActionCallback?.invoke(segment, url)
                } // modes
            }

            ITEM_EXTERNAL_BOOKING -> {
                ExternalActionTripPreviewItemFragment.newInstance(page.tripSegment) { segment, action ->
                    externalActionCallback?.invoke(segment, action)
                } // taxis, gocatch
            }

            ITEM_SERVICE -> {
                val fragment = ServiceTripPreviewItemFragment.newInstance(
                    page.tripSegment,
                    position,
                    true
                ) // PT with Stops
                fragment.onNextPage = {
                    onSwipePage.invoke(true)
                }
                fragment.onPreviousPage = {
                    onSwipePage.invoke(false)
                }
                fragment
            }

            ITEM_TIMETABLE -> {
                val scheduledStop =
                    ScheduledStop(page.tripSegment.to)
                scheduledStop.code = page.tripSegment.startStopCode
                scheduledStop.endStopCode = page.tripSegment.endStopCode
                scheduledStop.modeInfo = page.tripSegment.modeInfo


                scheduledStop.type = StopType.from(page.tripSegment.modeInfo?.localIconName.orEmpty())
                val timetableFragment = TimetableFragment.Builder()
                    .withStop(scheduledStop)
                    .withBookingAction(page.tripSegment.booking?.externalActions)
                    .withSegmentActionStream(segmentActionStream)
                    .withTripSegment(page.tripSegment)
                    .hideSearchBar()
                    .showCloseButton()
                    .isFromPreview(true)
                    .build()
                timetableFragment._tripSegment = (page.tripSegment)
                timetableFragment.onNextPage = {
                    onSwipePage.invoke(true)
                }
                timetableFragment.onPreviousPage = {
                    onSwipePage.invoke(false)
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

    override fun getCount(): Int {
        return pages.size
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

                // Add the timetable card as well
                temp.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE, segment))
                addedCards++
            }

            if (itemType == ITEM_NEARBY) {
                if (activeTripSegmentId == segment.segmentId && itemType == ITEM_NEARBY
                    && activeTripSegmentPosition <= 0
                ) {
                    activeTripSegmentPosition = index + addedCards
                }

                // Add the mode location card as well
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

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

}