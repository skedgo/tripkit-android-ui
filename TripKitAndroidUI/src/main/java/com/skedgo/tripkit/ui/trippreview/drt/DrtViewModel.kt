package com.skedgo.tripkit.ui.trippreview.drt

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.booking.quickbooking.*
import com.skedgo.tripkit.common.model.BookingConfirmation
import com.skedgo.tripkit.common.model.BookingConfirmationAction
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.logError
import com.skedgo.tripkit.ui.interactor.TripKitEvent
import com.skedgo.tripkit.ui.interactor.TripKitEventBus
import com.skedgo.tripkit.ui.utils.getDisplayDateFormatter
import com.skedgo.tripkit.ui.utils.getDisplayTimeFormatter
import com.skedgo.tripkit.ui.utils.getISODateFormatter
import com.skedgo.tripkit.ui.utils.updateFields
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import retrofit2.HttpException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.HashMap

@SuppressLint("StaticFieldLeak")
class DrtViewModel @Inject constructor(
        private val context: Context,
        private val quickBookingService: QuickBookingService,
        private val resources: Resources,
        private val bookingService: BookingV2TrackingService
) : RxViewModel() {

    private val _quickBooking = MutableLiveData<QuickBooking>()

    private val _inputs = MutableLiveData<List<Input>>()

    private val _tickets = MutableLiveData<List<Ticket>>()

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _areInputsValid = MutableLiveData<Boolean>()
    val areInputsValid: LiveData<Boolean> = _areInputsValid

    private val _error = MutableLiveData<Pair<String?, String?>>()
    val error: LiveData<Pair<String?, String?>> = _error

    val onItemChangeActionStream = MutableSharedFlow<DrtItemViewModel>()

    val onTicketChangeActionStream = MutableSharedFlow<DrtTicketViewModel>()

    val onEmitPaymentDataStream = MutableSharedFlow<DrtViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemViewModel.diffCallback())
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    val tickets = DiffObservableList<DrtTicketViewModel>(DrtTicketViewModel.diffCallback())
    val ticketBinding = ItemBinding.of<DrtTicketViewModel>(BR.viewModel, R.layout.item_drt_ticket)

    private val _bookingConfirmation = MutableLiveData<BookingConfirmation>()
    val bookingConfirmation: LiveData<BookingConfirmation> = _bookingConfirmation

    private val _confirmationActions =
            MutableLiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>>()
    val confirmationActions: LiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>> =
            _confirmationActions

    private val _accessibilityLabel = MutableLiveData<String>()
    val accessibilityLabel: LiveData<String> = _accessibilityLabel

    private val _isHideExactTimes = MutableLiveData<Boolean>()
    val isHideExactTimes: LiveData<Boolean> = _isHideExactTimes

    /*
    private val _totalTickets = MutableLiveData<Double>()
    val totalTickets: LiveData<Double> = _totalTickets

    private val _numberTickets = MutableLiveData<Long>()
    val numberTickets: LiveData<Long> = _numberTickets

    private val _labelTicketQuantity = MutableLiveData<String>()
    val labelTicketQuantity: LiveData<String> = _labelTicketQuantity

    private val _labelTicketTotal = MutableLiveData<String>()
    val labelTicketTotal: LiveData<String> = _labelTicketTotal
    */

    private val _paymentOptions = MutableLiveData<List<PaymentOption>>()
    val paymentOptions: LiveData<List<PaymentOption>> = _paymentOptions

    private val _review = MutableLiveData<List<Review>>()
    val review: LiveData<List<Review>> = _review

    private val _goToPayment = MutableLiveData<Unit>()
    val goToPayment: LiveData<Unit> = _goToPayment

    private val stopPollingUpdate = AtomicBoolean()

    private val quickBookingObserver = Observer<QuickBooking> {
        generateDrtTickets(it.tickets)
        generateDrtItems(it.input)
        getBookingUpdate(it.tripUpdateURL)
    }

    private val inputsObserver = Observer<List<Input>> { inputs ->
        _areInputsValid.value = inputs.none {
            it.required &&
                    ((it.type == QuickBookingType.MULTIPLE_CHOICE && !it.options.isNullOrEmpty() && it.values.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.SINGLE_CHOICE && !it.options.isNullOrEmpty() && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.LONG_TEXT && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.NUMBER && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.RETURN_TRIP && it.value.isNullOrEmpty()))
        }
    }

    private val bookingConfirmationObserver = Observer<BookingConfirmation> {
        it?.let {
            TripKitEventBus.publish(
                TripKitEvent.OnToggleDrtFooterVisibility(
                    it.status()?.value() != null
                )
            )

            val result = mutableListOf<DrtItemViewModel>()
            it.input().forEach { input ->
                if ((!input.value().isNullOrEmpty() && input.value() != getDefaultValueByType(input.type(), input.title())) ||
                        (!input.values().isNullOrEmpty() && input.values() != getDefaultValue(Input.parse(input)))) {
                    result.add(
                            DrtItemViewModel().apply {
                                setIcon(getIconById(input.id()))
                                setLabel(input.title())
                                setType(input.type())
                                setValue(
                                        if (input.type().equals(QuickBookingType.RETURN_TRIP, true)) {
                                            val segmentTz = DateTimeZone.forID(
                                                    segment.value?.timeZone
                                                            ?: "UTC"
                                            )
                                            val rawTz = DateTimeZone.forID("UTC")

                                            try {
                                                val dateTime = DateTime.parse(
                                                        input.value()?.replace("Z", ""),
                                                        getISODateFormatter(rawTz)
                                                )
                                                val dateString =
                                                        dateTime.toString(getDisplayDateFormatter(segmentTz))
                                                val timeString =
                                                        dateTime.toString(getDisplayTimeFormatter(segmentTz))
                                                listOf("$dateString at $timeString")
                                            } catch (e: Exception) {
                                                listOf("One-way only")
                                            }
                                        } else {
                                            if (!input.value().isNullOrEmpty()) {
                                                listOf(input.value() ?: "")
                                            } else if (!input.values().isNullOrEmpty()) {
                                                input.values() ?: listOf("")
                                            } else {
                                                val list = ArrayList<String>()
                                                val map = HashMap<String, String>()
                                                input.options()?.forEach { option ->
                                                    map[option.id()] = option.title()
                                                }
                                                input.values()?.forEach { value ->
                                                    map[value]?.let { title -> list.add(title) }
                                                }
                                                list
                                            }
                                        }
                                )
                                setRequired(input.required())
                                setItemId(input.id())
                                input.options()?.let { opt ->
                                    setOptions(
                                            opt.map {
                                                Option.parseBookingConfirmationInputOptions(it)
                                            }
                                    )
                                }
                                setViewMode(true)
                                setShowChangeButton(
                                        !input.type().equals(QuickBookingType.RETURN_TRIP, true)
                                )
                                setContentDescription(null)
                                onChangeStream = onItemChangeActionStream
                            }
                    )
                }
            }

            if (it.notes().size > 0) {
                result.add(
                        DrtItemViewModel().apply {
                            setIcon(getIconById(null))
                            setLabel("Notes from operator")
                            setType("Notes")
                            setValue(
                                    listOf(String.format("You have %d note", it.notes().size))
                            )
                            setRequired(false)
//                        setItemId(input.id())
                            it.notes()?.let { opt ->
                                setOptions(
                                        opt.map { note ->
                                            Option.parseBookingConfirmationNotes(note)
                                        }
                                )
                            }
                            setViewMode(true)
                            //setContentDescription(context.getString(R.string.tap_to_add_notes))
                            onChangeStream = onItemChangeActionStream
                        }
                )
            }

            items.update(result)

            _confirmationActions.value = it.actions().map { confirmationAction ->
                com.skedgo.tripkit.ui.generic.action_list.Action(
                        confirmationAction.title(),
                        if (confirmationAction.isDestructive) {
                            R.color.tripKitError
                        } else {
                            R.color.colorPrimary
                        },
                        confirmationAction
                )
            }

            val newTickets = mutableListOf<DrtTicketViewModel>()
            it.tickets().forEach {
                /*
                if (it.type != QuickBookingType.LONG_TEXT && it.type != QuickBookingType.RETURN_TRIP && it.options.isNullOrEmpty()) {
                    return@forEach
                }
                */
                newTickets.add(
                        DrtTicketViewModel().apply {
                            setLabel(it.name())
                            setItemId(it.id())
                            setDescription(it.description())
                            setCurrency(it.currency())
                            setPrice(it.price())
                            setValue(it.value() ?: 0)
                            onChangeStream = onTicketChangeActionStream
//                        setTicket(it)
                            setViewMode(true)
                        }
                )
            }

            tickets.update(newTickets)


        }
    }

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
        _inputs.observeForever(inputsObserver)
        _bookingConfirmation.observeForever(bookingConfirmationObserver)
    }

    /*
    fun setTotalTickets(value: Double, currency: String) {
        _totalTickets.value = value
        _labelTicketTotal.value = String.format("%s%s", currency, value.toString())
    }

    fun setNumberTickets(value: Long) {
        _numberTickets.value = value
        _labelTicketTotal.value =
                String.format("%s%s", value.toString(), if (value > 1) "tickets" else "ticket")
    }
    */

    private fun generateDrtTickets(list: List<Ticket>) {

        val result = mutableListOf<DrtTicketViewModel>()

        list.forEach {
            /*
            if (it.type != QuickBookingType.LONG_TEXT && it.type != QuickBookingType.RETURN_TRIP && it.options.isNullOrEmpty()) {
                return@forEach
            }
            */

            result.add(
                    DrtTicketViewModel().apply {
                        setLabel(it.name)
                        setItemId(it.id)
                        setDescription(it.description)
                        setCurrency(it.currency)
                        setPrice(it.price)
                        setValue(it.value ?: 0)
                        onChangeStream = onTicketChangeActionStream
                        setTicket(it)
                        setViewMode(
                                segment.value?.booking?.confirmation?.status()
                                        ?.value() == BookingConfirmationStatusValue.PROCESSING
                        )
                    }
            )
        }

        _tickets.value = list

        tickets.clear()
        tickets.update(result)

        viewModelScope.launch {
            onEmitPaymentDataStream.emit(this@DrtViewModel)
        }
    }

    private fun generateDrtItems(inputs: List<Input>) {

        val result = mutableListOf<DrtItemViewModel>()

        inputs.forEach {
            /*
            if (it.type != QuickBookingType.LONG_TEXT && it.type != QuickBookingType.RETURN_TRIP && it.options.isNullOrEmpty()) {
                return@forEach
            }
            */

            result.add(
                    DrtItemViewModel().apply {
                        setIcon(getIconById(it.id))
                        setLabel(it.title)
                        setType(it.type)
                        setValue(getDefaultValue(it))
                        setDefaultValue(getDefaultValue(it).first())
                        setRequired(it.required)
                        setItemId(it.id)
                        if (it.type == QuickBookingType.NUMBER) {
                            setMinValue(it.minValue)
                            setMaxValue(it.maxValue)
                        }
                        it.options?.let { opt -> setOptions(opt) }
                        setContentDescriptionWithAppendingLabel(
                                if (it.type.equals(QuickBookingType.RETURN_TRIP, true))
                                    resources.getString(R.string.str_return_trip_voice_over)
                                else getDefaultValue(it).firstOrNull()
                        )
                        /*
                        setContentDescription(
                                when {
                                    it.type.equals(QuickBookingType.RETURN_TRIP, true) -> {
                                        resources.getString(R.string.str_return_trip_voice_over)
                                    }
                                    it.type.equals(QuickBookingType.LONG_TEXT, true) -> {
                                        context.getString(R.string.tap_to_add_notes)
                                    }
                                    else -> {
                                        "Tap Change to make selections"
                                    }
                                }
                        )
                        */
                        onChangeStream = onItemChangeActionStream
                    }
            )
        }

        _inputs.value = inputs

        items.clear()
        items.update(result)

    }

    fun updateInputValue(item: DrtItemViewModel) {
        _inputs.updateFields {
            it.value?.let { list ->
                list.filter { input -> input.id == item.itemId.value }.map { input ->
                    if (input.type == QuickBookingType.MULTIPLE_CHOICE) {
                        input.values = item.values.value
                    } else if (input.type == QuickBookingType.RETURN_TRIP) {
                        input.value = item.rawDate.value
                        if (input.value.isNullOrEmpty()) {
                            input.value = item.values.value?.first()
                        }
                    } else {
                        input.value = item.values.value?.firstOrNull()
                    }
                }
            }
        }
    }

    fun updateTicketValue(ticketItem: DrtTicketViewModel) {
        _tickets.updateFields {
            it.value?.let { list ->
                list.filter { ticket -> ticket.id == ticketItem.itemId.value }.map { ticket ->
                    ticket.value = ticketItem.value.value ?: 0L
                }
            }
        }
    }

    private fun getIconById(id: String?): Int {
        return when (id) {
            "mobilityOptions", "totalPassengers" -> R.drawable.ic_person
            "purpose" -> R.drawable.ic_flag
            "notes" -> R.drawable.ic_edit
            "returnTrip" -> R.drawable.ic_tickets
            "wheelchairPassengers" -> R.drawable.ic_wheelchair
            else -> R.drawable.ic_note
        }
    }

    private fun getDefaultValue(input: Input): List<String> {
        val defaultValues = ArrayList<String>()
        val rawValues = ArrayList<String>()

        val hashValues = HashMap<String, String>()
        input.options?.forEach {
            hashValues[it.id] = it.title
        }

        input.values?.let { rawValues.addAll(it) }
        input.value?.let { rawValues.add(it) }

        rawValues.forEach { value ->
            hashValues[value]?.let {
                defaultValues.add(it)
            }
        }

        if (defaultValues.isEmpty()) {
            if (input.type == QuickBookingType.NUMBER) {
                defaultValues.add(input.minValue.toString())
            } else {
                defaultValues.addAll(listOf(getDefaultValueByType(input.type, input.title)))
            }
        } else {
            if (input.type == QuickBookingType.MULTIPLE_CHOICE) {
                input.values = defaultValues
            } else {
                input.value = defaultValues.firstOrNull()
            }
        }

        return defaultValues
    }

    fun getDefaultValueByType(
            @QuickBookingType type: String,
            title: String,
            values: List<String>? = null
    ): String {
        return when (type) {
            QuickBookingType.LONG_TEXT -> {
                String.format(context.getString(R.string.tap_to), title)
            }
            QuickBookingType.NUMBER -> {
                values?.firstOrNull() ?: "0"
            }
            else -> {
                "Tap to make selections"
            }
        }
    }

    fun setTripSegment(segment: TripSegment) {
        _segment.value = segment
        if (segment.booking.quickBookingsUrl.isNullOrEmpty()) {
            segment.trip.temporaryURL?.let { getBookingUpdate(it) }
        } else {
            segment.booking.quickBookingsUrl?.let { fetchQuickBooking(it) }
        }

        updateSegment(segment)

        val confirmation = segment.booking?.confirmation
        confirmation?.let {
            _bookingConfirmation.postValue(it)
            if (it.status().value() != BookingConfirmationStatusValue.PROCESSING) {
                stopPollingUpdate.set(true)
            }
        }
        _accessibilityLabel.value = segment.booking?.accessibilityLabel ?: "Book"
        _isHideExactTimes.value = segment.isHideExactTimes
    }

    private fun fetchQuickBooking(url: String) {
        quickBookingService.getQuickBooking(url)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    _loading.value = true
                }
                .subscribeBy(
                        onError = {
                            it.printStackTrace()
                            _loading.value = false

                            try {
                                val errorString = (it as HttpException).response()?.errorBody()?.string()
                                errorString?.let {
                                    val json = Gson().fromJson(errorString, DRTError::class.java)
                                    _error.value = Pair(null, json.error)
                                }
                            } catch (e: Exception) {
                            }
                        },
                        onSuccess = {
                            it.firstOrNull()?.let {
                                _quickBooking.value = it
                            }
                            _loading.value = false
                        }
                ).autoClear()
    }


    fun book() {
        items.forEach { updateInputValue(it) }
        tickets.forEach { updateTicketValue(it) }
        _quickBooking.value?.let { quickBooking ->
            logAction(quickBooking.bookingURL)
        }
    }

    private fun logAction(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _loading.postValue(true)
            }
            val result = bookingService.logTrip(url)
            if (result !is NetworkResponse.Success) {
                result.logError()
            }
            viewModelScope.launch(Dispatchers.Main) {
                proceedBooking()
                _loading.postValue(false)
            }
        }
    }


    private fun proceedBooking() {
        _inputs.value?.let { inputs ->
            //inputs.singleOrNull { it.id == "mobilityOptions" }?.values = listOf("Guest")
            _tickets.value?.let { tickets ->
                _quickBooking.value?.let { quickBooking ->
                    quickBookingService.quickBook(
                            quickBooking.bookingURL,
                            QuickBookRequest(
                                    inputs.filter {
                                        !it.value.isNullOrEmpty() || !it.values.isNullOrEmpty()
                                    },
                                    tickets.filter {
                                        it.value != null && (it.value ?: 0L) > 0
                                    }
                            )
                    ).observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe {
                                _loading.value = true
                            }
                            .subscribeBy(
                                    onError = {
                                        if (it is HttpException) {
                                            val error: DRTError = Gson().fromJson(
                                                    it.response()?.errorBody()?.string(),
                                                    DRTError::class.java
                                            )
                                            this@DrtViewModel._error.value = Pair(error.title, error.error)
                                        }
                                        _loading.value = false

                                        //getBookingUpdate(quickBooking.tripUpdateURL)
                                    },
                                    onSuccess = { response ->

                                        _loading.value = false
                                        if (response.review != null && response.paymentOptions != null) {
                                            _review.postValue(response.review!!)
                                            _paymentOptions.postValue(response.paymentOptions!!)
                                            _goToPayment.postValue(Unit)
                                        } else {
                                            processBookingResponse(response.refreshURLForSourceObject)
                                        }
                                    }
                            ).autoClear()
                }
            }
        }
    }

    fun processBookingResponse(url: String) {
        getBookingUpdate(url)
    }

    private fun setBookingUpdatePolling(url: String) {
        //For the initial call before the polling
        stopPollingUpdate.set(false)
        Observable.interval(30L, TimeUnit.SECONDS, Schedulers.io())
                .takeWhile { !stopPollingUpdate.get() }
                .flatMapSingle {
                    quickBookingService.getBookingUpdate(url)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { response ->
                                response.processRawData(resources, Gson())
                                val segment = response.tripGroupList.firstOrNull()?.trips
                                        ?.firstOrNull()?.segments?.firstOrNull { it.booking != null }

                                updateSegment(segment)

                                val confirmation = segment?.booking?.confirmation
                                confirmation?.let {
                                    _bookingConfirmation.postValue(it)
                                    if (it.status().value() != BookingConfirmationStatusValue.PROCESSING) {
                                        stopPollingUpdate.set(true)
                                    }
                                }
                            }
                }.subscribeBy(
                        onError = {
                            it.printStackTrace()
                        }
                ).autoClear()
    }

    private fun getBookingUpdate(url: String?) {
        url?.let { it ->
            quickBookingService.getBookingUpdate(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                            onSuccess = { response ->
                                response.processRawData(resources, Gson())
                                val segment = response.tripGroupList.firstOrNull()?.trips
                                        ?.firstOrNull()?.segments?.firstOrNull { it.booking != null }

                                updateSegment(segment)

                                val confirmation = segment?.booking?.confirmation
                                confirmation?.let { bookingConfirmation ->
                                    _bookingConfirmation.postValue(bookingConfirmation)

                                    if (bookingConfirmation.status()
                                                    .value() == BookingConfirmationStatusValue.PROCESSING
                                    ) {
                                        setBookingUpdatePolling(url)
                                    } else {
                                        stopPollingUpdate.set(true)
                                    }
                                }
                            },
                            onError = {
                                it.printStackTrace()
                            }
                    ).autoClear()
        }
    }


    private fun updateSegment(segment: TripSegment?) {
        if (segment?.id == _segment.value?.id) {
            _segment.updateFields {
                it.value?.booking = segment?.booking
            }
        }
    }


    fun processAction(action: BookingConfirmationAction?) {
        action?.let {
            it.internalURL()?.let {
                quickBookingService.executeBookingAction(it)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { _loading.value = true }
                        .subscribeBy(
                                onError = { e ->
                                    e.printStackTrace()
                                    if (e is HttpException) {
                                        val error: DRTError = Gson().fromJson(
                                                e.response()?.errorBody()?.string(),
                                                DRTError::class.java
                                        )
                                        this@DrtViewModel._error.value = Pair(error.title, error.error)
                                    }
                                    _loading.value = false
                                },
                                onSuccess = { response ->
                                    getBookingUpdate(response.refreshURLForSourceObject)
                                    _loading.value = false
                                }
                        ).autoClear()
            }
        }
    }

}