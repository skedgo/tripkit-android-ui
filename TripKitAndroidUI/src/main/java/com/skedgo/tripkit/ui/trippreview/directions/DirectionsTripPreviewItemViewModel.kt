package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.routing.RoadTag
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.getRoadSafetyColor
import com.skedgo.tripkit.routing.getRoadTagLabel
import com.skedgo.tripkit.routing.getTextColor
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trippreview.GetInstructionIcon
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.tripresult.RoadTagChartItem
import com.skedgo.tripkit.ui.tripresult.TripPreviewStepCustomRecyclerViewAdapter
import com.skedgo.tripkit.ui.tripresult.TripSegmentCustomRecyclerViewAdapter
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import me.tatarka.bindingcollectionadapter2.ItemBinding
import kotlin.Exception
import kotlin.math.roundToInt

class DirectionsTripPreviewItemStepViewModel : ViewModel() {
    var icon = ObservableField<Drawable>()
    var title = ObservableField<String>("")
    var description = ObservableField("")
    var roadTags: List<RoadTag> = emptyList()

    fun generateRoadTagItems(): List<RoadTagChartItem> {
        val result = mutableListOf<RoadTagChartItem>()
        roadTags.forEach { roadTag ->
            result.add(
                RoadTagChartItem(
                    label = roadTag.getRoadTagLabel(),
                    length = 0,
                    color = roadTag.getRoadSafetyColor(),
                    textColor = roadTag.getTextColor()
                )
            )
        }
        return result
    }
}

class DirectionsTripPreviewItemViewModel : TripPreviewPagerItemViewModel() {

    val customAdapter = TripPreviewStepCustomRecyclerViewAdapter<DirectionsTripPreviewItemStepViewModel>()

    var items = ObservableArrayList<DirectionsTripPreviewItemStepViewModel>()
    var itemBinding = ItemBinding.of<DirectionsTripPreviewItemStepViewModel>(
        BR.viewModel,
        R.layout.trip_preview_step
    )

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
        items.clear()
        if (segment.turnByTurn != null) {
            showLaunchInMaps.set(true)
        }

        val iconGetter = GetInstructionIcon()

        segment.streets?.forEach {
            val vm = DirectionsTripPreviewItemStepViewModel()
            vm.title.set(DistanceFormatter.format(it.metres().roundToInt()))
            if (it.name().isNullOrBlank()) {
                vm.description.set(context.getString(R.string.along_unnamed_street))
            } else {
                vm.description.set(context.getString(R.string.along__pattern, it.name()))
            }
            vm.icon.set(iconGetter.getIcon(context, it))
            if (!items.any { it.title == vm.title && it.description == vm.description }) {
                items.add(vm)
            }
            vm.roadTags = it.roadTags()?.map {
                try {
                    RoadTag.valueOf(it.replace("-","_"))
                } catch (e: Exception) {
                    RoadTag.UNKNOWN
                }
            } ?: emptyList()
        }
    }
}

