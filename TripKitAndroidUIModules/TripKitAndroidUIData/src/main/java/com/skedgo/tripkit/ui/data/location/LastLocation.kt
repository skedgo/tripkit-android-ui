package com.skedgo.tripkit.ui.data.location
import android.location.Location

sealed class LastLocation {
  data class Available(val value: Location) : LastLocation()
  object NotAvailable : LastLocation()
}