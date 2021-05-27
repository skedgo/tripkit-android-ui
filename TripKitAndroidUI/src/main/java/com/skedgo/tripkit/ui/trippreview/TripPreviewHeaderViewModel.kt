package com.skedgo.tripkit.ui.trippreview

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.TapStateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject

class TripPreviewHeaderItemViewModel : ViewModel() {
    val id = ObservableField<Long>()
    val title = ObservableField<String>()
    val subTitle = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val selected = ObservableBoolean(false)
    val description = ObservableField<String>()

    val itemClick = TapStateFlow.create { this }
}

class TripPreviewHeaderViewModel @Inject constructor() : RxViewModel() {

    val items: ObservableArrayList<TripPreviewHeaderItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripPreviewHeaderItemViewModel>(BR.viewModel, R.layout.item_trip_preview_header)

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _showDescription = MutableLiveData(false)
    val showDescription: LiveData<Boolean> = _showDescription

    private val _selectedSegmentId = MutableLiveData<Long>()
    val selectedSegmentId: LiveData<Long> = _selectedSegmentId

    fun setup(headerItems: List<TripPreviewHeader>) {
        items.clear()
        items.addAll(
                headerItems.mapIndexed { index, previewHeader ->
                    TripPreviewHeaderItemViewModel().apply {
                        title.set(previewHeader.title)
                        subTitle.set(previewHeader.subTitle)
                        icon.set(previewHeader.icon)
                        id.set(previewHeader.id)
                        description.set(previewHeader.description)

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
                                _selectedSegmentId.value = this
                            }
                        }.launchIn(viewModelScope)
                    }
                }
        )
    }

    fun setSelectedById(segmentId: Long) {
        items.firstOrNull { it.id.get() == segmentId }?.apply {
            selected.set(true)

            items.filter { item -> item.id.get() != segmentId }.map { otherItem ->
                otherItem.selected.set(false)
            }

            description.get()?.let { desc ->
                _description.value = desc
                _showDescription.value = true
            } ?: kotlin.run { _showDescription.value = false }

        }
    }
}