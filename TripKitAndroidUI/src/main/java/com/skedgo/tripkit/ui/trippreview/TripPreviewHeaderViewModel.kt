package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import androidx.databinding.ObservableArrayList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummary
import com.skedgo.tripkit.ui.trippreview.segment.TripSegmentSummaryItemViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject

class TripPreviewHeaderViewModel @Inject constructor() : RxViewModel() {

    val items: ObservableArrayList<TripSegmentSummaryItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripSegmentSummaryItemViewModel>(BR.viewModel, R.layout.item_trip_segment_summary)

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _showDescription = MutableLiveData(false)
    val showDescription: LiveData<Boolean> = _showDescription

    private val _selectedSegmentId = MutableLiveData<Pair<Long, String>>()
    val selectedSegmentId: LiveData<Pair<Long, String>> = _selectedSegmentId

    private val _isHideExactTimes = MutableLiveData(false)
    val isHideExactTimes: LiveData<Boolean> = _isHideExactTimes

    fun setHideExactTimes(value: Boolean){
        _isHideExactTimes.value = value
    }

    fun setup(context: Context, segmentSummaryItems: List<TripSegmentSummary>) {

        val isRightToLeft = context.resources.getBoolean(R.bool.is_right_to_left)

        items.clear()
        items.addAll(
                segmentSummaryItems.mapIndexed { index, segmentSummary ->
                    TripSegmentSummaryItemViewModel.parseFromTripSegmentSummary(
                        segmentSummary, isRightToLeft
                    ).apply {
                        if (index == 0) {
                            selected.set(true)
                            segmentSummary.description?.let { desc ->
                                _description.value = desc
                                _showDescription.value = true
                            }
                        }

                        itemClick.observable.onEach {
                            it.id.get()?.apply {
                                setSelectedById(this)
                                _selectedSegmentId.value = Pair(this, it.modeId.get().toString())
                            }
                        }.launchIn(viewModelScope)
                    }
                }
        )
    }

    fun setSelectedById(segmentId: Long, modeId: String? = null) {
        items.firstOrNull { it.id.get() == segmentId }?.apply {
            selected.set(true)

            items.filter { item -> item.id.get() != segmentId }.map { otherItem ->
                otherItem.selected.set(false)
            }

            description.get()?.let { desc ->
                _description.value = desc
                _showDescription.value = true
            } ?: kotlin.run { _showDescription.value = false }

        } ?: kotlin.run {
            val mode = if (modeId == "null" || modeId == TransportMode.ID_WALK) {
                null
            } else {
                modeId
            }
            mode?.let {
                when (it) {
                    getNextPreviousItemModeId(true) -> {
                        items[
                                items.indexOfFirst { selectedItem ->
                                    selectedItem.selected.get()
                                } + 1
                        ]
                    }
                    getNextPreviousItemModeId(false) -> {
                        items[
                                items.indexOfFirst { selectedItem ->
                                    selectedItem.selected.get()
                                } - 1
                        ]
                    }
                    else -> {
                        checkNearestSegment(segmentId)
                    }
                }?.apply {
                    setSelected(this)
                }
            } ?: kotlin.run {
                checkNearestSegment(segmentId)?.apply {
                    setSelected(this)
                }
            }
        }
    }

    private fun setSelected(item: TripSegmentSummaryItemViewModel) {
        with(item) {
            selected.set(true)

            items.filter { item -> item.id.get() != this.id.get() }.map { otherItem ->
                otherItem.selected.set(false)
            }

            description.get()?.let { desc ->
                _description.value = desc
                _showDescription.value = true
            } ?: kotlin.run { _showDescription.value = false }
        }
    }

    private fun getNextPreviousItemModeId(isNext: Boolean): String? {
        val selectedIndex = items.indexOfFirst { it.selected.get() }
        return if (isNext) {
            if ((selectedIndex + 1) < items.size) {
                items[selectedIndex + 1].modeId.get()
            } else {
                null
            }
        } else {
            if ((selectedIndex - 1) >= 0) {
                items[selectedIndex - 1].modeId.get()
            } else {
                null
            }
        }
    }


    private fun checkNearestSegment(segmentId: Long): TripSegmentSummaryItemViewModel? {
        val segmentGreater = items.filter {
            it.id.get() != null && it.id.get()!! > segmentId
        }.minByOrNull { it.id.get()!! }

        val segmentLower = items.filter {
            it.id.get() != null && it.id.get()!! < segmentId
        }.maxByOrNull { it.id.get()!! }

        return when {
            segmentGreater != null && segmentLower != null -> {
                if (segmentLower.modeId.get() != TransportMode.ID_WALK) {
                    segmentLower
                } else {
                    segmentGreater
                }
            }
            segmentGreater != null && segmentGreater.modeId.get() != TransportMode.ID_WALK -> {
                segmentGreater
            }
            segmentLower != null && segmentLower.modeId.get() != TransportMode.ID_WALK -> {
                segmentLower
            }
            else -> {
                null
            }
        }
    }
}