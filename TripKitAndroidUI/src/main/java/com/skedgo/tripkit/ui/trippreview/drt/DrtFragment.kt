package com.skedgo.tripkit.ui.trippreview.drt

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.URLUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.skedgo.tripkit.booking.quickbooking.QuickBookingType
import com.skedgo.tripkit.common.model.BookingConfirmationAction
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentDrtBinding
import com.skedgo.tripkit.ui.dialog.*
import com.skedgo.tripkit.ui.generic.action_list.ActionListAdapter
import com.skedgo.tripkit.ui.generic.transport.TransportDetails
import com.skedgo.tripkit.ui.interactor.TripKitEvent
import com.skedgo.tripkit.ui.interactor.TripKitEventBus
import com.skedgo.tripkit.ui.payment.PaymentData
import com.skedgo.tripkit.ui.payment.PaymentSummaryDetails
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.utils.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DrtFragment : BaseFragment<FragmentDrtBinding>(), DrtHandler {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var tripKitEventBus: TripKitEventBus

    private val viewModel: DrtViewModel by viewModels { viewModelFactory }

    private val pagerItemViewModel: TripPreviewPagerItemViewModel by viewModels()

    private var segment: TripSegment? = null

    private var paymentDataStream: PublishSubject<PaymentData>? = null

    private var tripSegmentUpdateCallback: ((TripSegment) -> Unit)? = null

    private var cachedReturnMills: Long = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(60)

    private var focusedAfterBookingConfirmed = false

    internal var bottomSheetDragToggleCallback: ((Boolean) -> Unit)? = null

    private var bookingCallback: ((Boolean, PaymentData?) -> Unit)? = null

    @Inject
    lateinit var actionsAdapter: ActionListAdapter

    override val layoutRes: Int
        get() = R.layout.fragment_drt

    override val observeAccessibility: Boolean
        get() = true

    override fun getDefaultViewForAccessibility(): View {
        return binding.drtClTitle
    }

    override fun onBook() {
        viewModel.book()
    }

    override fun onCancelRide() {

    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().tripPreviewComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstance: Bundle?) {
        initBinding()
        initObserver()
        initViews()
    }

    override fun onPause() {
        super.onPause()
        bottomSheetDragToggleCallback?.invoke(true)
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.pagerItemViewModel = pagerItemViewModel
        binding.handler = this
    }

    private fun initObserver() {
        viewModel.onItemChangeActionStream
            .onEach { drtItem ->
                if (drtItem.type.value.equals("notes", true)) {
                    GenericListDisplayDialogFragment.newInstance(
                        GenericListItem.parseOptions(
                            drtItem.options.value ?: emptyList()
                        ),
                        title = drtItem.type.value ?: ""
                    ).show(childFragmentManager, drtItem.label.value ?: "")
                } else {
                    val defaultValue = viewModel.getDefaultValueByType(
                        drtItem.type.value ?: "",
                        drtItem.label.value ?: ""
                    )

                    when (drtItem.label.value) {
                        DrtItem.ADD_NOTE -> {
                            GenericNoteDialogFragment.newInstance(
                                drtItem.label.value ?: "",
                                if (drtItem.values.value?.firstOrNull() != defaultValue) {
                                    drtItem.values.value?.firstOrNull() ?: ""
                                } else {
                                    ""
                                },
                                viewModel.bookingConfirmation.value != null
                            ) {
                                if (it.isEmpty()) {
                                    listOf(defaultValue)
                                } else {
                                    drtItem.setValue(listOf(it))
                                    viewModel.updateInputValue(drtItem)
                                }
                            }.show(childFragmentManager, drtItem.label.value ?: "")
                        }
                        DrtItem.RETURN_TRIP -> {
                            showDateTimePicker(
                                object :
                                    TripKitDateTimePickerDialogFragment.OnTimeSelectedListener {
                                    override fun onTimeSelected(timeTag: TimeTag) {
                                        if (timeTag.isLeaveNow) {
                                            drtItem.setValue(listOf(getString(R.string.one_way_only)))
                                        } else {
                                            val rawTz = DateTimeZone.forID("UTC")
                                            val segmentTz = DateTimeZone.forID(
                                                segment?.timeZone
                                                    ?: "UTC"
                                            )
                                            val dateTime = DateTime(timeTag.timeInMillis)
                                            cachedReturnMills = timeTag.timeInMillis /*/ 1000*/

                                            val isoDate =
                                                dateTime.toString(getISODateFormatter(rawTz))
                                            val rawDateBuilder = StringBuilder()
                                            rawDateBuilder.append(isoDate).append("Z")
                                            val dateString =
                                                dateTime.toString(getDisplayDateFormatter(segmentTz))
                                            val timeString =
                                                dateTime.toString(getDisplayTimeFormatter(segmentTz))

                                            drtItem.setRawDate(rawDateBuilder.toString())
                                            drtItem.setValue(listOf("$dateString at $timeString"))
                                        }
                                        viewModel.updateInputValue(drtItem)
                                    }
                                }
                            )
                        }
                        else -> {
                            GenericListDialogFragment.newInstance(
                                GenericListItem.parseOptions(
                                    drtItem.options.value ?: emptyList()
                                ),
                                isSingleSelection = drtItem.type.value == QuickBookingType.SINGLE_CHOICE,
                                title = drtItem.label.value ?: "",
                                onConfirmCallback = { selectedItems ->
                                    drtItem.setValue(
                                        if (selectedItems.isEmpty()) {
                                            listOf(
                                                viewModel.getDefaultValueByType(
                                                    drtItem.type.value ?: "",
                                                    drtItem.label.value ?: ""
                                                )
                                            )
                                        } else {
                                            selectedItems.map { it.label }
                                        }
                                    )
                                    viewModel.updateInputValue(drtItem)
                                },
                                previousSelectedValues = if (drtItem.values.value != listOf(
                                        defaultValue
                                    )
                                ) {
                                    drtItem.values.value
                                } else {
                                    null
                                },
                                viewOnlyMode = viewModel.bookingConfirmation.value != null
                            ).show(childFragmentManager, drtItem.label.value ?: "")
                        }
                    }
                }
                emitPaymentData()
            }.launchIn(lifecycleScope)

        segment?.let {
            pagerItemViewModel.setSegment(requireContext(), it)
            viewModel.setTripSegment(it)
            cachedReturnMills = TimeUnit.SECONDS.toMillis(it.startTimeInSecs + 3600)
            TripKitEventBus.publish(
                TripKitEvent.OnToggleDrtFooterVisibility(
                    it.booking?.confirmation?.status()?.value()
                            == BookingConfirmationStatusValue.PROCESSING
                )
            )
        }

        viewModel.onTicketChangeActionStream
            .onEach { drtTicket ->
                /*
                var totalTickets = 0.0
                var numberTickets = 0L
                viewModel.tickets.forEach {
                    totalTickets += (it.price.value ?: 0.0).times(it.value.value ?: 0)
                    numberTickets += (it.value.value ?: 0)
                }

                viewModel.setTotalTickets(totalTickets, (vm.currency.value ?: ""))
                viewModel.setNumberTickets(numberTickets)
                */

                viewModel.updateTicketValue(drtTicket)

                emitPaymentData()
            }.launchIn(lifecycleScope)

        viewModel.onEmitPaymentDataStream
            .onEach {
                emitPaymentData()
            }.launchIn(lifecycleScope)

        observe(viewModel.confirmationActions) {
            it?.let { actionsAdapter.collection = it }
        }

        observe(viewModel.segment) {
            it?.let { tripSegmentUpdateCallback?.invoke(it) }
        }

        observe(viewModel.bookingConfirmation) {
            it?.let {
                if (!focusedAfterBookingConfirmed) {
                    focusAccessibilityDefaultView(false)
                    focusedAfterBookingConfirmed = true
                }
            }
        }

        observe(viewModel.error) {
            it?.let { errorPair ->
                if (errorPair.first != null || errorPair.second != null) {
                    context?.showConfirmationPopUpDialog(
                        errorPair.first,
                        errorPair.second,
                        resources.getString(R.string.ok)
                    )
                }
            }
        }

        observe(viewModel.goToPayment) {
            it?.let {
                bookingCallback?.invoke(true, generatePaymentData())
            }
        }

    }

    private fun emitPaymentData() {
        paymentDataStream?.onNext(generatePaymentData())
    }

    private fun generatePaymentData(): PaymentData {
        val drtItems = (if (viewModel.items.isEmpty()) listOf() else viewModel.items).filter {
            val defaultValue = viewModel.getDefaultValueByType(
                it.type.value ?: "", it.label.value ?: ""
            )
            it.values.value?.isNotEmpty() == true && it.getItemValueAsString() != defaultValue
        }
        val drtTickets = (if (viewModel.tickets.isEmpty()) listOf() else viewModel.tickets).filter {
            it.value.value ?: 0L > 0L
        }


//        val summaryDetails = viewModel.review.value?.firstOrNull()?.let { it.tickets.map { PaymentSummaryDetails.parseTicket(it) } }
//                ?: kotlin.run { drtTickets.map { it.generateSummaryDetails() } + drtItems.map { it.generateSummaryDetails() } }
        val summaryDetails =
            drtTickets.map { it.generateSummaryDetails() } + drtItems.map { it.generateSummaryDetails() }

        val a = viewModel.review.value?.firstOrNull()?.let { TransportDetails.parseFromReview(it) }
        val b = pagerItemViewModel.generateTransportDetails()
        val transportDetails = a ?: b
        val total = drtTickets.sumOf {
            ((it.price.value ?: 0.0) / 100) * (it.value.value?.toDouble() ?: 0.0)
        }

        val currency = drtTickets.firstOrNull { it.currency.value != null }?.currency?.value

        return PaymentData(
            this@DrtFragment.hashCode(),
            pagerItemViewModel.modeTitle.get().toString(),
            pagerItemViewModel.modeIconUrl.get(),
            pagerItemViewModel.segment?.darkVehicleIcon,
            summaryDetails,
            transportDetails,
            total,
            currency ?: "",
            viewModel.paymentOptions.value,
            viewModel.review.value
        )
    }

    private fun showDateTimePicker(listener: TripKitDateTimePickerDialogFragment.OnTimeSelectedListener) {
        try {
            val fragment = TripKitDateTimePickerDialogFragment.Builder()
                .withTitle(getString(R.string.set_time))
                .timeMillis(cachedReturnMills)
                .withPositiveAction(R.string.done)
                .isSingleSelection("Return Trip")
                .withNegativeAction(R.string.one_way_only)
                .withTimeZones(
                    segment?.timeZone ?: TimeZone.getDefault().id, segment?.timeZone
                        ?: TimeZone.getDefault().id
                )
                .build()
            fragment.setOnTimeSelectedListener(listener)
            fragment.show(requireFragmentManager(), "timePicker")
        } catch (error: IllegalStateException) {
            // To prevent https://fabric.io/skedgo/android/apps/com.buzzhives.android.tripplanner/issues/5967e7f0be077a4dcc839dc5.
            Timber.e("An error occurred", error)
        }
    }

    override fun onResume() {
        super.onResume()
        bottomSheetDragToggleCallback?.invoke(false)
        pagerItemViewModel.closeClicked.observable
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onCloseButtonListener?.onClick(null) }.addTo(autoDisposable)

        tripKitEventBus.listen(TripKitEvent.OnBookDrt::class.java)
            .subscribe {
                if (it.fragmentHashCode == this@DrtFragment.hashCode()) {
                    viewModel.book()
                }
                bookingCallback = it.bookCallBack
            }.addTo(autoDisposable)


        tripKitEventBus.listen(TripKitEvent.OnDrtConfirmPaymentUpdate::class.java)
            .subscribe {
                if (URLUtil.isValidUrl(it.updateUrl)) {
                    viewModel.processBookingResponse(it.updateUrl)
                }
            }.addTo(autoDisposable)

    }

    private fun initViews() {
        val gridLayoutManager = GridLayoutManager(requireActivity(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (actionsAdapter.collection.size % 2 > 0) {
                    if (position == actionsAdapter.collection.size - 1) {
                        return 2
                    }
                }
                return 1
            }
        }

        binding.drtRvActions.layoutManager = gridLayoutManager
        binding.drtRvActions.adapter = actionsAdapter
        actionsAdapter.clickListener = {
            when {
                it.bookingConfirmationAction?.externalURL() != null -> {
                    if (it.bookingConfirmationAction.type() == BookingConfirmationAction.TYPE_CALL) {
                        val intent = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(it.bookingConfirmationAction.externalURL())
                        )
                        requireActivity().startActivity(intent)
                    }
                }
                !it.bookingConfirmationAction?.confirmationMessage().isNullOrEmpty() -> {
                    requireContext().showConfirmationPopUpDialog(
                        title = it.bookingConfirmationAction?.title(),
                        message = it.bookingConfirmationAction?.confirmationMessage(),
                        positiveLabel = "Yes",
                        positiveCallback = {
                            viewModel.processAction(it.bookingConfirmationAction)
                        },
                        negativeLabel = "No"
                    )
                }
                it.bookingConfirmationAction?.type() == BookingConfirmationAction.TYPE_REQUEST_ANOTHER -> {
                    tripPreviewPagerListener?.onRestartHomePage()
                }
                else -> {
                    viewModel.processAction(it.bookingConfirmationAction)
                }
            }
        }

        /*
        binding.drtNsv.setOnTouchListener { view, motionEvent ->
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

        binding.drtNsv.setOnTouchListener(swipeListener)

    }

    companion object {
        fun newInstance(
            segment: TripSegment,
            paymentDataStream: PublishSubject<PaymentData>?,
            tripSegmentUpdateCallback: ((TripSegment) -> Unit)? = null,
        ): DrtFragment {
            val fragment = DrtFragment()
            fragment.segment = segment
            fragment.tripSegmentUpdateCallback = tripSegmentUpdateCallback
            fragment.paymentDataStream = paymentDataStream
            return fragment
        }
    }
}