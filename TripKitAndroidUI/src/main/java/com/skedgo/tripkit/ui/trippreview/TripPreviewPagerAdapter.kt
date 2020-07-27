package com.skedgo.tripkit.ui.trippreview

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.skedgo.tripkit.routing.TripSegment
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

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DIRECTIONS -> DirectionsTripPreviewItemFragment(page.tripSegment)
            ITEM_NEARBY -> NearbyTripPreviewItemFragment(page.tripSegment)
            ITEM_MODE_LOCATION -> ModeLocationTripPreviewItemFragment(page.tripSegment)
            ITEM_EXTERNAL_BOOKING -> ExternalActionTripPreviewItemFragment(page.tripSegment)
            ITEM_SERVICE -> ServiceTripPreviewItemFragment(page.tripSegment)
            else -> StandardTripPreviewItemFragment(page.tripSegment)
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
            val itemType = segment.correctItemType()
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

}