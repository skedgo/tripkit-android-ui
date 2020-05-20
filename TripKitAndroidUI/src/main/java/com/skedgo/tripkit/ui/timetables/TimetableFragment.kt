package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.AutoDisposable
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.rxproperty.asObservable
import com.skedgo.tripkit.ui.databinding.TimetableFragmentBinding
import com.skedgo.tripkit.ui.dialog.TimeDatePickerFragment
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.model.TripKitButton
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class TimetableFragment : BaseTripKitFragment(), View.OnClickListener {
    /**
     * This callback will be invoked when a specific timetable entry is clicked.
     */
    interface OnTimetableEntrySelectedListener {
        fun onTimetableEntrySelected(service: TimetableEntry, stop : ScheduledStop, minStartTime: Long)
    }

    private var timetableEntrySelectedListener: MutableList<OnTimetableEntrySelectedListener> = mutableListOf()
    fun addOnTimetableEntrySelectedListener(callback: OnTimetableEntrySelectedListener) {
        if (!timetableEntrySelectedListener.contains(callback)) {
            timetableEntrySelectedListener.add(callback)
        }
    }

    fun addOnTimetableEntrySelectedListener(listener:(TimetableEntry, ScheduledStop, Long) -> Unit) {
        this.timetableEntrySelectedListener.add(object: OnTimetableEntrySelectedListener {
            override fun onTimetableEntrySelected(service: TimetableEntry, stop: ScheduledStop, minStartTime: Long) {
                listener(service, stop, minStartTime)
            }
        })
    }


    /**
        When you provide the Timetable fragment with buttons, this callback will be called when one is clicked.
     */
    interface OnTripKitButtonClickListener {
        fun onTripButtonClicked(id: String, stop: ScheduledStop)
    }

    private var tripButtonClickListener: OnTripKitButtonClickListener? = null
    fun setOnTripKitButtonClickListener(listener: OnTripKitButtonClickListener) {
        this.tripButtonClickListener = listener
    }
    fun setOnTripKitButtonClickListener(listener:(String, ScheduledStop) -> Unit) {
        this.tripButtonClickListener = object: OnTripKitButtonClickListener {
            override fun onTripButtonClicked(id: String, stop: ScheduledStop) {
                listener(id, stop)
            }
        }
    }

    @Inject
    lateinit var viewModel: TimetableViewModel
    var stop: ScheduledStop? = null
        set(value) {
            if (value != null) {
                this.viewModel.stop.accept(value);
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
                    msv.setViewForState((activity as OnResultStateListener).provideErrorView(error),
                            MultiStateView.ViewState.ERROR, true)
                } else {
                    val errorView = LayoutInflater.from(activity).inflate(R.layout.generic_error_view, null)
                    errorView?.findViewById<TextView>(R.id.errorMessageView)?.text = error
                    msv.setViewForState(errorView, MultiStateView.ViewState.ERROR, true)
                }
            }
        }.addTo(autoDisposable)

        viewModel.stateChange.observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.multiStateView?.let { msv ->
                if (it == MultiStateView.ViewState.EMPTY) {
                    if (activity is OnResultStateListener) {
                        msv.setViewForState((activity as OnResultStateListener).provideEmptyView(), MultiStateView.ViewState.EMPTY, true)
                    } else {
                        val emptyView = LayoutInflater.from(activity).inflate(R.layout.generic_empty_view, null)
                        msv.setViewForState(emptyView, MultiStateView.ViewState.EMPTY, true)
                    }
                }
            }
        }.addTo(autoDisposable)

        viewModel.scrollToNow
                .delay(500, TimeUnit.MILLISECONDS) // 500 ms is a guess, wait for the data to be set to the adapter.
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
        viewModel.downloadTimetable.accept(System.currentTimeMillis() - TimeUtils.InMillis.MINUTE * 10)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = TimetableFragmentBinding.inflate(layoutInflater)

        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.serviceLineRecyclerView.layoutManager = layoutManager

        binding.viewModel = viewModel
        binding.serviceLineRecyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.isNestedScrollingEnabled = true
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy >= 0) {
                    binding.goToNowButton.setVisibility(View.GONE)
                } else {
                    binding.goToNowButton.setVisibility(View.VISIBLE)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (!recyclerView.canScrollVertically(1) && !viewModel.showLoading.get()) {
                    viewModel.downloadMoreTimetableAsync()
                }
            }
        })
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        binding.departuresSearchSetTime.timeSet.setOnClickListener {
            selectTime()
        }

        val search =  binding.departuresSearchSetTime.stationSearch
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
                binding.buttonsFlexboxLayout.addView(button)
            } catch (e: InflateException) {
                Timber.e("Invalid button layout ${it.layoutResourceId}", e)
            }
        }

        // For some reason, subscribing to this in onResume() loses click events after resuming.
        // Doing so here, in onCreateView(), works.
        clickDisposable.add(viewModel.services.asObservable()
                .map {
                    it.map {vm ->
                        vm.onItemClick.observable
                    }
                }
                .switchMap {
                    Observable.merge(it)
                }
                .subscribe { timetableEntry ->
                    Observable.combineLatest(viewModel.stopRelay, viewModel.startTimeRelay,
                            BiFunction { one: ScheduledStop, two: Long -> one to two })
                            .take(1).subscribe { pair ->
                                timetableEntrySelectedListener.forEach { listener ->
                                    listener.onTimetableEntrySelected(timetableEntry, pair.first, pair.second)
                                }
                            }
                })

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0);
        }

        viewModel.setText()
        arguments?.let {
            handleArguments(it)
        }
    }

    private fun handleArguments(it: Bundle) {
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stop = arguments?.getParcelable(ARG_STOP)
        binding.goToNowButton.setOnClickListener { binding.recyclerView.scrollToPosition(viewModel.getFirstNowPosition()) }
        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        viewModel.showCloseButton.set(showCloseButton)
        binding.closeButton.setOnClickListener(onCloseButtonListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
        clickDisposable.clear()
    }

    fun selectTime() {
        val fragment = TimeDatePickerFragment.newInstance(getString(R.string.set_time))
        fragment.timeRelay
                .subscribe {
                    viewModel.onDateChanged.accept(it)
                }.addTo(autoDisposable)

        fragment.show(childFragmentManager, null)
    }

    override fun onClick(p0: View?) {
        if (p0 != null) {
            tripButtonClickListener?.onTripButtonClicked(p0.tag as String, viewModel.stop.value!!)
        }
    }

    class Builder {
        private var showCloseButton = false
        private var stop: ScheduledStop? = null
        private var buttons: MutableList<TripKitButton> = mutableListOf()

        fun withStop(stop: ScheduledStop?): Builder {
            this.stop = stop
            return this
        }

        fun withButton(id: String, layoutResourceId: Int): Builder {
            val b = TripKitButton(id, layoutResourceId)
            buttons.add(b)
            return this
        }

        fun showCloseButton(): Builder {
            showCloseButton = true
            return this
        }
        fun build(): TimetableFragment {
            val args = Bundle()
            val fragment = TimetableFragment()
            args.putParcelable(ARG_STOP, stop)
            args.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)
            fragment.arguments = args
            fragment.buttons = buttons

            return fragment
        }
    }

}
