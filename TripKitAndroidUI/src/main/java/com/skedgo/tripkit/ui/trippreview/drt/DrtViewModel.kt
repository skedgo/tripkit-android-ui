package com.skedgo.tripkit.ui.trippreview.drt

import android.content.res.Resources
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


class DrtViewModel @Inject constructor(
        private val quickBookingService: QuickBookingService,
        private val resources: Resources,
        private val bookingService: BookingV2TrackingService
) : RxViewModel() {

    private val _quickBooking = MutableLiveData<QuickBooking>()

    private val _inputs = MutableLiveData<List<Input>>()

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _areInputsValid = MutableLiveData<Boolean>()
    val areInputsValid: LiveData<Boolean> = _areInputsValid

    private val _error = MutableLiveData<Pair<String?, String?>>()
    val error: LiveData<Pair<String?, String?>> = _error

    val onItemChangeActionStream = MutableSharedFlow<DrtItemViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemViewModel.diffCallback())
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    private val _bookingConfirmation = MutableLiveData<BookingConfirmation>()
    val bookingConfirmation: LiveData<BookingConfirmation> = _bookingConfirmation

    private val _confirmationActions = MutableLiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>>()
    val confirmationActions: LiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>> = _confirmationActions

    private val _accessibilityLabel = MutableLiveData<String>()
    val accessibilityLabel: LiveData<String> = _accessibilityLabel

    private val _isHideExactTimes = MutableLiveData<Boolean>()
    val isHideExactTimes: LiveData<Boolean> = _isHideExactTimes

    private val stopPollingUpdate = AtomicBoolean()

    private val quickBookingObserver = Observer<QuickBooking> {
        generateDrtItems(it.input)
        getBookingUpdate(it.tripUpdateURL)
    }

    private val inputsObserver = Observer<List<Input>> { inputs ->
        _areInputsValid.value = inputs.none {
            it.required &&
                    ((it.type == QuickBookingType.MULTIPLE_CHOICE && !it.options.isNullOrEmpty() && it.values.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.SINGLE_CHOICE && !it.options.isNullOrEmpty() && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.LONG_TEXT && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.RETURN_TRIP && it.value.isNullOrEmpty()))
        }
    }

    private val bookingConfirmationObserver = Observer<BookingConfirmation> {
        it?.let {
            val result = mutableListOf<DrtItemViewModel>()
            it.input().forEach { input ->
                if (!input.value().isNullOrEmpty() || !input.values().isNullOrEmpty()) {
                    result.add(
                            DrtItemViewModel().apply {
                                setIcon(getIconById(input.id()))
                                setLabel(input.title())
                                setType(input.type())
                                setValue(
                                        if (input.type().equals(QuickBookingType.RETURN_TRIP, true)) {
                                            val segmentTz = DateTimeZone.forID(segment.value?.timeZone
                                                    ?: "UTC")
                                            val rawTz = DateTimeZone.forID("UTC")

                                            try {
                                                val dateTime = DateTime.parse(input.value()?.replace("Z", ""), getISODateFormatter(rawTz))
                                                val dateString = dateTime.toString(getDisplayDateFormatter(segmentTz))
                                                val timeString = dateTime.toString(getDisplayTimeFormatter(segmentTz))
                                                listOf("$dateString at $timeString")
                                            } catch (e: Exception) {
                                                listOf("One-way only")
                                            }
                                        } else {
                                            if (!input.value().isNullOrEmpty()) {
                                                listOf(input.value() ?: "")
                                            } else {
                                                input.values() ?: emptyList()
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
                                setShowChangeButton(!input.type().equals(QuickBookingType.RETURN_TRIP, true))
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
        }
    }

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
        _inputs.observeForever(inputsObserver)
        _bookingConfirmation.observeForever(bookingConfirmationObserver)
    }

    private fun generateDrtItems(inputs: List<Input>) {

        val result = mutableListOf<DrtItemViewModel>()

        inputs.forEach {
            if (it.type != QuickBookingType.LONG_TEXT && it.type != QuickBookingType.RETURN_TRIP && it.options.isNullOrEmpty()) {
                return@forEach
            }

            result.add(
                    DrtItemViewModel().apply {
                        setIcon(getIconById(it.id))
                        setLabel(it.title)
                        setType(it.type)
                        setValue(getDefaultValue(it))
                        setRequired(it.required)
                        setItemId(it.id)
                        it.options?.let { opt -> setOptions(opt) }
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

    private fun getIconById(id: String?): Int {
        return when (id) {
            "mobilityOptions" -> R.drawable.ic_person
            "purpose" -> R.drawable.ic_flag
            "notes" -> R.drawable.ic_edit
            "returnTrip" -> R.drawable.ic_tickets
            else -> R.drawable.ic_note
        }
    }

    private fun getDefaultValue(input: Input): List<String> {
        val defaultValues = ArrayList<String>()
        val rawValues = ArrayList<String>()
        input.values?.let { rawValues.addAll(it) }
        input.value?.let { rawValues.add(it) }

        rawValues.forEach { value ->
            if (value.isNotEmpty()) {
                defaultValues.add(String.format("%s%s", value.substring(0, 1).capitalize(Locale.getDefault()),
                        value.substring(1, value.length)))
            }
        }

        if (defaultValues.isEmpty()) {
            defaultValues.addAll(listOf(getDefaultValueByType(input.type, input.title)))
        }

        return defaultValues
    }

    fun getDefaultValueByType(@QuickBookingType type: String, title: String): String {
        return if (type == QuickBookingType.LONG_TEXT) {
            "Tap to $title"
        } else {
            "Tap Change to make selections"
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
        _isHideExactTimes.value = segment.trip.isHideExactTimes
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
        _quickBooking.value?.let { quickBooking ->
            logAction(quickBooking.bookingURL)
        }
    }

    private fun logAction(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = bookingService.logTrip(url)
            if (result !is NetworkResponse.Success) {
                result.logError()
            }
            viewModelScope.launch(Dispatchers.Main) {
                proceedBooking()
            }
        }
    }


    private fun proceedBooking() {
        _inputs.value?.let { inputs ->
            //inputs.singleOrNull { it.id == "mobilityOptions" }?.values = listOf("Guest")
            _quickBooking.value?.let { quickBooking ->
                quickBookingService.quickBook(
                        quickBooking.bookingURL,
                        QuickBookRequest(
                                inputs.filter {
                                    !it.value.isNullOrEmpty() || !it.values.isNullOrEmpty()
                                }
                        )
                ).observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            _loading.value = true
                        }
                        .subscribeBy(
                                onError = {
                                    if (it is HttpException) {
                                        val error: DRTError = Gson().fromJson(it.response()?.errorBody()?.string(), DRTError::class.java)
                                        this@DrtViewModel._error.value = Pair(error.title, error.error)
                                    }
                                    _loading.value = false
                                },
                                onSuccess = {
                                    _loading.value = false
                                    processBookingResponse(it)
                                }
                        ).autoClear()
            }
        }
    }

    private fun processBookingResponse(response: QuickBookResponse) {
        getBookingUpdate(response.refreshURLForSourceObject)
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

    private fun getBookingUpdate(url: String) {
        quickBookingService.getBookingUpdate(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = { response ->
                            response.processRawData(resources, Gson())
                            val segment = response.tripGroupList.firstOrNull()?.trips
                                    ?.firstOrNull()?.segments?.firstOrNull { it.booking != null }

                            updateSegment(segment)

                            val confirmation = segment?.booking?.confirmation
                            confirmation?.let {
                                _bookingConfirmation.postValue(it)

                                if (it.status().value() == BookingConfirmationStatusValue.PROCESSING) {
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