package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.TripKitUI.Companion.getInstance
import com.skedgo.tripkit.ui.booking.BookViewClickEventHandler.Companion.create
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.databinding.TripResultPagerBinding
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.model.TripKitButtonConfigurator
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripKitButtonClickListener
import com.skedgo.tripkit.ui.tripresult.TripSegmentListFragment.OnTripSegmentClickListener
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.squareup.otto.Bus
import javax.inject.Inject

class TripResultPagerFragment : BaseTripKitFragment(), OnPageChangeListener,
    OnTripKitButtonClickListener {
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
    private var binding: TripResultPagerBinding? = null
    private val mapContributor = TripResultMapContributor()
    private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null
    private var initialTripGroupList: List<TripGroup>? = null
    private var queryFromLocation: Location? = null
    private var queryToLocation: Location? = null
    private var args: PagerFragmentArguments? = null
    private var currentPage = -1
    private var tripAlertChangeValidator: (() -> Boolean)? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = TripResultPagerBinding.inflate(inflater)
        this.binding = binding

        binding.viewModel = viewModel
        binding.tripGroupsPager.adapter = tripGroupsPagerAdapter

        // Only set the page if we have a valid currentPage (not -1) or if this is the first time
        // This prevents subsequent fragment instances from overwriting the correct page during restoration
        if (currentPage != -1) {
            // Only set the page if we have a valid currentPage and trip groups are available
            val currentTripGroups = tripGroupsPagerAdapter?.tripGroups
            if (currentTripGroups != null && currentPage >= 0 && currentPage < currentTripGroups.size) {
                binding.tripGroupsPager.currentItem = currentPage
                viewModel.currentPage.set(currentPage)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up the currentTrip observer once in onViewCreated to prevent duplicate calls during restoration
        viewModel.currentTrip.observe(viewLifecycleOwner, Observer { trip: Trip? ->
            if (tripUpdatedListener != null) {
                tripUpdatedListener!!.onTripUpdated(trip)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        bus!!.register(this)
        bus!!.register(bookViewClickEventHandler)
        autoDisposable.add(
            viewModel.trackViewingTrip()
                .subscribe()
        )

        autoDisposable.add(
            viewModel.observeTripGroups()
                .subscribe { groups: List<TripGroup>? -> tripGroupsPagerAdapter!!.notifyDataSetChanged() })

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

        checkNotNull(args)
        autoDisposable.add(
            viewModel.getSortedTripGroups(args!!, initialTripGroupList!!)
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

    }

    fun contributor(): TripKitMapContributor {
        return mapContributor
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
        binding!!.tripGroupsPager.addOnPageChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
        binding!!.tripGroupsPager.removeOnPageChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        bus.unregister(this)
        bus.unregister(bookViewClickEventHandler)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (binding != null && binding!!.tripGroupsPager != null) {
            outState.putInt(KEY_CURRENT_PAGE, binding!!.tripGroupsPager.currentItem)
        }
        viewModel.onSavedInstanceState(outState)
    }

    val currentFragment: Fragment
        get() = tripGroupsPagerAdapter!!.instantiateItem(
            binding!!.tripGroupsPager,
            if (currentPage == -1) binding!!.tripGroupsPager.currentItem else currentPage
        ) as Fragment

    override fun onAttach(context: Context) {
        getInstance().tripDetailsComponent().inject(this)
        mapContributor.initialize()
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE)
        }
        viewModel.onCreate(savedInstanceState)
        var tripId: Long? = null
        var groupId: String? = null
        tripGroupsPagerAdapter = TripGroupsPagerAdapter(childFragmentManager, mapContributor)

        if (savedInstanceState == null) {
            if (args is HasInitialTripGroupId) {
                groupId = (args as HasInitialTripGroupId).tripGroupId()
                tripId = (args as HasInitialTripGroupId).tripId()
                tripGroupsPagerAdapter!!.tripIds[groupId] = tripId!!
                viewModel.setInitialSelectedTripGroupId(groupId)
                mapContributor.setTripGroupId(groupId, tripId)
            }
        }

        val configurator: TripKitButtonConfigurator? = null
        val b = arguments
        if (b != null) {
            tripGroupsPagerAdapter!!.setShowCloseButton(b.getBoolean(KEY_SHOW_CLOSE_BUTTON, false))
        }

        tripGroupsPagerAdapter!!.listener = this
        tripGroupsPagerAdapter!!.segmentClickListener = tripSegmentClickListener
        tripGroupsPagerAdapter!!.closeListener = onCloseButtonListener
        tripGroupsPagerAdapter!!.setActionButtonHandlerFactory(actionButtonHandlerFactory)
        tripGroupsPagerAdapter!!.setQueryLocations(queryFromLocation, queryToLocation)
        tripGroupsPagerAdapter!!.tripAlertChangeValidator = tripAlertChangeValidator
    }

    fun setArgs(args: PagerFragmentArguments) {
        this.args = args
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        val group = tripGroupsPagerAdapter!!.tripGroups!![position]
        mapContributor.setTripGroupId(group.uuid(), null)
        viewModel.currentPage.set(position)
        
        // Update current trip to trigger OnTripUpdatedListener when switching fragments
        viewModel.updateCurrentTripForPage(position)
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
            val b = Bundle()
            b.putBoolean(KEY_SHOW_CLOSE_BUTTON, showCloseButton)
            fragment.initialTripGroupList = initialTripGroupList
            fragment.arguments = b
            return fragment
        }
    }

    companion object {
        private const val KEY_CURRENT_PAGE = "currentPage"
        private const val KEY_SHOW_CLOSE_BUTTON = "showCloseButton"
    }
}
