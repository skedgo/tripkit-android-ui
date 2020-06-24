package com.skedgo.tripkit.ui.trippreview.external

import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.core.RxViewModel


class ExternalActionViewModel : RxViewModel() {
    var title = ObservableField<String>()
    var action = ""
}