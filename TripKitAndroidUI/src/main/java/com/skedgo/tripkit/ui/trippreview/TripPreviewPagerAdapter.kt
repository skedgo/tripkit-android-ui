package com.skedgo.tripkit.ui.trippreview

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.trippreview.default.DefaultTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment

const val ITEM_DEFAULT = 0
const val ITEM_DIRECTIONS = 1
const val ITEM_NEARBY = 2
const val ITEM_MODE_LOCATION = 3
const val ITEM_TIMETABLE = 4
const val ITEM_QUICK_BOOKING = 5
const val ITEM_EXTERNAL_BOOKING = 6
data class TripPreviewPagerAdapterItem(val type: Int, val tripSegment: TripSegment)

class TripPreviewPagerAdapter(fragmentManager: FragmentManager)
    : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var pages = mutableListOf<TripPreviewPagerAdapterItem>()
    var onCloseButtonListener: View.OnClickListener? = null

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DEFAULT -> DefaultTripPreviewItemFragment(page.tripSegment)
            ITEM_DIRECTIONS -> DirectionsTripPreviewItemFragment(page.tripSegment)
            ITEM_NEARBY -> NearbyTripPreviewItemFragment(page.tripSegment)
            ITEM_MODE_LOCATION -> ModeLocationTripPreviewItemFragment(page.tripSegment)
            ITEM_EXTERNAL_BOOKING -> ExternalActionTripPreviewItemFragment(page.tripSegment)
            else -> DefaultTripPreviewItemFragment(page.tripSegment)
        }
        fragment.onCloseButtonListener = onCloseButtonListener
        return fragment
    }

    override fun getCount(): Int {
        return pages.size
    }

    fun setTripSegments(activeTripSegmentId: Long, tripSegments: List<TripSegment>): Int {
        var activeTripSegmentPosition = 0
        tripSegments.forEachIndexed {index, segment ->

            val itemType = getCorrectItemType(segment)
            val newItem = TripPreviewPagerAdapterItem(itemType, segment)
            pages.add(newItem)

            if (itemType == ITEM_NEARBY) {
                // Add the mode location card as well
                pages.add(TripPreviewPagerAdapterItem(ITEM_MODE_LOCATION, segment))
            }

            if (activeTripSegmentId == segment.templateHashCode) {
                activeTripSegmentPosition = index
            }
        }
        notifyDataSetChanged()
        return activeTripSegmentPosition
    }

    private fun getCorrectItemType(segment: TripSegment): Int {
        return if (segment.turnByTurn != null) {
            ITEM_DIRECTIONS
        } else if (segment.mode?.isPublicTransport == true) {
            ITEM_TIMETABLE
        } else if (segment.modeInfo?.id == "stationary_vehicle-collect" || segment.hasCarParks()) {
            ITEM_NEARBY
        }  else if (segment.booking?.quickBookingsUrl != null || segment.booking?.confirmation != null) {
            ITEM_QUICK_BOOKING
        } else if (segment.booking?.externalActions != null && segment.booking.externalActions!!.count() > 0) {
            ITEM_EXTERNAL_BOOKING
        } else {
            ITEM_DEFAULT
        }
    }
}