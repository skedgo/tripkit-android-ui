package com.skedgo.tripkit.ui.trippreview.directions

import android.content.Context
import android.graphics.drawable.Drawable
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.library.baseAdapters.BR
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.trippreview.GetInstructionIcon
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import me.tatarka.bindingcollectionadapter2.ItemBinding
import java.util.*
import kotlin.math.roundToInt

class DirectionsTripPreviewItemStepViewModel : ViewModel() {
    var icon = ObservableField<Drawable>()
    var title = ObservableField<String>("")
    var description = ObservableField("")
}

class DirectionsTripPreviewItemViewModel : TripPreviewPagerItemViewModel() {
    var items = ObservableArrayList<DirectionsTripPreviewItemStepViewModel>()
    var itemBinding = ItemBinding.of<DirectionsTripPreviewItemStepViewModel>(BR.viewModel, R.layout.trip_preview_step)

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
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
            if(!items.any { it.title == vm.title && it.description == vm.description }){
                items.add(vm)
            }
        }
    }
}

