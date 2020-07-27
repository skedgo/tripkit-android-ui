package com.skedgo.tripkit.ui.trippreview.nearby

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.ui.core.RxViewModel


class InfoGroupViewModel : ViewModel() {
    var icon = ObservableField<Int>()
    var title = ObservableField<Int>()
    var value = ObservableField("")
}