package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager2.widget.ViewPager2
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI.Companion.getInstance
import com.skedgo.tripkit.ui.booking.BookViewClickEventHandler.Companion.create
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.TripResultPagerBinding
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.model.TripKitButtonConfigurator
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripKitButtonClickListener
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripSegmentClickListener
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.squareup.otto.Bus
import timber.log.Timber
import javax.inject.Inject
import com.skedgo.tripkit.ui.tripresult.v2.TripGroupsPagerAdapter

class TripResultPagerFragment : BaseFragment<TripResultPagerBinding>(), OnPageChangeListener,
    OnTripKitButtonClickListener {

    override val layoutRes: Int
        get() = R.layout.trip_result_pager

    private val bookViewClickEventHandler = create(this)
    var tripSegmentClickListener: OnTripSegmentClickListener? = null
    var tripButtonClickListener: OnTripKitButtonClickListener? = null
    var tripUpdatedListener: OnTripUpdatedListener? = null

    /* TODO: Replace with RxJava-based approach. */
    @Inject
    @Deprecated("")
    lateinit var bus: Bus

    @Inject
    lateinit var viewModel: TripResultPagerViewModel

    @Inject
    lateinit var errorLogger: ErrorLogger


    private var tripGroupsPagerAdapter: TripGroupsPagerAdapter? = null
    private val mapContributor = TripResultMapContributor()
    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
    private var initialTripGroupList: List<TripGroup>? = null
    private var queryFromLocation: Location? = null
    private var queryToLocation: Location? = null
    private var args: PagerFragmentArguments? = null
    private var currentPage = -1
    private var tripAlertChangeValidator: (() -> Boolean)? = null
    
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            this@TripResultPagerFragment.onPageSelected(position)
        }
    }

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    fun setOnTripKitButtonClickListener(listener: OnTripKitButtonClickListener?) {
        this.tripButtonClickListener = listener
    }

    fun setOnTripUpdatedListener(listener: OnTripUpdatedListener) {
        this.tripUpdatedListener = listener
    }

    fun setActionButtonHandlerFactory(actionButtonHandlerFactory: ActionButtonHandlerFactory?) {
        this.actionButtonHandlerFactory = actionButtonHandlerFactory
    }

    fun setTripAlertChangeValidator(validator: () -> Boolean) {
        this.tripAlertChangeValidator = validator
    }

    fun setQueryLocations(from: Location?, to: Location?) {
        queryFromLocation = from
        queryToLocation = to
    }

    override fun onCreated(savedInstance: Bundle?) {

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        if (savedInstance != null) {
            currentPage = savedInstance.getInt(KEY_CURRENT_PAGE)
        }

        viewModel.onCreate(savedInstance)
        var tripId: Long? = null
        var groupId: String? = null
        tripGroupsPagerAdapter = TripGroupsPagerAdapter(this, mapContributor)
        tripGroupsPagerAdapter?.tripGroups = emptyList()

        if (savedInstance == null) {
            if (args is HasInitialTripGroupId) {
                groupId = (args as HasInitialTripGroupId).tripGroupId()
                tripId = (args as HasInitialTripGroupId).tripId()
                tripGroupsPagerAdapter?.tripIds?.set(groupId, tripId!!)
                viewModel.setInitialSelectedTripGroupId(groupId)
                mapContributor.setTripGroupId(groupId, tripId)
            }
        }

        val args = arguments
        if (args != null) {
            tripGroupsPagerAdapter?.setShowCloseButton(args.getBoolean(KEY_SHOW_CLOSE_BUTTON, false))
        }

        tripGroupsPagerAdapter?.apply {
            listener = this@TripResultPagerFragment
            segmentClickListener = tripSegmentClickListener
            closeListener = onCloseButtonListener
            setActionButtonHandlerFactory(actionButtonHandlerFactory)
            setQueryLocations(queryFromLocation, queryToLocation)
            tripAlertChangeValidator = this@TripResultPagerFragment.tripAlertChangeValidator
        }

        binding.tripGroupsPager.adapter = tripGroupsPagerAdapter
        binding.tripGroupsPager.offscreenPageLimit = 1

        binding.tripGroupsPager.setCurrentItem(currentPage, false)

        // Note: Callback registration moved to onStart() to properly handle onStart/onStop lifecycle
        // (callback gets unregistered in onStop, re-registered in onStart)

        binding.tripGroupsPager.currentItem = currentPage
        viewModel.currentPage.set(currentPage)
    }

    override fun onResume() {
        super.onResume()
        bus.register(this)
        bus.register(bookViewClickEventHandler)

        autoDisposable.add(
            viewModel.trackViewingTrip()
                .subscribe()
        )

        autoDisposable.add(
            viewModel.observeTripGroups()
                .subscribe { groups: List<TripGroup> ->
                    tripGroupsPagerAdapter?.tripGroups = groups
                    tripGroupsPagerAdapter?.notifyDataSetChanged()
                }
        )

        autoDisposable.add(
            viewModel.observeInitialPage()
                .subscribe()
        )

        autoDisposable.add(
            viewModel.updateSelectedTripGroup()
                .subscribe()
        )

        autoDisposable.add(
            viewModel.loadFetchingRealtimeStatus()
                .subscribe()
        )

        // Call getSortedTripGroups if we have args (initialTripGroupList can be null/empty)
        // This is critical for loading trip data - without it, fragment shows loading spinner forever
        if (args != null) {
            Timber.d("$LOG_TAG - Calling getSortedTripGroups with args=${args?.javaClass?.simpleName}, initialTripGroupList size=${initialTripGroupList?.size ?: 0}")
            autoDisposable.add(
                viewModel.getSortedTripGroups(args!!, initialTripGroupList ?: emptyList())
                    .subscribe({ tripGroup: Unit ->
                        if (args is FavoriteTrip) {
                            // The trip group will possibly have changed after reloading it, so set the map to the correct one here
                            mapContributor.setTripGroupId(viewModel.currentTripGroupId.get(), null)
                        }
                    }, { error: Throwable ->
                        errorLogger.trackError(
                            error
                        )
                    })
            )
        } else {
            Timber.w("$LOG_TAG - Cannot call getSortedTripGroups: args is null")
        }

        viewModel.currentTrip.observe(viewLifecycleOwner) { trip: Trip? ->
            if (tripUpdatedListener != null) {
                tripUpdatedListener!!.onTripUpdated(trip)
            }
        }

        // Note: tripGroupsBinding is now ObservableField, updated via observeTripGroups() in onResume
    }

    fun contributor(): TripKitMapContributor {
        return mapContributor
    }
    
    fun getCurrentPage(): Int {
        return currentPage
    }

    fun updatePagerFragmentTripGroup(tripGroup: TripGroup) {
        viewModel.setInitialSelectedTripGroupId(tripGroup.uuid())
    }

    fun updateTripGroupResult(tripGroup: List<TripGroup>) {
        viewModel.updateTripGroupResult(tripGroup)
    }

    override fun onDestroy() {
        mapContributor.cleanup()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
        mapContributor.setup()
        //binding.tripGroupsPager.addOnPageChangeListener(this)
        if(binding.tripGroupsPager.adapter == null) {
            binding.tripGroupsPager.adapter = tripGroupsPagerAdapter
            // Restore the current page position after setting adapter
            // (adapter removal in onStop resets the ViewPager position)
            if (currentPage >= 0) {
                Timber.d("$LOG_TAG - onStart: Restoring ViewPager position to $currentPage")
                binding.tripGroupsPager.setCurrentItem(currentPage, false)
            }
        }
        // Re-register page change callback (it gets unregistered in onStop)
        binding.tripGroupsPager.registerOnPageChangeCallback(pageChangeCallback)
        Timber.d("$LOG_TAG - onStart: Page change callback re-registered")
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
        
        // Save current page position before stopping (e.g., when navigating to TripPreview)
        // Notify parent fragment so it can restore this position when recreating the pager
        val currentPagePosition = currentPage
        if (currentPagePosition >= 0) {
            pagePositionListener?.onPagePositionSaved(currentPagePosition)
            Timber.d("$LOG_TAG - onStop: Notified parent of current page position: $currentPagePosition")
        }
        
        Timber.d("$LOG_TAG - onStop: Unregistering page change callback")
        binding.tripGroupsPager.unregisterOnPageChangeCallback(pageChangeCallback)
        //binding.tripGroupsPager.removeOnPageChangeListener(this)
        binding.tripGroupsPager.adapter = null
    }

    override fun onPause() {
        super.onPause()
        bus.unregister(this)
        bus.unregister(bookViewClickEventHandler)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (binding != null && binding.tripGroupsPager != null) {
            outState.putInt(KEY_CURRENT_PAGE, binding.tripGroupsPager.currentItem)
        }
        viewModel.onSavedInstanceState(outState)
    }

    override fun onAttach(context: Context) {
        getInstance().tripDetailsComponent().inject(this)
        mapContributor.initialize()
        super.onAttach(context)
    }

    fun setArgs(args: PagerFragmentArguments) {
        this.args = args
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        val group = tripGroupsPagerAdapter?.tripGroups?.get(position)
        Timber.d("$LOG_TAG - Page swiped to position $position, tripGroupId=${group?.uuid()}")
        // Update the field so onStop can save it
        currentPage = position
        mapContributor.setTripGroupId(group?.uuid(), null)
        viewModel.currentPage.set(position)
        // Update current trip when page changes
        viewModel.updateCurrentTripForPage(position)
        Timber.d("$LOG_TAG - Map contributor updated for new trip, currentPage field updated to $currentPage")
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun tripKitButtonClicked(id: Int, tripGroup: TripGroup) {
        if (tripButtonClickListener != null) {
            tripButtonClickListener!!.onTripKitButtonClicked(id, tripGroup)
        }
    }

    interface OnTripKitButtonClickListener {
        fun onTripKitButtonClicked(id: Int, tripGroup: TripGroup?)
    }

    interface OnTripUpdatedListener {
        fun onTripUpdated(trip: Trip?)
    }
    
    interface OnPagePositionListener {
        fun onPagePositionSaved(position: Int)
    }
    
    private var pagePositionListener: OnPagePositionListener? = null
    
    fun setOnPagePositionListener(listener: OnPagePositionListener?) {
        this.pagePositionListener = listener
    }

    class Builder {
        private var tripGroupId = ""
        private var favoriteTripId = ""
        private var tripId = -1L
        private var sortOrder = 1
        private var requestId = ""
        private var arriveBy = 0L
        private var fromLocation: Location? = null
        private var toLocation: Location? = null
        private var showCloseButton = false
        private var singleRoute = false
        private var initialTripGroupList: List<TripGroup>? = null
        private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
        private var currentPage: Int = -1  // Initial page position

        fun withActionButtonHandlerFactory(factory: ActionButtonHandlerFactory?): Builder {
            this.actionButtonHandlerFactory = factory
            return this
        }

        fun withViewTrip(trip: ViewTrip): Builder {
            this.tripGroupId = trip.tripGroupUUID()
            this.tripId = trip.displayTripID
            this.sortOrder = trip.sortOrder
            this.arriveBy = trip.query().arriveBy
            this.requestId = trip.query().uuid()
            this.fromLocation = trip.query().fromLocation
            this.toLocation = trip.query().toLocation
            return this
        }

        fun withFavoriteTripId(id: String): Builder {
            favoriteTripId = id
            singleRoute = true
            return this
        }

        fun withTripGroupId(tripGroupId: String): Builder {
            this.tripGroupId = tripGroupId
            return this
        }

        fun withTripId(tripId: Long): Builder {
            this.tripId = tripId
            return this
        }

        fun showSingleRoute(): Builder {
            this.singleRoute = true
            return this
        }

        fun withSortOrder(sortOrder: Int): Builder {
            this.sortOrder = sortOrder
            return this
        }

        fun withRequestId(requestId: String): Builder {
            this.requestId = requestId
            return this
        }

        fun withArriveBy(arriveBy: Long): Builder {
            this.arriveBy = arriveBy
            return this
        }

        fun withInitialTripGroupList(initialTripGroupList: List<TripGroup>?): Builder {
            this.initialTripGroupList = initialTripGroupList
            return this
        }

        fun showCloseButton(): Builder {
            this.showCloseButton = true
            return this
        }
        
        fun withCurrentPage(page: Int): Builder {
            this.currentPage = page
            return this
        }

        fun build(): TripResultPagerFragment {
            val args = if (singleRoute) {
                if (!favoriteTripId.isEmpty()) {
                    FavoriteTrip(this.favoriteTripId)
                } else {
                    SingleTrip(this.tripGroupId, this.tripId)
                }
            } else {
                FromRoutes(
                    this.tripGroupId,
                    this.tripId,
                    this.sortOrder,
                    this.requestId,
                    this.arriveBy
                )
            }
            val fragment = TripResultPagerFragment()
            fragment.setArgs(args)
            fragment.setActionButtonHandlerFactory(actionButtonHandlerFactory)
            fragment.setQueryLocations(fromLocation, toLocation)
            // Set current page before arguments if specified
            if (currentPage >= 0) {
                fragment.currentPage = currentPage
            }
            val b = Bundle()
            b.putBoolean(KEY_SHOW_CLOSE_BUTTON, showCloseButton)
            fragment.initialTripGroupList = initialTripGroupList
            fragment.arguments = b
            return fragment
        }
    }

    companion object {
        // Logging tag for state restoration debugging
        private const val LOG_TAG = "[StateRestore] TripResultPagerFragment"
        
        private const val KEY_CURRENT_PAGE = "currentPage"
        private const val KEY_SHOW_CLOSE_BUTTON = "showCloseButton"
    }
}
