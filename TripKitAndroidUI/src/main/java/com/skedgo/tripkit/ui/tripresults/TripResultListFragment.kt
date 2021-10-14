package com.skedgo.tripkit.ui.tripresults

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.model.ViewTrip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.OnResultStateListener
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.TripResultListFragmentBinding
import com.skedgo.tripkit.ui.dialog.TripKitDateTimePickerDialogFragment
import com.skedgo.tripkit.ui.tripresult.UpdateTripForRealtime
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandler
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButtonHandlerFactory
import com.skedgo.tripkit.ui.views.MultiStateView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.trip_result_list_fragment.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class TripResultListFragment : BaseTripKitFragment() {
    /**
     * This callback will be invoked when a search result is clicked.
     */
    interface OnTripSelectedListener {
        fun onTripSelected(viewTrip: ViewTrip, tripGroupList: List<TripGroup>)
    }

    private var multiStateErrorTextId: Int = View.NO_ID
    private var tripSelectedListener: OnTripSelectedListener? = null
    fun setOnTripSelectedListener(callback: OnTripSelectedListener) {
        this.tripSelectedListener = callback
    }

    fun setOnTripSelectedListener(callback:(ViewTrip, List<TripGroup>) -> Unit) {
        this.tripSelectedListener = object: OnTripSelectedListener {
            override fun onTripSelected(viewTrip: ViewTrip, tripGroupList: List<TripGroup>) {
                callback(viewTrip, tripGroupList)
            }
        }
    }

    interface OnLocationClickListener {
        fun onStartLocationClicked()
        fun onDestinationLocationClicked()
    }

    private var locationClickListener: OnLocationClickListener? = null
    fun setOnLocationClickListener(listener: OnLocationClickListener) {
        this.locationClickListener = listener
    }

    fun setOnLocationClickListener(startLocationClicked:() -> Unit, destinationLocationClicked:() -> Unit) {
        this.locationClickListener = object: OnLocationClickListener {
            override fun onStartLocationClicked() {
                startLocationClicked()
            }

            override fun onDestinationLocationClicked() {
                destinationLocationClicked()
            }
        }
    }

    @Inject
    lateinit var viewModelProviderFactory: TripResultListViewModelFactory
    private lateinit var viewModel: TripResultListViewModel
    lateinit var binding: TripResultListFragmentBinding
    private var query: Query? = null
    private var transportModeFilter: TransportModeFilter? = null
    var actionButtonHandlerFactory: ActionButtonHandlerFactory?= null
    private var showTransportSelectionView = true

    fun query(): Query {
        return viewModel.query
    }

    fun setQuery(query: Query) {
        viewModel.changeQuery(query)
    }

    var shouldShowMoreButton = false

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().routesComponent().inject(this);
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelProviderFactory)
                .get(TripResultListViewModel::class.java);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = TripResultListFragmentBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        viewModel.showCloseButton.set(showCloseButton)
        binding.closeButton.setOnClickListener(onCloseButtonListener)
        binding.toLocation.setOnClickListener {
            locationClickListener?.onDestinationLocationClicked()
        }
        binding.from.setOnClickListener {
            locationClickListener?.onStartLocationClicked()
        }

        binding.leaveNowLayout.setOnClickListener { showDateTimePicker() }
//        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.reload() }
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        binding.transportItemsView.layoutManager = layoutManager

        return binding.root
    }

    fun showCloseButton(showCloseButton: Boolean) {
        viewModel.showCloseButton.set(showCloseButton)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onFinished.observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
        }.addTo(autoDisposable)

        viewModel.onError.observeOn(AndroidSchedulers.mainThread()).subscribe { error ->
            binding.multiStateView?.let { msv ->
                if (activity is OnResultStateListener) {
                    val view = (activity as OnResultStateListener).provideErrorView(error)
                    msv.setViewForState(view, MultiStateView.ViewState.ERROR, true)
                } else {
                    val view = LayoutInflater.from(activity).inflate(R.layout.generic_error_view, null)
                    view?.findViewById<TextView>(R.id.errorMessageView)?.text = error
                    msv.setViewForState(view, MultiStateView.ViewState.ERROR, true)
                }
            }
        }.addTo(autoDisposable)

        viewModel.onItemClicked
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { viewTrip -> tripSelectedListener?.onTripSelected(viewTrip, viewModel.tripGroupList)
                }.subscribe().addTo(autoDisposable)

//        viewModel.onMoreButtonClicked
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    trip ->
//                    if (actionButtonHandler?.actionClicked(trip) == true) {
//                        // We should monitor this trip for changes, likely due to booking
////                        viewModel.monitorTrip(trip)
//                        Timber.d("Triggering updates")
//                        updateTripRelay.accept(trip.group)
//                    }
//                }.addTo(autoDisposable)
        viewModel.stateChange.observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.multiStateView?.let { msv ->
                if (it == MultiStateView.ViewState.EMPTY) {
                    if (activity is OnResultStateListener) {
                        msv.setViewForState((activity as OnResultStateListener).provideEmptyView(), MultiStateView.ViewState.EMPTY, true)
                    } else {
                        val view = LayoutInflater.from(activity).inflate(R.layout.generic_empty_view, null)
                        msv.setViewForState(view, MultiStateView.ViewState.EMPTY, true)
                    }
                } else if (it == MultiStateView.ViewState.CONTENT) {
                    msv.viewState = MultiStateView.ViewState.CONTENT
                }
            }
        }.addTo(autoDisposable)
    }

    private fun showDateTimePicker() {
        var departureTimezone: String? = null
        var arrivalTimezone: String? = null

        var timeMillis = System.currentTimeMillis()
        val timeTag = viewModel.query.timeTag
        if (!timeTag!!.isDynamic) {
            timeMillis = TimeUnit.SECONDS.toMillis(timeTag.timeInSecs)
        }

        if (viewModel.query.fromLocation != null) {
            departureTimezone = viewModel.query.fromLocation!!.timeZone
        }
        if (viewModel.query.toLocation != null) {
            arrivalTimezone = viewModel.query.toLocation!!.timeZone
        }

        try {
            var fragment = TripKitDateTimePickerDialogFragment.Builder()
                    .withTimeZones(departureTimezone, arrivalTimezone)
                    .withTimeType(timeTag.type)
                    .timeMillis(timeMillis)
                    .build()
            fragment.setOnTimeSelectedListener(object: TripKitDateTimePickerDialogFragment.OnTimeSelectedListener {
                override fun onTimeSelected(timeTag: TimeTag) {
                     viewModel.updateQueryTime(timeTag)
                }
            })
            fragment.show(fragmentManager!!, "timePicker")
        } catch (error: IllegalStateException) {
            // To prevent https://fabric.io/skedgo/android/apps/com.buzzhives.android.tripplanner/issues/5967e7f0be077a4dcc839dc5.
            Timber.e("An error occurred", error)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        query = arguments?.getParcelable<Query>(ARG_QUERY) as Query
        arguments?.getParcelable<TransportModeFilter>(ARG_TRANSPORT_MODE_FILTER)?.let {
            transportModeFilter = it
        }

        showTransportSelectionView = arguments?.getBoolean(ARG_SHOW_TRANSPORT_MODE_SELECTION, true)!!
        query?.let { viewModel.setup(it, showTransportSelectionView, transportModeFilter, actionButtonHandlerFactory) }
    }

    class Builder {
        private var query: Query? = null
        private var transportModeFilter: TransportModeFilter? = null
        private var showTransportModeSelection = true
        private var showCloseButton = false
        private var actionButtonHandlerFactory: ActionButtonHandlerFactory? = null

        fun withQuery(query: Query): Builder {
            this.query = query
            return this
        }

        fun withTransportModeFilter(transportModeFilter: TransportModeFilter): Builder {
            this.transportModeFilter = transportModeFilter
            return this
        }

        fun withActionButtonHandlerFactory(factory: ActionButtonHandlerFactory): Builder {
            this.actionButtonHandlerFactory = factory
            return this
        }

        fun showTransportModeSelection(showSelection: Boolean): Builder {
            this.showTransportModeSelection = showSelection
            return this
        }

        fun showCloseButton(): Builder {
            this.showCloseButton = true
            return this
        }

        fun build(): TripResultListFragment {
            val args = Bundle()
            val fragment = TripResultListFragment()
            args.putParcelable(ARG_QUERY, query)
            args.putParcelable(ARG_TRANSPORT_MODE_FILTER, transportModeFilter)
            args.putBoolean(ARG_SHOW_TRANSPORT_MODE_SELECTION, showTransportModeSelection)
            args.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)
            fragment.arguments = args
            fragment.actionButtonHandlerFactory = actionButtonHandlerFactory
            return fragment
        }
    }
}
