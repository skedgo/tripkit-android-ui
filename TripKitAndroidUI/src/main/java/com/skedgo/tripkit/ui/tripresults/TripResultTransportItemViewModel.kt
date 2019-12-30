package com.skedgo.tripkit.ui.tripresults

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject


class TripResultTransportItemViewModel  @Inject constructor(): RxViewModel() {
    val modeId = ObservableField<String>()
    val modeIconId = ObservableField<String>()
    val checked = ObservableBoolean(false)

    val clicked: PublishRelay<Pair<String, Boolean>> = PublishRelay.create()

    fun onItemClick(view: View) {
        checked.set(!checked.get())
        clicked.accept(modeId.get()!! to checked.get() )
    }

    fun setup(mode: TransportMode) {
        modeId.set(mode.id)
        modeIconId.set(mode.iconId)
    }
}