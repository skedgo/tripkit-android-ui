package com.skedgo.tripkit.ui.trippreview.external

import android.content.Context
import android.webkit.URLUtil
import androidx.databinding.ObservableArrayList
import com.skedgo.tripkit.common.model.booking.Booking
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import me.tatarka.bindingcollectionadapter2.ItemBinding
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trippreview.handleExternalAction

class ExternalActionTripPreviewItemViewModel : TripPreviewPagerItemViewModel() {
    val items = ObservableArrayList<ExternalActionViewModel>()

    val binding = ItemBinding.of<ExternalActionViewModel>(
        BR.viewModel,
        R.layout.trip_preview_external_action_pager_list_item
    )
        .bindExtra(BR.parentViewModel, this)

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
        items.clear()
        segment.booking?.externalActions?.forEach {
            generateTitle(context, it, segment.booking)?.let { title ->
                val vm = ExternalActionViewModel()
                vm.title.set(title)
                vm.action = it
                vm.externalAction = context.handleExternalAction(it)
                items.add(vm)
            }
        }
    }

    private fun generateTitle(context: Context, action: String, booking: Booking): String? {

        return if (booking.externalActions?.size ?: 0 > 1) {
            when {
                action == "gocatch" -> {
                    context.getString(R.string.gocatch_a_taxi)
                }
                action == "ingogo" -> {
                    context.getString(R.string.action_get_ingogo)
                }
                action == "mtaxi" -> {
                    null
                }
                action.startsWith("tel:") -> {
                    context.getString(R.string.action_call_taxis)
                }
                URLUtil.isNetworkUrl(action) -> {
                    context.getString(R.string.show_website)
                }
                else -> {
                    action
                }
            }
        } else {
            var label = booking.title
            /*
            if (!booking.accessibilityLabel.isNullOrEmpty()) {
                label = booking.accessibilityLabel
            }
            */
            label
        }

    }
}