package com.skedgo.tripkit.ui.trippreview.service

import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.core.RxViewModel

class ServiceTripActionViewModel : RxViewModel() {
    var title = ObservableField<String>()
    var action = ""
}