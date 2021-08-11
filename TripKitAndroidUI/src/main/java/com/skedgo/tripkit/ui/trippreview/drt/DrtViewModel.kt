package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import javax.inject.Inject

class DrtViewModel @Inject constructor() : RxViewModel() {

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _bookingInProgress = MutableLiveData<Boolean>()
    val bookingInProgress: LiveData<Boolean> = _bookingInProgress

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

    init {
        getDrtItems()
    }


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

    fun setBookingInProgress(value: Boolean){
        _bookingInProgress.value = value

        items.forEach {
            it.setViewMode(value)
        }
    }
}