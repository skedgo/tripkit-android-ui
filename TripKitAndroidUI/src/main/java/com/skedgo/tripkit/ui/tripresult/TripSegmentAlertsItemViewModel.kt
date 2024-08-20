package com.skedgo.tripkit.ui.tripresult

import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.core.RxViewModel

class TripSegmentAlertsItemViewModel : RxViewModel() {
    var titleText = ObservableField<String>()
    var descriptionText = ObservableField<String>()
}