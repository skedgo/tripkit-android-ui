package com.skedgo.tripkit.ui.trippreview

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.TapStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject
import kotlin.math.abs

class TripPreviewHeaderItemViewModel : ViewModel() {
    val id = ObservableField<Long>()
    val title = ObservableField<String>()
    val subTitle = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val selected = ObservableBoolean(false)
    val description = ObservableField<String>()
    val modeId = ObservableField<String>()
    val isMirrored = ObservableBoolean(false)

    val itemClick = TapStateFlow.create { this }


}

class TripPreviewHeaderViewModel @Inject constructor() : RxViewModel() {

    val items: ObservableArrayList<TripPreviewHeaderItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripPreviewHeaderItemViewModel>(BR.viewModel, R.layout.item_trip_preview_header)

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _showDescription = MutableLiveData(false)
    val showDescription: LiveData<Boolean> = _showDescription

    private val _selectedSegmentId = MutableLiveData<Pair<Long, String>>()
    val selectedSegmentId: LiveData<Pair<Long, String>> = _selectedSegmentId

    private val _isHideExactTimes = MutableLiveData(false)
    val isHideExactTimes: LiveData<Boolean> = _isHideExactTimes

    fun setup(context: Context, headerItems: List<TripPreviewHeader>) {

        val isRightToLeft = context.resources.getBoolean(R.bool.is_right_to_left)

        items.clear()
        items.addAll(
                headerItems.mapIndexed { index, previewHeader ->
                    TripPreviewHeaderItemViewModel().apply {
                        title.set(previewHeader.title)
                        subTitle.set(previewHeader.subTitle)
                        icon.set(previewHeader.icon)
                        id.set(previewHeader.id)
                        description.set(previewHeader.description)
                        modeId.set(previewHeader.modeId)
                        _isHideExactTimes.value = previewHeader.isHideExactTimes

                        if (previewHeader.modeId != TransportMode.ID_TAXI &&
                                TransportMode.getLocalIconResId(previewHeader.modeId) != 0 ||
                                previewHeader.modeId == "me_car-r") {
                            isMirrored.set(isRightToLeft)
                        }

                        if (index == 0) {
                            selected.set(true)
                            previewHeader.description?.let { desc ->
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

    private fun setSelected(item: TripPreviewHeaderItemViewModel) {
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


    private fun checkNearestSegment(segmentId: Long): TripPreviewHeaderItemViewModel? {
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