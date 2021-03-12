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
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripPreviewItemFragment
import com.skedgo.tripkit.ui.utils.*

data class TripPreviewPagerAdapterItem(val type: Int, val tripSegment: TripSegment)

class TripPreviewPagerAdapter(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var pages = mutableListOf<TripPreviewPagerAdapterItem>()
    var onCloseButtonListener: View.OnClickListener? = null
    var tripPreviewPagerListener: TripPreviewPagerFragment.Listener? = null

    var directionsTripPreviewItemFragment: DirectionsTripPreviewItemFragment? = null
    var nearbyTripPreviewItemFragment: NearbyTripPreviewItemFragment? = null
    var modeLocationTripPreviewItemFragment: ModeLocationTripPreviewItemFragment? = null
    var externalActionTripPreviewItemFragment: ExternalActionTripPreviewItemFragment? = null
    var serviceTripPreviewItemFragment: ServiceTripPreviewItemFragment? = null
    var timetableFragment: TimetableFragment? = null

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DIRECTIONS -> {
                if (directionsTripPreviewItemFragment == null) {
                    directionsTripPreviewItemFragment = DirectionsTripPreviewItemFragment(page.tripSegment) // Directions showing miles per item
                } else {
                    directionsTripPreviewItemFragment!!.segment = page.tripSegment
                }
                directionsTripPreviewItemFragment!!
            }
            ITEM_NEARBY -> {
                if (nearbyTripPreviewItemFragment == null) {
                    nearbyTripPreviewItemFragment = NearbyTripPreviewItemFragment(page.tripSegment) // Happening nearby (collect bicycle, walk, etc)
                } else {
                    nearbyTripPreviewItemFragment!!.segment = page.tripSegment
                }
                nearbyTripPreviewItemFragment!!
            }
            ITEM_MODE_LOCATION -> {
                if (modeLocationTripPreviewItemFragment == null) {
                    modeLocationTripPreviewItemFragment = ModeLocationTripPreviewItemFragment(page.tripSegment) // modes
                } else {
                    modeLocationTripPreviewItemFragment!!.segment = page.tripSegment
                }
                modeLocationTripPreviewItemFragment!!
            }
            ITEM_EXTERNAL_BOOKING -> {
                if (externalActionTripPreviewItemFragment == null) {
                    externalActionTripPreviewItemFragment = ExternalActionTripPreviewItemFragment(page.tripSegment) // taxis, gocatch
                }
                externalActionTripPreviewItemFragment!!
            }
            ITEM_SERVICE -> {
                if (serviceTripPreviewItemFragment == null) {
                    serviceTripPreviewItemFragment = ServiceTripPreviewItemFragment(page.tripSegment) // PT with Stops
                } else {
                    serviceTripPreviewItemFragment!!.segment = page.tripSegment
                }
                serviceTripPreviewItemFragment!!
            }
            ITEM_TIMETABLE -> {
                val scheduledStop = ScheduledStop(page.tripSegment.to)
                scheduledStop.code = page.tripSegment.startStopCode
                scheduledStop.modeInfo = page.tripSegment.modeInfo
                scheduledStop.type = StopType.from(page.tripSegment.modeInfo?.localIconName)
                if (timetableFragment == null) {
                    timetableFragment = TimetableFragment.Builder()
                            .withStop(scheduledStop)
                            .withBookingAction(page.tripSegment.booking?.externalActions)
                            .hideSearchBar()
                            .build()
                } else {
                    timetableFragment!!.setBookingActions(page.tripSegment.booking?.externalActions)
                }
                timetableFragment!!
            }
            else -> StandardTripPreviewItemFragment(page.tripSegment)
        }
        fragment.onCloseButtonListener = onCloseButtonListener
        fragment.tripPreviewPagerListener = tripPreviewPagerListener
        return fragment
    }

    override fun getCount(): Int {
        return pages.size
    }

    fun setTripSegments(activeTripSegmentId: Long, tripSegments: List<TripSegment>): Int {
        pages.clear()
        var activeTripSegmentPosition = 0
        var addedModeCards = 0
        tripSegments.forEachIndexed { index, segment ->
            val itemType = segment.correctItemType()

            if (itemType == ITEM_SERVICE) {
                // Add the timetable card as well
                pages.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE, segment))
            }

            val newItem = TripPreviewPagerAdapterItem(itemType, segment)
            pages.add(newItem)

            if (itemType == ITEM_NEARBY) {
                // Add the mode location card as well
                pages.add(TripPreviewPagerAdapterItem(ITEM_MODE_LOCATION, segment))
                addedModeCards++
            }

            if (activeTripSegmentId == segment.id) {
                activeTripSegmentPosition = index + addedModeCards
            }
        }
        notifyDataSetChanged()
        return activeTripSegmentPosition
    }
}