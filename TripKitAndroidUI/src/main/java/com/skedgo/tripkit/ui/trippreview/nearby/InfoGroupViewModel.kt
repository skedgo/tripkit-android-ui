package com.skedgo.tripkit.ui.trippreview.nearby

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel


class InfoGroupViewModel : ViewModel() {
    var icon = ObservableField<Int>()
    var title = ObservableField<Int>()
    var value = ObservableField("")
}