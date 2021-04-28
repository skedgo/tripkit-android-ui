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
import com.skedgo.tripkit.ui.trippreview.Action
import com.skedgo.tripkit.ui.trippreview.handleExternalAction
import com.skedgo.tripkit.ui.utils.TapAction

class ExternalActionTripPreviewItemViewModel : TripPreviewPagerItemViewModel() {
    val items = ObservableArrayList<ExternalActionViewModel>()

    val binding = ItemBinding.of<ExternalActionViewModel>(BR.viewModel, R.layout.trip_preview_external_action_pager_list_item)
            .bindExtra(BR.parentViewModel, this)

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
        items.clear()
        segment.booking?.externalActions?.forEach {
            val vm = ExternalActionViewModel()
            vm.title.set(segment.booking.title)
            vm.action = it
            vm.externalAction = context.handleExternalAction(it)
            items.add(vm)
        }
    }
}