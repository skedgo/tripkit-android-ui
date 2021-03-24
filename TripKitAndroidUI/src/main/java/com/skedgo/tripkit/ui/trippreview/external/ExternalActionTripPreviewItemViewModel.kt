package com.skedgo.tripkit.ui.trippreview.external

import android.content.Context
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import me.tatarka.bindingcollectionadapter2.ItemBinding
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.utils.TapAction

class ExternalActionTripPreviewItemViewModel : TripPreviewPagerItemViewModel() {
    val items = ObservableArrayList<ExternalActionViewModel>()
    val actionChosen = PublishRelay.create<String>()
    val binding = ItemBinding.of<ExternalActionViewModel>(BR.viewModel, R.layout.trip_preview_external_action_pager_list_item)
            .bindExtra(BR.parentViewModel, this)
    val enableButton = ObservableBoolean(true)

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
        segment.booking?.externalActions?.forEach {
            val vm = ExternalActionViewModel()
            vm.title.set(segment.booking.title)
            vm.action = it
            items.add(vm)
        }
    }
}