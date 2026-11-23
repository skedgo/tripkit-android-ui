package com.skedgo.tripkit.ui.tripresults

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject


class TripResultTransportItemViewModel @Inject constructor() : RxViewModel() {
    val modeId = MutableLiveData<String>()
    val modeIconId = MutableLiveData<String?>()

    /** Human-readable mode name, used as the toggle's accessibility label. */
    val modeTitle = MutableLiveData<String?>()
    val checked = MutableLiveData(false)

    val clicked: PublishRelay<Pair<String, Boolean>> = PublishRelay.create()

    fun onItemClick(view: View) {
        checked.value = !(checked.value ?: false)
        clicked.accept(modeId.value!! to (checked.value ?: false))
    }

    fun setup(mode: TransportMode) {
        modeId.value = mode.id
        modeIconId.value = mode.iconId
        modeTitle.value = mode.title ?: mode.id
    }
}