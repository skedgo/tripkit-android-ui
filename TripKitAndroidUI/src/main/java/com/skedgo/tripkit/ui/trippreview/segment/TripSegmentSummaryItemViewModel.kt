package com.skedgo.tripkit.ui.trippreview.segment

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.utils.TapStateFlow

/**
 * Moved from [com.skedgo.tripkit.ui.trippreview.TripPreviewHeaderViewModel]
 * and renamed from [TripPreviewHeaderItemViewModel] to [TripSegmentSummaryItemViewModel]
 * since it'll be generic and will not only be used on the preview header feature
 */
class TripSegmentSummaryItemViewModel : ViewModel() {

    companion object {
        fun parseFromTripSegmentSummary(
            summary: TripSegmentSummary,
            isRightToLeft: Boolean
        ): TripSegmentSummaryItemViewModel =
            TripSegmentSummaryItemViewModel().apply {
                title.set(summary.title)
                subTitle.set(summary.subTitle)
                icon.set(summary.icon)
                id.set(summary.id)
                description.set(summary.description)
                modeId.set(summary.modeId)

                if (summary.modeId != TransportMode.ID_TAXI &&
                    TransportMode.getLocalIconResId(summary.modeId) != 0 ||
                    summary.modeId == "me_car-r"
                ) {
                    isMirrored.set(isRightToLeft)
                }

                contentDescription.set("Title: ${summary.title ?: "N/A"}, Subtitle: ${summary.subTitle ?: "N/A"}, Description: ${summary.description ?: "N/A"}")
            }
    }

    val id = ObservableField<Long>()
    val title = ObservableField<String>()
    val subTitle = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val selected = ObservableBoolean(false)
    val description = ObservableField<String>()
    val modeId = ObservableField<String>()
    val isMirrored = ObservableBoolean(false)
    val contentDescription = ObservableField<String>()

    val itemClick = TapStateFlow.create { this }
}