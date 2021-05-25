package com.skedgo.tripkit.ui.trippreview

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject

class TripPreviewHeaderItemViewModel : ViewModel() {
    val title = ObservableField<String>()
    val subTitle = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val selected = ObservableBoolean(false)
}

class TripPreviewHeaderViewModel @Inject constructor() : RxViewModel() {

    val items: ObservableArrayList<TripPreviewHeaderItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripPreviewHeaderItemViewModel>(BR.viewModel, R.layout.item_trip_preview_header)

    fun setup(headerItems: List<TripPreviewHeader>) {
        items.addAll(
                headerItems.map {
                    TripPreviewHeaderItemViewModel().apply {
                        title.set(it.title)
                        subTitle.set(it.subTitle)
                        icon.set(it.icon)
                    }
                }
        )
    }
}