package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.booking.quickbooking.Input
import com.skedgo.tripkit.booking.quickbooking.QuickBooking
import com.skedgo.tripkit.booking.quickbooking.QuickBookingService
import com.skedgo.tripkit.booking.quickbooking.QuickBookingType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.addTo
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

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _bookingInProgress = MutableLiveData<Boolean>()
    val bookingInProgress: LiveData<Boolean> = _bookingInProgress

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

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

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
    }

    private fun generateDrtItems(input: List<Input>) {

        val result = mutableListOf<DrtItemViewModel>()

        input.forEach {
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

        items.clear()
        items.update(result)

    }

    private fun getIconById(id: String): Int {
        return when (id) {
            "mobilityOptions" -> {
                R.drawable.ic_person
            }
            "purpose" -> {
                R.drawable.ic_flag
            }
            "notes" -> {
                R.drawable.ic_edit
            }
            else -> {
                R.drawable.ic_car
            }
        }
    }

    fun getDefaultValueByType(@QuickBookingType type: String, title: String): String {
        return if (type == QuickBookingType.LONG_TEXT) {
            "Tap to $title"
        } else {
            "Tap Change to make selections"
        }
    }

    /* Using static values
    private fun getDrtItems() {
        val result = mutableListOf<DrtItemViewModel>()

        generateDrtItem(DrtItem.MOBILITY_OPTIONS)?.let { result.add(it) }
        generateDrtItem(DrtItem.PURPOSE)?.let { result.add(it) }
        generateDrtItem(DrtItem.ADD_NOTE)?.let { result.add(it) }

        items.update(result)
    }

    private fun generateDrtItem(@DrtItem item: String): DrtItemViewModel? {

        return when (item) {
            DrtItem.MOBILITY_OPTIONS -> DrtItemViewModel().apply {
                setIcon(R.drawable.ic_person)
                setLabel(item)
                setValue(listOf("Tap Change to make selections"))
                setRequired(true)
                onChangeStream = onItemChangeActionStream
            }
            DrtItem.PURPOSE -> DrtItemViewModel().apply {
                setIcon(R.drawable.ic_flag)
                setLabel(item)
                setValue(listOf("Tap Change to make selections"))
                setRequired(true)
                onChangeStream = onItemChangeActionStream
            }
            DrtItem.ADD_NOTE -> DrtItemViewModel().apply {
                setIcon(R.drawable.ic_edit)
                setLabel(item)
                setValue(listOf("Tap Change to add notes"))
                setRequired(false)
                onChangeStream = onItemChangeActionStream
            }
            else -> null
        }
    }
    */

    fun setTripSegment(segment: TripSegment) {
        _segment.value = segment
        segment.booking.quickBookingsUrl?.let {
            fetchQuickBooking(it)
        }
    }

    fun setBookingInProgress(value: Boolean) {
        _bookingInProgress.value = value

        items.forEach {
            it.setViewMode(value)
        }
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
}