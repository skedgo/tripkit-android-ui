package com.skedgo.tripkit.ui.core.settings

import io.reactivex.Observable

interface DeveloperPreferenceRepository {
  val onIsEnabledChange: Observable<Boolean>
  val onServerChange: Observable<String>
  var isEnabled: Boolean
  var bookingsUseSandbox: Boolean
  var paymentsUseSandbox: Boolean
  val server: String
}