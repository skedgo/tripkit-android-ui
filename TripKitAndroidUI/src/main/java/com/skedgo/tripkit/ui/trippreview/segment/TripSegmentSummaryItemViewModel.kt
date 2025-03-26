package com.skedgo.tripkit.ui.trippreview.segment

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
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
                title.value = summary.title.orEmpty()
                subTitle.value = summary.subTitle.orEmpty()
                icon.value = summary.icon
                id.value = summary.id
                description.value = summary.description
                modeId.value = summary.modeId

                if (summary.modeId != TransportMode.ID_TAXI &&
                    TransportMode.getLocalIconResId(summary.modeId) != 0 ||
                    summary.modeId == "me_car-r"
                ) {
                    isMirrored.value = isRightToLeft
                }
            }
    }

    val id = MutableLiveData<Long?>()
    val title = MutableLiveData<String>()
    val subTitle = MutableLiveData<String>()
    val icon = MutableLiveData<Drawable?>()
    val selected = MutableLiveData(false)
    val description = MutableLiveData<String?>()
    val modeId = MutableLiveData<String?>()
    val isMirrored = MutableLiveData(false)

    val itemClick = TapStateFlow.create { this }
}