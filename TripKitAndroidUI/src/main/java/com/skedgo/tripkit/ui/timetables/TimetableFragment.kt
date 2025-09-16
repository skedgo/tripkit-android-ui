package com.skedgo.tripkit.ui.timetables

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitPagerFragment
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TimetableFragmentBinding
import com.skedgo.tripkit.ui.dialog.TimeDatePickerFragment
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.model.TripKitButton
import com.skedgo.tripkit.ui.search.ARG_SHOW_SEARCH_FIELD
import com.skedgo.tripkit.ui.utils.OnSwipeTouchListener
import com.skedgo.tripkit.ui.utils.observe
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TimetableFragment : BaseTripKitPagerFragment(), View.OnClickListener {

    /**
     * This callback will be invoked when a specific timetable entry is clicked.
     */
    interface OnTimetableEntrySelectedListener {
        fun onTimetableEntrySelected(
            segment: TripSegment?,
            service: TimetableEntry,
            stop: ScheduledStop,
            minStartTime: Long
        )
    }

    private var timetableEntrySelectedListener: MutableList<OnTimetableEntrySelectedListener> =
        mutableListOf()

    fun addOnTimetableEntrySelectedListener(callback: OnTimetableEntrySelectedListener) {
        if (!timetableEntrySelectedListener.contains(callback)) {
            timetableEntrySelectedListener.add(callback)
        }
    }

    fun addOnTimetableEntrySelectedListener(listener: (TimetableEntry, ScheduledStop, Long) -> Unit) {
        this.timetableEntrySelectedListener.add(object : OnTimetableEntrySelectedListener {
            override fun onTimetableEntrySelected(
                segment: TripSegment?,
                service: TimetableEntry,
                stop: ScheduledStop,
                minStartTime: Long
            ) {
                listener(service, stop, minStartTime)
            }
        })
    }


    /**
    When you provide the Timetable fragment with buttons, this callback will be called when one is clicked.
     */
    interface OnTripKitButtonClickListener {
        fun onTripButtonClicked(id: Int, stop: ScheduledStop)
    }

    private var tripButtonClickListener: OnTripKitButtonClickListener? = null
    fun setOnTripKitButtonClickListener(listener: OnTripKitButtonClickListener) {
        this.tripButtonClickListener = listener
    }

    fun setOnTripKitButtonClickListener(listener: (Int, ScheduledStop) -> Unit) {
        this.tripButtonClickListener = object : OnTripKitButtonClickListener {
            override fun onTripButtonClicked(id: Int, stop: ScheduledStop) {
                listener(id, stop)
            }
        }
    }

    var segmentActionStream: PublishSubject<TripSegment>? = null

    private val viewModel: TimetableViewModel by viewModels() { viewModelFactory }

    @Inject
    lateinit var viewModelFactory: TimetableViewModelFactory

    var stop: ScheduledStop? = null
        set(value) {
            if (value != null) {
                this.viewModel.stop.accept(value);
            }
            field = value
        }

    var tripSegment: TripSegment? = null
        set(value) {
            if (value != null) {
                this.viewModel.serviceTripId.accept(value.serviceTripId)
            } else {
                this.viewModel.serviceTripId.accept("")
            }
            field = value
        }

    var _tripSegment: TripSegment? = null

    var cachedStop: ScheduledStop? = null
    var cachedShowSearchBar: Boolean = true
    var fromPreview: Boolean = false
    var cachedBookingActions: ArrayList<String>? = null

    var bookingActions: ArrayList<String>? = null
        set(value) {
            if (value != null) {
                this.viewModel.withBookingActions(value, tripSegment)
            }
            field = value
        }
    private val filterThrottle = PublishSubject.create<String>()
    private lateinit var binding: TimetableFragmentBinding
    protected var buttons: List<TripKitButton> = emptyList()
    var servicesAreLoaded = false

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this);
        super.onAttach(context)
    }

    /**
     * State restoration for TimetableFragment
     *
     * This handles the comprehensive saving of timetable state when the app is killed and restarted.
     * The saving process follows a specific order to ensure all critical data is preserved:
     *
     * 1. Save basic fragment data (stop, booking actions, UI state)
     * 2. Save trip segment data for route restoration
     * 3. Save view model state (filter, service trip ID)
     * 4. Save scroll position for user experience continuity
     * 5. Save cached data for offline/background scenarios
     * 6. Save button state for action restoration
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Step 1: Save basic fragment data
        saveBasicFragmentData(outState)

        // Step 2: Save trip segment data
        saveTripSegmentData(outState)

        // Step 3: Save view model state
        saveViewModelState(outState)

        // Step 4: Save scroll position
        saveScrollPosition(outState)

        // Step 5: Save cached data
        saveCachedData(outState)

        // Step 6: Save button state
        saveButtonState(outState)
    }

    /**
     * Save basic fragment data including stop, booking actions, and UI state.
     * This ensures the core timetable functionality can be restored.
     */
    private fun saveBasicFragmentData(outState: Bundle) {
        outState.putParcelable(ARG_STOP, stop)
        outState.putStringArrayList(ARG_BOOKING_ACTION, bookingActions)
        outState.putBoolean(ARG_SHOW_SEARCH_FIELD, viewModel.showSearch.value ?: false)
        outState.putBoolean(ARG_SHOW_CLOSE_BUTTON, viewModel.showCloseButton.value ?: false)
    }

    /**
     * Save trip segment data for route restoration.
     * This ensures the timetable can be properly linked to the correct trip segment.
     */
    private fun saveTripSegmentData(outState: Bundle) {
        tripSegment?.let { segment ->
            outState.putString(KEY_SAVED_TRIP_SEGMENT_SERVICE_TRIP_ID, segment.serviceTripId ?: "")
            outState.putString(KEY_SAVED_TRIP_SEGMENT_ID, segment.id)
            outState.putLong(KEY_SAVED_TRIP_SEGMENT_SEGMENT_ID, segment.segmentId)
        }
    }

    /**
     * Save view model state including filter and service trip ID.
     * This preserves the current search/filter state and service context.
     */
    private fun saveViewModelState(outState: Bundle) {
        outState.putString(KEY_SAVED_FILTER_TEXT, viewModel.filter.value ?: "")
        outState.putString(KEY_SAVED_SERVICE_TRIP_ID, viewModel.serviceTripId.value ?: "")
    }

    /**
     * Save scroll position for user experience continuity.
     * This ensures the user returns to the same position in the timetable list.
     */
    private fun saveScrollPosition(outState: Bundle) {
        if (::binding.isInitialized) {
            val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.let { manager ->
                outState.putInt(KEY_SAVED_SCROLL_POSITION, manager.findFirstVisibleItemPosition())
                outState.putInt(KEY_SAVED_SCROLL_OFFSET, manager.findViewByPosition(manager.findFirstVisibleItemPosition())?.top ?: 0)
            }
        }
    }

    /**
     * Save cached data for offline/background scenarios.
     * This ensures the timetable can function even when network data is unavailable.
     */
    private fun saveCachedData(outState: Bundle) {
        cachedStop?.let { cached ->
            outState.putParcelable(KEY_SAVED_CACHED_STOP, cached)
        }
        outState.putBoolean(KEY_SAVED_CACHED_SHOW_SEARCH_BAR, cachedShowSearchBar)
        outState.putBoolean(KEY_SAVED_FROM_PREVIEW, fromPreview)
        cachedBookingActions?.let { cached ->
            outState.putStringArrayList(KEY_SAVED_CACHED_BOOKING_ACTIONS, cached)
        }
    }

    /**
     * Save button state for action restoration.
     * This preserves any custom action buttons that were configured.
     */
    private fun saveButtonState(outState: Bundle) {
        outState.putInt(KEY_SAVED_BUTTONS_COUNT, buttons.size)
        buttons.forEachIndexed { index, button ->
            outState.putString("$KEY_SAVED_BUTTON_PREFIX$index", button.id)
        }
    }

    /**
     * State restoration for TimetableFragment
     *
     * This handles the restoration of timetable state when the app is killed and restarted.
     * The restoration process follows a specific order to ensure proper initialization:
     *
     * 1. Restore basic booking actions and cached data
     * 2. Apply restored state to view model
     * 3. Restore scroll position when view is ready
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { arguments = it }

        // Step 1: Restore basic booking actions and cached data
        restoreBasicData()
    }

    /**
     * Restore basic data including booking actions and cached information.
     * This ensures the fragment has the necessary context for proper initialization.
     */
    private fun restoreBasicData() {
        // Restore basic booking actions
        bookingActions = cachedBookingActions ?: arguments?.getStringArrayList(ARG_BOOKING_ACTION)

        // Restore cached data
        arguments?.let { bundle ->
            cachedStop = bundle.getParcelable(KEY_SAVED_CACHED_STOP)
            cachedShowSearchBar = bundle.getBoolean(KEY_SAVED_CACHED_SHOW_SEARCH_BAR, true)
            fromPreview = bundle.getBoolean(KEY_SAVED_FROM_PREVIEW, false)
            cachedBookingActions = bundle.getStringArrayList(KEY_SAVED_CACHED_BOOKING_ACTIONS)
        }
    }

    override fun onResume() {
        super.onResume()

        setObservers()

        // Restore state if available
        arguments?.let { bundle ->
            restoreState(bundle)
        }

        binding.recyclerView.scrollToPosition(0)

        if (stop == null) {
            stop = cachedStop
        }

        tripSegment = _tripSegment
    }

    // TODO code moved from [onResume], breakdown result handling into specific functions
    private fun setObservers() {
        filterThrottle
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling {
                viewModel.filter.accept(it)
            }.addTo(autoDisposable)

        viewModel.onError.observeOn(AndroidSchedulers.mainThread()).subscribeWithErrorHandling { error ->
            binding.multiStateView.let { msv ->
                if (activity is OnResultStateListener) {
                    msv.setViewForState(
                        (activity as OnResultStateListener).provideErrorView(error),
                        MultiStateView.ViewState.ERROR, true
                    )
                } else {
                    val errorView =
                        LayoutInflater.from(activity).inflate(R.layout.generic_error_view, null)
                    errorView?.findViewById<TextView>(R.id.errorMessageView)?.text = error
                    msv.setViewForState(errorView, MultiStateView.ViewState.ERROR, true)
                }
            }
        }.addTo(autoDisposable)

        viewModel.stateChange.observeOn(AndroidSchedulers.mainThread()).subscribeWithErrorHandling {
            binding.multiStateView.let { msv ->
                if (it == MultiStateView.ViewState.EMPTY) {
                    if (
                        activity is OnResultStateListener &&
                        (activity as OnResultStateListener).provideEmptyView() != null
                    ) {
                        msv.setViewForState(
                            (activity as OnResultStateListener).provideEmptyView()!!,
                            MultiStateView.ViewState.EMPTY,
                            true
                        )
                    } else {
                        val emptyView =
                            LayoutInflater.from(activity).inflate(R.layout.generic_empty_view, null)
                        msv.setViewForState(emptyView, MultiStateView.ViewState.EMPTY, true)
                    }
                }
            }
        }.addTo(autoDisposable)

        viewModel.scrollToNow
            .delay(
                500,
                TimeUnit.MILLISECONDS
            ) // 500 ms is a guess, wait for the data to be set to the adapter.
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { integer ->
                val smoothScroller = object : LinearSmoothScroller(context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }

                smoothScroller.targetPosition = integer.toInt()
//                binding.recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
                scrollToNowPosition()
            }.addTo(autoDisposable)

        val buffer = if (tripSegment == null) {
            TimeUtils.InMillis.MINUTE * 10
        } else {
            (60 * 20)
        }
        viewModel.downloadTimetable.accept(
            (tripSegment?.startTimeInSecs
                ?: System.currentTimeMillis()) - buffer
        )

        viewModel.actionChosen.observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling {
                if (viewModel.action == "book") {
                    viewModel.buttonText.value = "Booking..."
                    viewModel.enableButton.value = false
                }
                tripPreviewPagerListener?.onServiceActionButtonClicked(
                    tripSegment,
                    viewModel.action
                )
            }.addTo(autoDisposable)

        viewModel.timetableEntryChosen.observeOn(AndroidSchedulers.mainThread()).subscribeWithErrorHandling {
            if (viewModel.action.isNotEmpty() || fromPreview) {
                tripSegment?.let { segmentActionStream?.onNext(it) }
                if (BuildConfig.TRIPKIT_UI_VERSION < 2) {
                    // FIXME processing data, e.g. getting waypoints and updated data, showing dialog updating should be moved on this class' ViewModel
                    //  and only have the listener trigger the additional actions from the class using TimetableFragment for the result.
                    //  More importantly, viewModelScope instance should not be passed and used outside its ViewModel
                    tripPreviewPagerListener?.onTimetableEntryClicked(
                        tripSegment,
                        viewModel.viewModelScope,
                        it
                    )
                } else {
                    viewModel.onTimetableEntryClicked(it, tripSegment)
                }
            } else {
                Observable.combineLatest(
                    viewModel.stopRelay,
                    viewModel.startTimeRelay
                ) { one: ScheduledStop, two: Long -> one to two }
                    .take(1).subscribeWithErrorHandling { pair ->
                        timetableEntrySelectedListener.forEach { listener ->
                            listener.onTimetableEntrySelected(
                                tripSegment,
                                it,
                                pair.first,
                                pair.second
                            )
                        }
                    }.addTo(autoDisposable)
            }
        }.addTo(autoDisposable)

        observe(viewModel.showTimeTableEntry) {
            it?.let {
                tripPreviewPagerListener?.viewTimetableEntry(
                    it.tripGroup,
                    it.trip,
                    it.tripSegment
                )
            }
        }

        observe(viewModel.showUpdateLoader) {
            it?.let {
                tripPreviewPagerListener
                    ?.showUpdateLoader(it, getString(R.string.str_updating))
            }
        }

        // Observer for services list changes
        viewModel.servicesObservable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWithErrorHandling { servicesList ->
                if(!servicesAreLoaded) {
                    scrollToNowPosition()
                    servicesAreLoaded = true
                }
            }.addTo(autoDisposable)
    }

    fun setBookingActions(bookingActions: List<String>?) {
        viewModel.enableButton.value = true
        if (!bookingActions.isNullOrEmpty()) {
            val list = ArrayList<String>()
            list.addAll(bookingActions.toMutableList())
            arguments?.putStringArrayList(ARG_BOOKING_ACTION, list)
            try {
                this.bookingActions = list
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TimetableFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner

        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.serviceLineRecyclerView.layoutManager = layoutManager

        binding.viewModel = viewModel
        binding.serviceLineRecyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.isNestedScrollingEnabled = true

        val swipeListener = OnSwipeTouchListener(requireContext(),
            object : OnSwipeTouchListener.SwipeGestureListener {
                override fun onSwipeRight() {
                    onNextPage?.invoke()
                }

                override fun onSwipeLeft() {
                    onPreviousPage?.invoke()
                }
            })

        swipeListener.touchCallback = { v, event ->
            v?.parent?.requestDisallowInterceptTouchEvent(true)
            v?.onTouchEvent(event)
        }

        binding.recyclerView.setOnTouchListener(swipeListener)


        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val nowPosition = viewModel.getFirstNowPosition()

                (binding.recyclerView.layoutManager as LinearLayoutManager).let {
                    if (nowPosition in it.findFirstVisibleItemPosition()..it.findLastVisibleItemPosition()) {
                        binding.goToNowButton.visibility = View.GONE
                    } else {
                        binding.goToNowButton.visibility = View.VISIBLE
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && viewModel.showLoading.value != true) {
                    viewModel.downloadMoreTimetableAsync()
                }
            }
        })

        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )

        binding.departuresSearchSetTime.timeSet.setOnClickListener {
            selectTime()
        }

        val search = binding.departuresSearchSetTime.stationSearch
        search.isSelected = false
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                p0?.let {
                    filterThrottle.onNext(it.toString())
                }
            }
        })

        buttons.forEach {
            try {
                val button = layoutInflater.inflate(it.layoutResourceId, null, false)
                button.tag = it.id
                button.setOnClickListener(this)
                binding.buttonLayout.addView(button)
            } catch (e: InflateException) {
                Timber.e("Invalid button layout ${it.layoutResourceId}", e)
            }
        }

        return binding.root
    }

    fun replaceButton(id: String, newLayoutId: Int) {
        buttons.forEach { button ->
            if (button.id == id) {
                val currentView = binding.buttonLayout.findViewWithTag<View?>(id)
                currentView?.let {
                    val currentViewIndex = binding.buttonLayout.indexOfChild(currentView)
                    val newView = layoutInflater.inflate(newLayoutId, null, false)
                    newView.tag = button.id
                    newView.setOnClickListener(this)

                    binding.buttonLayout.removeView(currentView)
                    binding.buttonLayout.addView(newView, currentViewIndex)
                    button.layoutResourceId = newLayoutId
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0);
        }

        viewModel.setText(requireContext())
        arguments?.let {
            handleArguments(it)
        }
    }

    private fun handleArguments(it: Bundle) {
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize basic data
        stop = arguments?.getParcelable(ARG_STOP)
        if (stop == null) {
            stop = cachedStop
        }
        tripSegment = _tripSegment

        binding.goToNowButton.setOnClickListener {
            scrollToNowPosition()
        }

        bookingActions =
            cachedBookingActions ?: savedInstanceState?.getStringArrayList(ARG_BOOKING_ACTION)
                ?: arguments?.getStringArrayList(ARG_BOOKING_ACTION)

        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        viewModel.showCloseButton.value = showCloseButton

        val showSearchBar = arguments?.getBoolean(ARG_SHOW_SEARCH_FIELD) ?: cachedShowSearchBar
        viewModel.showSearch.value = showSearchBar

        binding.closeButton.setOnClickListener(onCloseButtonListener)

        segmentActionStream?.let {
            it.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    setBookingActions(it.booking?.externalActions)
                }, { it.printStackTrace() })
                .addTo(autoDisposable)
        }

        // Restore state if available
        savedInstanceState?.let { bundle ->
            restoreState(bundle)
        }
    }

    /**
     * Restore state from saved instance state.
     * This method applies the saved state to the view model and UI components.
     * The restoration process follows a specific order to ensure proper initialization:
     *
     * 1. Restore trip segment data for proper route linking
     * 2. Restore filter state for search continuity
     * 3. Restore scroll position for user experience continuity
     */
    private fun restoreState(bundle: Bundle) {
        // Step 1: Restore trip segment data
        restoreTripSegmentData(bundle)

        // Step 2: Restore filter state
        restoreFilterState(bundle)

        // Step 3: Restore scroll position
        restoreScrollPosition(bundle)
    }

    /**
     * Restore trip segment data for proper route linking.
     * This ensures the timetable is correctly associated with the trip segment.
     */
    private fun restoreTripSegmentData(bundle: Bundle) {
        bundle.getString(KEY_SAVED_TRIP_SEGMENT_SERVICE_TRIP_ID)?.let { serviceTripId ->
            if (serviceTripId.isNotEmpty()) {
                viewModel.serviceTripId.accept(serviceTripId)
            }
        }
    }

    /**
     * Restore filter state for search continuity.
     * This ensures the timetable displays the correct filtered results.
     */
    private fun restoreFilterState(bundle: Bundle) {
        bundle.getString(KEY_SAVED_FILTER_TEXT)?.let { filterText ->
            if (filterText.isNotEmpty()) {
                viewModel.filter.accept(filterText)
            }
        }
    }

    /**
     * Restore scroll position for user experience continuity.
     * This ensures the user returns to the same position in the timetable list.
     */
    private fun restoreScrollPosition(bundle: Bundle) {
        val savedScrollPosition = bundle.getInt(KEY_SAVED_SCROLL_POSITION, -1)
        val savedScrollOffset = bundle.getInt(KEY_SAVED_SCROLL_OFFSET, 0)
        
        if (savedScrollPosition >= 0 && ::binding.isInitialized) {
            lifecycleScope.launch {
                delay(500) // Wait for adapter to be ready
                withContext(Dispatchers.Main) {
                    try {
                        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
                        layoutManager?.let { manager ->
                            manager.scrollToPositionWithOffset(savedScrollPosition, savedScrollOffset)
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Timber.w("Failed to restore scroll position: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun scrollToNowPosition(loadDelay: Long = 0) {
        lifecycleScope.launch {
            delay(loadDelay)
            withContext(Dispatchers.Main) {
                val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
                val firstNowPosition = viewModel.getFirstNowPosition()
                val itemCount = binding.recyclerView.adapter?.itemCount ?: 0

                if (firstNowPosition in 0 until itemCount) {
                    // If it's not at the bottom, make sure it stays at the top
                    if (firstNowPosition < itemCount - 1) {
                        layoutManager.scrollToPositionWithOffset(firstNowPosition, 0)
                    } else {
                        // If it's the last item, just scroll smoothly to it
                        binding.recyclerView.smoothScrollToPosition(firstNowPosition)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun removeViewLsiteners() {
        binding.recyclerView.setOnTouchListener(null)
        binding.recyclerView.clearOnScrollListeners()

    }

    fun clearInstances() {
        timetableEntrySelectedListener.clear()
        tripButtonClickListener = null
        segmentActionStream = null
        stop = null
        tripSegment = null
        _tripSegment = null
        cachedStop = null
        cachedBookingActions = null
        bookingActions = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
    }

    fun selectTime() {
        val fragment = TimeDatePickerFragment.newInstance(getString(R.string.set_time))
        fragment.timeRelay
            .skip(1)
            .subscribeWithErrorHandling {
                viewModel.stopRealtime()
                viewModel.services.update(listOf())
                viewModel.onDateChanged.accept(it)
            }.addTo(autoDisposable)
        fragment.show(childFragmentManager, null)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            tripButtonClickListener?.onTripButtonClicked(p0.id, viewModel.stop.value!!)
        }
    }


    fun showShareDialog() {
        viewModel.getShareUrl(getString(R.string.share_url), stop!!)
            .take(1)
            .subscribe({ url: String? ->
                val intentPartager = Intent(Intent.ACTION_SEND)
                intentPartager.type = "text/plain"
                intentPartager.putExtra(Intent.EXTRA_TEXT, url)
                val startingIntent = Intent.createChooser(intentPartager, "Share this using...")
                startActivity(startingIntent)
            }, { Timber.e(it) }).addTo(autoDisposable)
    }

    // TODO check flow for this. I think it'll be better if we create a new instance (kill the prveious one),
    //  than using the same instance and just updating the stop
    fun updateStop(stop: ScheduledStop) {
        servicesAreLoaded = false
        this.stop = stop
        viewModel.setText(requireContext())
    }

    class Builder {
        private var showCloseButton = false
        private var showSearchBar = true
        private var stop: ScheduledStop? = null
        private var tripSegment: TripSegment? = null
        private var bookingActions: ArrayList<String>? = null
        private var buttons: MutableList<TripKitButton> = mutableListOf()
        private var actionStream: PublishSubject<TripSegment>? = null
        private var fromPreview: Boolean = false

        fun withStop(stop: ScheduledStop?): Builder {
            this.stop = stop
            return this
        }

        fun withButton(id: String, layoutResourceId: Int): Builder {
            val b = TripKitButton(id, layoutResourceId)
            buttons.add(b)
            return this
        }

        fun withBookingAction(bookingActions: List<String>?): Builder {
            if (!bookingActions.isNullOrEmpty()) {
                val list = ArrayList<String>()
                list.addAll(bookingActions.toMutableList())
                this.bookingActions = list
            }
            return this
        }

        fun hideSearchBar(): Builder {
            showSearchBar = false
            return this
        }

        fun showCloseButton(): Builder {
            showCloseButton = true
            return this
        }

        fun withSegmentActionStream(actionStream: PublishSubject<TripSegment>?): Builder {
            this.actionStream = actionStream
            return this
        }

        fun withTripSegment(tripSegment: TripSegment): Builder {
            this.tripSegment = tripSegment
            return this
        }

        fun isFromPreview(fromPreview: Boolean): Builder {
            this.fromPreview = fromPreview
            return this
        }

        fun build(): TimetableFragment {
            val args = Bundle()
            val fragment = TimetableFragment()
            args.putParcelable(ARG_STOP, stop)
            args.putStringArrayList(ARG_BOOKING_ACTION, bookingActions)
            args.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)
            args.putBoolean(ARG_SHOW_SEARCH_FIELD, showSearchBar)
            fragment.arguments = args
            fragment.buttons = buttons
            fragment.segmentActionStream = actionStream
            fragment.cachedStop = stop
            fragment.cachedShowSearchBar = showSearchBar
            fragment._tripSegment = tripSegment
            fragment.fromPreview = fromPreview
            fragment.cachedBookingActions = bookingActions

            return fragment
        }
    }

    companion object {
        // Trip segment state keys
        private const val KEY_SAVED_TRIP_SEGMENT_SERVICE_TRIP_ID = "saved_trip_segment_service_trip_id"
        private const val KEY_SAVED_TRIP_SEGMENT_ID = "saved_trip_segment_id"
        private const val KEY_SAVED_TRIP_SEGMENT_SEGMENT_ID = "saved_trip_segment_segment_id"

        // View model state keys
        private const val KEY_SAVED_FILTER_TEXT = "saved_filter_text"
        private const val KEY_SAVED_SERVICE_TRIP_ID = "saved_service_trip_id"

        // Scroll position keys
        private const val KEY_SAVED_SCROLL_POSITION = "saved_scroll_position"
        private const val KEY_SAVED_SCROLL_OFFSET = "saved_scroll_offset"

        // Cached data keys
        private const val KEY_SAVED_CACHED_STOP = "saved_cached_stop"
        private const val KEY_SAVED_CACHED_SHOW_SEARCH_BAR = "saved_cached_show_search_bar"
        private const val KEY_SAVED_FROM_PREVIEW = "saved_from_preview"
        private const val KEY_SAVED_CACHED_BOOKING_ACTIONS = "saved_cached_booking_actions"

        // Button state keys
        private const val KEY_SAVED_BUTTONS_COUNT = "saved_buttons_count"
        private const val KEY_SAVED_BUTTON_PREFIX = "saved_button_"
    }

}
