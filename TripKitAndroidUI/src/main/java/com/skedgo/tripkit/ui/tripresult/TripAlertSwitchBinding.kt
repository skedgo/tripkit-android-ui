package com.skedgo.tripkit.ui.tripresult

import androidx.appcompat.widget.SwitchCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("tripAlertChecked", "tripAlertOwner")
internal fun bindTripAlertChecked(
    view: SwitchCompat,
    checked: Boolean?,
    owner: TripSegmentGetOffAlertsViewModel?
) {
    // Rendering persisted state is not an activation request. Keep the checked listener
    // for both user taps and thumb drags, which do not dispatch a click.
    view.setOnCheckedChangeListener(null)
    view.isChecked = checked == true
    view.setOnCheckedChangeListener { _, value -> owner?.setAlertState(view.context, value) }
}
