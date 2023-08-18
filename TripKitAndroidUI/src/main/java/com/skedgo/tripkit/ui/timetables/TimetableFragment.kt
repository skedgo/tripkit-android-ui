package com.skedgo.tripkit.ui.timetables

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.BaseTripKitPagerFragment
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TimetableFragmentBinding
import com.skedgo.tripkit.ui.dialog.TimeDatePickerFragment
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.model.TripKitButton
import com.skedgo.tripkit.ui.search.ARG_SHOW_SEARCH_FIELD
import com.skedgo.tripkit.ui.utils.OnSwipeTouchListener
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.Exception
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
    val clickDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this);
        super.onAttach(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(ARG_STOP, stop)
        outState.putStringArrayList(ARG_BOOKING_ACTION, bookingActions)
        outState.putBoolean(ARG_SHOW_SEARCH_FIELD, viewModel.showSearch.get())
        outState.putBoolean(ARG_SHOW_CLOSE_BUTTON, viewModel.showCloseButton.get())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //viewModel = ViewModelProvider(this, viewModelFactory).get(TimetableViewModel::class.java)

        savedInstanceState?.let { arguments = it }

        bookingActions = cachedBookingActions ?: arguments?.getStringArrayList(ARG_BOOKING_ACTION)
    }

    override fun onResume() {
        super.onResume()
        filterThrottle
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                viewModel.filter.accept(it)
            }.addTo(autoDisposable)

        viewModel.onError.observeOn(AndroidSchedulers.mainThread()).subscribe { error ->
            binding.multiStateView?.let { msv ->
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

        viewModel.stateChange.observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.multiStateView?.let { msv ->
                if (it == MultiStateView.ViewState.EMPTY) {
                    if (activity is OnResultStateListener) {
                        msv.setViewForState(
                            (activity as OnResultStateListener).provideEmptyView(),
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
            .subscribe { integer ->
                val smoothScroller = object : LinearSmoothScroller(context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }
                }

                smoothScroller.targetPosition = integer.toInt()
                binding.recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            }.addTo(autoDisposable)
//        viewModel.downloadTimetable.accept(System.currentTimeMillis() - TimeUtils.InMillis.MINUTE * 10)
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
            .subscribe {
                if (viewModel.action == "book") {
                    viewModel.buttonText.set("Booking...")
                    viewModel.enableButton.set(false)
                }
                tripPreviewPagerListener?.onServiceActionButtonClicked(
                    tripSegment,
                    viewModel.action
                )
            }.addTo(autoDisposable)

        viewModel.timetableEntryChosen.observeOn(AndroidSchedulers.mainThread()).subscribe {
            if (viewModel.action.isNotEmpty() || fromPreview) {
                tripSegment?.let { segmentActionStream?.onNext(it) }
                tripPreviewPagerListener?.onTimetableEntryClicked(
                    tripSegment,
                    viewModel.viewModelScope,
                    it
                )
            } else {
                Observable.combineLatest(viewModel.stopRelay, viewModel.startTimeRelay,
                    BiFunction { one: ScheduledStop, two: Long -> one to two })
                    .take(1).subscribe { pair ->
                        timetableEntrySelectedListener.forEach { listener ->
                            listener.onTimetableEntrySelected(
                                tripSegment,
                                it,
                                pair.first,
                                pair.second
                            )
                        }
                    }
            }
        }.addTo(autoDisposable)

        binding.recyclerView.scrollToPosition(0)

        if (stop == null) {
            stop = cachedStop
        }

        tripSegment = _tripSegment
    }

    fun setBookingActions(bookingActions: List<String>?) {
//        if (cachedBookingActions != null) return
        viewModel.enableButton.set(true)
        if (!bookingActions.isNullOrEmpty()) {
            val list = ArrayList<String>()
            list.addAll(bookingActions.toMutableList())
            arguments?.putStringArrayList(ARG_BOOKING_ACTION, list)
            try {
                this.bookingActions = list
            } catch (e: Exception) {
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TimetableFragmentBinding.inflate(layoutInflater)

        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.serviceLineRecyclerView.layoutManager = layoutManager

        binding.viewModel = viewModel
        binding.serviceLineRecyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.isNestedScrollingEnabled = true

        /*
        binding.recyclerView.setOnTouchListener { view, motionEvent ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            view.onTouchEvent(motionEvent)
            true
        }
        */

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
                /*
                if (dy >= 0) {
                    binding.goToNowButton.setVisibility(View.GONE)
                } else {
                    binding.goToNowButton.setVisibility(View.VISIBLE)
                }
                */

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

                if (!recyclerView.canScrollVertically(1) && !viewModel.showLoading.get()) {
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
        stop = arguments?.getParcelable(ARG_STOP)
        if (stop == null) {
            stop = cachedStop
        }
        tripSegment = _tripSegment

        binding.goToNowButton.setOnClickListener {
            val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
            val firstNowPosition = viewModel.getFirstNowPosition()
            if (layoutManager.findFirstVisibleItemPosition() < firstNowPosition &&
                firstNowPosition != 0 &&
                (firstNowPosition + 1) < binding.recyclerView.adapter?.itemCount ?: 0
            ) {
                binding.recyclerView.scrollToPosition(firstNowPosition + 1)
            } else {
                binding.recyclerView.scrollToPosition(firstNowPosition)
            }
        }

        bookingActions =
            cachedBookingActions ?: savedInstanceState?.getStringArrayList(ARG_BOOKING_ACTION)
                    ?: arguments?.getStringArrayList(ARG_BOOKING_ACTION)

        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        viewModel.showCloseButton.set(showCloseButton)

        val showSearchBar = arguments?.getBoolean(ARG_SHOW_SEARCH_FIELD) ?: cachedShowSearchBar
        viewModel.showSearch.set(showSearchBar)

//        tripSegment?.ticket?.let {
//            viewModel.showButton.set(true)
//            viewModel.buttonText.set("Book")
//        }

        binding.closeButton.setOnClickListener(onCloseButtonListener)

        segmentActionStream
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                /*
                if (it.first == stop?.code) {
                    setBookingActions(it.second)
                }
                */
                //setBookingActions(tripSegment?.booking?.externalActions ?: it.second)
//                    setBookingActions(it.booking?.externalActions)
            }, {
                it.printStackTrace()
            })?.addTo(autoDisposable)

    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }

    override fun onDestroyView() {
        removeViewLsiteners()
        super.onDestroyView()
        clearInstances()
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
            .subscribe {
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
            }, { Timber.e(it) })
    }

    fun updateStop(stop: ScheduledStop) {
        this.stop = stop
        viewModel.setText(requireContext())
    }

//    fun setTripSegment(tripSegment: TripSegment?) {
//        this.tripSegment = tripSegment
//    }

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

}
