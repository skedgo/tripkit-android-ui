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
    val id = ObservableField<String>()
    val title = ObservableField<String>()
    val subTitle = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val selected = ObservableBoolean(false)

    val itemClick = TapStateFlow.create { this }
}

class TripPreviewHeaderViewModel @Inject constructor() : RxViewModel() {

    val items: ObservableArrayList<TripPreviewHeaderItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripPreviewHeaderItemViewModel>(BR.viewModel, R.layout.item_trip_preview_header)

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _showDescription = MutableLiveData<Boolean>()
    val showDescription: LiveData<Boolean> = _showDescription


    fun setup(headerItems: List<TripPreviewHeader>) {
        items.clear()
        items.addAll(
                headerItems.map {
                    TripPreviewHeaderItemViewModel().apply {
                        title.set(it.title)
                        subTitle.set(it.subTitle)
                        icon.set(it.icon)
                        id.set(it.id)

                        itemClick.observable.onEach {
                            it.selected.set(true)
                            items.filter { item -> item.selected.get() && item.id.get() != it.id.get() }
                                    .forEach { it.selected.set(false) }
                        }.launchIn(viewModelScope)
                    }
                }
        )
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setShowDescription(value: Boolean) {
        _showDescription.value = value
    }

}