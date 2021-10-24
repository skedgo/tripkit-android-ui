package com.skedgo.tripkit.ui.trippreview

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.StopType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.timetables.TimetableFragment
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.drt.DrtFragment
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripPreviewItemFragment
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.subjects.PublishSubject

data class TripPreviewPagerAdapterItem(val type: Int, var tripSegment: TripSegment)

class TripPreviewPagerAdapter(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    internal var onSwipePage: (Boolean /*true = Next, false = Previous*/) -> Unit = { _ -> }

    var pages = mutableListOf<TripPreviewPagerAdapterItem>()
    var onCloseButtonListener: View.OnClickListener? = null
    var tripPreviewPagerListener: TripPreviewPagerFragment.Listener? = null

    //To emit booking actions updates to TimetableFragment instead of getting and using the fragments instance
    var segmentActionStream = PublishSubject.create<TripSegment>()

    var bookingActions: List<String>? = null

    //var timetableFragment: TimetableFragment? = null

    /**
     * Adding callback for handling external actions (3rd party applications booking) handling.
     * To also unload the handling using TripPreviewPagerFragment.Listener since code doesn't
     * have to go through a lot of classes just to handle the action since components being
     * used for handling such as BookingV2TrackingService, bookingResolver, etc. (as per tracking the code
     * from previous implementation) can be injected and be used in TripPreviewPagerFragment.
     */
    internal var externalActionCallback: ((TripSegment?, Action?) -> Unit)? = null

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
                val fragment = ServiceTripPreviewItemFragment.newInstance(page.tripSegment, position, true) // PT with Stops
                fragment.onNextPage = {
                    onSwipePage.invoke(true)
                }
                fragment.onPreviousPage = {
                    onSwipePage.invoke(false)
                }
                fragment
            }
            ITEM_TIMETABLE -> {
                val scheduledStop = ScheduledStop(page.tripSegment.to)
                scheduledStop.code = page.tripSegment.startStopCode
                scheduledStop.modeInfo = page.tripSegment.modeInfo


                scheduledStop.type = StopType.from(page.tripSegment.modeInfo?.localIconName)
                val timetableFragment = TimetableFragment.Builder()
                        .withStop(scheduledStop)
                        .withBookingAction(page.tripSegment.booking?.externalActions)
                        .withSegmentActionStream(segmentActionStream)
                        .withTripSegment(page.tripSegment)
                        .hideSearchBar()
                        .showCloseButton()
                        .isFromPreview(true)
                        .build()
                timetableFragment.setTripSegment(page.tripSegment)
                timetableFragment.onNextPage = {
                    onSwipePage.invoke(true)
                }
                timetableFragment.onPreviousPage = {
                    onSwipePage.invoke(false)
                }
                timetableFragment
            }
            ITEM_QUICK_BOOKING -> {
                DrtFragment.newInstance(page.tripSegment) { segment ->
                    pages.firstOrNull { it.tripSegment.id == segment.id }?.tripSegment = segment
                }
            }
            else -> StandardTripPreviewItemFragment.newInstance(page.tripSegment)
        }
        fragment.onCloseButtonListener = onCloseButtonListener
        fragment.tripPreviewPagerListener = tripPreviewPagerListener
        return fragment
    }

    fun getSegmentByPosition(position: Int): TripSegment {
        return pages[position].tripSegment
    }

    fun getSegmentPositionById(pair: Pair<Long, String>): Int {
        return pages.indexOfFirst { it.tripSegment.id == pair.first }
    }

    override fun getCount(): Int {
        return pages.size
    }

    fun setTripSegments(
            activeTripSegmentId: Long,
            tripSegments: List<TripSegment>,
            fromAction: Boolean = false): Int {
        var activeTripSegmentPosition = 0
        var addedCards = 0
        val temp = ArrayList<TripPreviewPagerAdapterItem>()
        tripSegments.forEachIndexed { index, segment ->
            val itemType = segment.correctItemType()

            if (itemType == ITEM_SERVICE) {
                if (activeTripSegmentId == segment.id && itemType == ITEM_SERVICE
                        && activeTripSegmentPosition <= 0) {
                    activeTripSegmentPosition = index + addedCards
                }

                // Add the timetable card as well
                temp.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE, segment))
                addedCards++
            }

            if (itemType == ITEM_NEARBY) {
                if (activeTripSegmentId == segment.id && itemType == ITEM_NEARBY
                        && activeTripSegmentPosition <= 0) {
                    activeTripSegmentPosition = index + addedCards
                }

                // Add the mode location card as well
                temp.add(TripPreviewPagerAdapterItem(ITEM_MODE_LOCATION, segment))
                addedCards++
            } else {
                temp.add(TripPreviewPagerAdapterItem(itemType, segment))
            }

            if (activeTripSegmentId == segment.id && activeTripSegmentPosition <= 0) {
                activeTripSegmentPosition = index + addedCards
            }
        }

//        val excess = pages.size - temp.size
//        if (excess > 0) {
//            val startExcess = pages.size - excess - 1
//
//            for (i in startExcess until pages.size - 1) {
//                pages.removeAt(i)
//            }
//        }
        pages = temp.toMutableList()

//        temp.forEachIndexed { index, it ->
//            try {
//                pages.remove(it)
//            } catch (e: Exception) {}
//            pages.add(index, it)
//        }

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