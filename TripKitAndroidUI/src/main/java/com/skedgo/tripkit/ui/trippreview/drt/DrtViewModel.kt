package com.skedgo.tripkit.ui.trippreview.drt

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.google.gson.Gson
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.quickbooking.*
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.utils.updateFields
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.flow.MutableSharedFlow
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import javax.inject.Inject

class DrtViewModel @Inject constructor(
        private val quickBookingService: QuickBookingService
) : RxViewModel() {

    private val _quickBooking = MutableLiveData<QuickBooking>()
    val quickBooking: LiveData<QuickBooking> = _quickBooking

    private val _inputs = MutableLiveData<List<Input>>()
    val inputs: LiveData<List<Input>> = _inputs

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _areInputsValid = MutableLiveData<Boolean>()
    val areInputsValid: LiveData<Boolean> = _areInputsValid

    private val _bookingResponse = MutableLiveData<QuickBookResponse>()
    val bookingResponse: LiveData<QuickBookResponse> = _bookingResponse

    val onItemChangeActionStream = MutableSharedFlow<DrtItemViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemsDiffCallBack)
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    object DrtItemsDiffCallBack : DiffUtil.ItemCallback<DrtItemViewModel>() {
        override fun areItemsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label == newItem.label

        override fun areContentsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label.value == newItem.label.value
                        && oldItem.values.value == newItem.values.value
    }

    private val quickBookingObserver = Observer<QuickBooking> {
        if (it.input.isNotEmpty()) {
            generateDrtItems(it.input)
        }
    }

    private val inputsObserver = Observer<List<Input>> { inputs ->
        _areInputsValid.value = inputs.none {
            it.required &&
                    (
                            (it.type == QuickBookingType.MULTIPLE_CHOICE && !it.options.isNullOrEmpty() && it.values.isNullOrEmpty()) ||
                                    (it.type == QuickBookingType.SINGLE_CHOICE && !it.options.isNullOrEmpty() && it.value.isNullOrEmpty()) ||
                                    (it.type == QuickBookingType.LONG_TEXT && it.value.isNullOrEmpty())
                            )

        }
    }

    private val bookingResponseObserver = Observer<QuickBookResponse> { response ->
        items.forEach { it.setViewMode(response != null) }
    }

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
        _inputs.observeForever(inputsObserver)
        _bookingResponse.observeForever(bookingResponseObserver)
    }

    private fun generateDrtItems(inputs: List<Input>) {

        val result = mutableListOf<DrtItemViewModel>()

        inputs.forEach {
            if (it.type != QuickBookingType.LONG_TEXT && it.options.isNullOrEmpty()) {
                return@forEach
            }
            result.add(
                    DrtItemViewModel().apply {
                        setIcon(getIconById(it.id))
                        setLabel(it.title)
                        setType(it.type)
                        setValue(listOf(getDefaultValueByType(it.type, it.title)))
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
                    } else {
                        input.value = item.values.value?.firstOrNull()
                    }
                }
            }
        }
    }

    private fun getIconById(id: String): Int {
        return when (id) {
            "mobilityOptions" -> R.drawable.ic_person
            "purpose" -> R.drawable.ic_flag
            "notes" -> R.drawable.ic_edit
            else -> R.drawable.ic_car
        }
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
        segment.booking.quickBookingsUrl?.let { fetchQuickBooking(it) }
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
        _inputs.value?.let { inputs ->
            inputs.singleOrNull { it.id == "mobilityOptions" }?.values = listOf("Guest")
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
                                    it.printStackTrace()
                                    _loading.value = false
                                },
                                onSuccess = {
                                    _loading.value = false
                                    _bookingResponse.value = it
                                }
                        ).autoClear()
            }
        }
    }
}