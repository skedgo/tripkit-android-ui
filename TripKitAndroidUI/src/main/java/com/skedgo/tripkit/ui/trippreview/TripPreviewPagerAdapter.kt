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

    var timetableFragment: TimetableFragment? = null

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        val fragment = when (page.type) {
            ITEM_DIRECTIONS -> {
                DirectionsTripPreviewItemFragment(page.tripSegment) // Directions showing miles per item
            }
            ITEM_NEARBY -> {
                NearbyTripPreviewItemFragment(page.tripSegment) // Happening nearby (collect bicycle, walk, etc)
            }
            ITEM_MODE_LOCATION -> {
                ModeLocationTripPreviewItemFragment(page.tripSegment) // modes
            }
            ITEM_EXTERNAL_BOOKING -> {
                ExternalActionTripPreviewItemFragment(page.tripSegment) // taxis, gocatch
            }
            ITEM_SERVICE -> {
                ServiceTripPreviewItemFragment(page.tripSegment) // PT with Stops
            }
            ITEM_TIMETABLE -> {
                val scheduledStop = ScheduledStop(page.tripSegment.to)
                scheduledStop.code = page.tripSegment.startStopCode
                scheduledStop.modeInfo = page.tripSegment.modeInfo
                scheduledStop.type = StopType.from(page.tripSegment.modeInfo?.localIconName)
                timetableFragment = TimetableFragment.Builder()
                        .withStop(scheduledStop)
                        .withBookingAction(page.tripSegment.booking?.externalActions)
                        .hideSearchBar()
                        .build()
                timetableFragment
            }
            else -> StandardTripPreviewItemFragment(page.tripSegment)
        }
        fragment!!.onCloseButtonListener = onCloseButtonListener
        fragment.tripPreviewPagerListener = tripPreviewPagerListener
        return fragment
    }

    override fun getCount(): Int {
        return pages.size
    }

    fun setTripSegments(activeTripSegmentId: Long, tripSegments: List<TripSegment>): Int {
        pages.clear()
        var activeTripSegmentPosition = 0
        var addedCards = 0
        tripSegments.forEachIndexed { index, segment ->
            val itemType = segment.correctItemType()

            if (itemType == ITEM_SERVICE) {
                if (activeTripSegmentId == segment.id && itemType == ITEM_SERVICE
                        && activeTripSegmentPosition <= 0) {
                    activeTripSegmentPosition = index + addedCards
                }

                // Add the timetable card as well
                pages.add(TripPreviewPagerAdapterItem(ITEM_TIMETABLE, segment))
                addedCards++
            }

            segment.action?.contains("neuron", true).let {
                if (!it!!) {
                    val newItem = TripPreviewPagerAdapterItem(itemType, segment)
                    pages.add(newItem)
                }
            }

            if (itemType == ITEM_NEARBY) {
                if (activeTripSegmentId == segment.id && itemType == ITEM_NEARBY
                        && activeTripSegmentPosition <= 0) {
                    activeTripSegmentPosition = index + addedCards
                }

                // Add the mode location card as well
                pages.add(TripPreviewPagerAdapterItem(ITEM_MODE_LOCATION, segment))
                addedCards++
            }

            if (activeTripSegmentId == segment.id && activeTripSegmentPosition <= 0) {
                activeTripSegmentPosition = index + addedCards
            }
        }
        notifyDataSetChanged()

        if (activeTripSegmentPosition < 0) {
            activeTripSegmentPosition = 0
        }
        return activeTripSegmentPosition
    }
}