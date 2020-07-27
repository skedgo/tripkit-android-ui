package com.skedgo.tripkit.ui.trippreview.nearby

import android.graphics.drawable.Drawable
import android.view.View
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import me.tatarka.bindingcollectionadapter2.ItemBinding
import javax.inject.Inject
import androidx.databinding.library.baseAdapters.BR
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.tripresults.LoaderPlaceholder
import com.skedgo.tripkit.ui.tripresults.TripResultTransportItemViewModel
import com.skedgo.tripkit.ui.tripresults.TripResultViewModel
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList
import me.tatarka.bindingcollectionadapter2.itembindings.OnItemBindClass
import timber.log.Timber
import javax.inject.Provider


class NearbyTripPreviewItemListItemViewModel {
    var icon = ObservableInt(0)
    var title = ObservableField<String>("")
    var location = ObservableField<String>("")
    var distance = ObservableField<String>("")
}

class NearbyTripPreviewModeItemViewModel {
    val modeId = ObservableField<String>()
    val modeIconId = ObservableInt(0)
    val checked = ObservableBoolean(true)

    val clicked: PublishRelay<Pair<String, Boolean>> = PublishRelay.create()

    fun onItemClick(view: View) {
        checked.set(!checked.get())
        clicked.accept(modeId.get()!! to checked.get() )
    }
}
class NearbyTripPreviewItemViewModel : RxViewModel() {
    val loadingItem = LoaderPlaceholder()
    val showModes = ObservableBoolean(false)
    var originalItems = listOf<NearbyLocation>()
    var items = ObservableArrayList<NearbyTripPreviewItemListItemViewModel>()
    val binding = ItemBinding.of(
            OnItemBindClass<Any>()
                    .map(NearbyTripPreviewItemListItemViewModel::class.java, com.skedgo.tripkit.ui.BR.viewModel, R.layout.trip_preview_pager_nearby_list_item)
                    .map(LoaderPlaceholder::class.java, ItemBinding.VAR_NONE, R.layout.circular_progress_loader))

    var transportModes = ObservableArrayList<NearbyTripPreviewModeItemViewModel>()
    var transportBinding = ItemBinding.of<NearbyTripPreviewModeItemViewModel>(BR.viewModel, R.layout.trip_preview_pager_nearby_list_mode_transport)
    val mergedList = MergeObservableList<Any>().insertItem(loadingItem).insertList(items)

    fun clearTransportModes() {
        transportModes.clear()
    }

    fun addMode(modeInfo: ModeInfo) {
        val vm = NearbyTripPreviewModeItemViewModel()
        vm.modeId.set(modeInfo.id)
        vm.clicked.observeOn(mainThread())
                .subscribe { loadLocations(true) }
                .autoClear()
        modeInfo.modeCompat?.let {
            vm.modeIconId.set(it.iconRes)
        }
        transportModes.add(vm)
        showModes.set(true)
    }

    fun loadLocations(checkModes: Boolean = false) {
        var enabledModes = transportModes.filter { it.checked.get() }.map{ it.modeId.get()!! }.toSet()
        items.clear()
        originalItems.forEach {
            if (!checkModes || enabledModes.contains(it.modeInfo?.id)) {
                val vm = NearbyTripPreviewItemListItemViewModel()
                vm.title.set(it.title)
                vm.location.set(it.address)
                if (it.modeInfo != null && it.modeInfo.modeCompat != null) {
                    vm.icon.set(it.modeInfo.modeCompat.iconRes)
                }
                items.add(vm)
            }
        }

    }
    fun setLocations(list: List<NearbyLocation>) {
        originalItems = list
        loadLocations()
        mergedList.removeItem(loadingItem)
        showModes.set(true)
    }
}