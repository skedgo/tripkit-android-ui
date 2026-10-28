package com.skedgo.tripkit.ui.utils

import android.content.Context
import com.skedgo.tripkit.DefaultTripPreferences
import com.skedgo.tripkit.data.util.onChanged
import com.skedgo.tripkit.ui.R
import io.reactivex.Observable

/** Repair an untouched legacy default without replacing a saved information preference. */
internal fun wheelchairInformationPreferences(context: Context): DefaultTripPreferences {
    val preferences = context.getSharedPreferences("TripPreferences", Context.MODE_PRIVATE)
    return DefaultTripPreferences(preferences).also {
        if (!preferences.contains("isWheelchairPreferred") && context.isWheelchairModeSelected()) {
            it.setWheelchairPreferred(true)
        }
    }
}

/** Service icon policy with initialization of an untouched legacy information preference. */
internal class ServiceAccessibility(context: Context) {
    private val tripPreferences = wheelchairInformationPreferences(context)
    private val transportPreferences = TransportModeSharedPreference(context)
    private val modePreferences = context.getSharedPreferences(PREF_KEY_TRANSPORT_MODE, Context.MODE_PRIVATE)

    val changes: Observable<Unit> = Observable.merge(
        tripPreferences.whenWheelchairPreferenceChanges().map { Unit },
        modePreferences.onChanged().map { Unit }
    )

    fun wheelchairIcon(accessible: Boolean?): Int? = when {
        accessible == false -> R.drawable.ic_wheelchair_not_accessible
        !tripPreferences.isWheelchairPreferred() -> null
        accessible == true -> R.drawable.ic_wheelchair
        else -> R.drawable.ic_wheelchair_unknown
    }

    fun wheelchairText(accessible: Boolean?): Int = when (accessible) {
        true -> R.string.wheelchair_accessible
        false -> R.string.not_wheelchair_accessible
        null -> R.string.wheelchair_accessibility_unknown
    }

    fun showBicycle(accessible: Boolean?): Boolean =
        accessible == true && transportPreferences.isBicycleOrMicromobilityEnabled()
}
