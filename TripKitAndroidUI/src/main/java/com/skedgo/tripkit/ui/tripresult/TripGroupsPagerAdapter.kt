package com.skedgo.tripkit.ui.tripresult

import android.util.Log
import android.util.SparseLongArray
import android.view.View
import android.view.ViewGroup
import androidx.core.util.getOrDefault
import androidx.core.util.getOrElse
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripSegmentClickListener
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import timber.log.Timber

class TripGroupsPagerAdapter(private val fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_SET_USER_VISIBLE_HINT) {
    var tripGroups: List<TripGroup>? = null
    set(value) {
        field = value
        notifyDataSetChanged()
    }
    var tripIds = mutableMapOf<String, Long>()

    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
    private var showCloseButton = false
    @JvmField
    var closeListener: View.OnClickListener? = null
    @JvmField
    var listener: TripSegmentListFragment.OnTripKitButtonClickListener? = null
    @JvmField
    var segmentClickListener: OnTripSegmentClickListener? = null

    /*
        This horrific workaround is necessary because the Material library's BottomSheetBehavior only looks for the
        first child with nestedScrollingEnabled. Meaning that by putting a ViewPager in the card, only the first fragment will be scrollable,
        and it's very hit-or-miss. In order to workaround that limitation, the RecyclerView containing the segment list
        (the only scrollable view) should have a specific tag which the adapter looks for and sets as being nestedScrollingEnabled.
        It then iterates through all of the other instantiated fragments, finds the other TripSegmentListFragments,
        and sets their segment lists to not be nested scrolling.
     */
    override fun setPrimaryItem(container: ViewGroup, position: Int, o: Any) {
        super.setPrimaryItem(container, position, o)
        if (o is TripSegmentListFragment) {
            val recyclerView = o.view?.findViewWithTag<RecyclerView>("segmentList")
            recyclerView?.isNestedScrollingEnabled = true

            fragmentManager.fragments.forEach { f ->
                if (f is TripSegmentListFragment && f.hashCode() != o.hashCode()) {
                    val otherView = f.view?.findViewWithTag<View>("segmentList")
                    otherView?.isNestedScrollingEnabled = false
                }
                container.requestLayout();
            }
        }
    }

    fun setShowCloseButton(showCloseButton: Boolean) {
        this.showCloseButton = showCloseButton
    }

    override fun getItem(position: Int): Fragment {
        val tripGroup = tripGroups!![position]
        val tripId = tripIds[tripGroup.uuid()]
        val fragment = TripSegmentListFragment.Builder()
                .withTripGroupId(tripGroup.uuid())
                .withTripId(tripId)
                .withActionButtonHandlerFactory(actionButtonHandlerFactory)
                .showCloseButton(showCloseButton)
                .build()
        fragment.setOnTripKitButtonClickListener(listener!!)
        fragment.onCloseButtonListener = closeListener
        fragment.setOnTripSegmentClickListener(segmentClickListener!!)
        return fragment
    }

    override fun getCount(): Int {
        return if (tripGroups == null) 0 else tripGroups!!.size
    }

    fun setActionButtonHandlerFactory(actionButtonHandlerFactory: ActionButtonHandlerFactory?) {
        this.actionButtonHandlerFactory = actionButtonHandlerFactory
    }
}