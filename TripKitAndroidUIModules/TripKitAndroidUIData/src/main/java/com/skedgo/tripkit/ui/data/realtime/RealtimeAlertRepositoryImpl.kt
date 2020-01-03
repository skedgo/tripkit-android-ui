package com.skedgo.tripkit.ui.data.realtime

import android.util.Log
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.ui.realtime.RealtimeAlertRepository
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject

class RealtimeAlertRepositoryImpl @Inject constructor() : RealtimeAlertRepository {

  private val alerts = mutableMapOf<Long, RealtimeAlert>()
  private val alertsHashCodeMap = mutableMapOf<String, List<Long>>()
  private val whenRealtimeAlertAdded = PublishRelay.create<Pair<Long, RealtimeAlert>>()

  override fun addAlerts(alerts: List<RealtimeAlert>) {
    alerts.forEach {
      this.alerts.put(it.remoteHashCode(), it)
      whenRealtimeAlertAdded.accept(Pair(it.remoteHashCode(), it))
    }
  }

  override fun addAlertHashCodesForId(id: String, alertHashCodes: List<Long>) {
    alertsHashCodeMap[id] = alertHashCodes
  }

  override fun getAlerts(id: String): List<RealtimeAlert>? =
      alertsHashCodeMap[id]?.mapNotNull { alerts[it] }

  override fun onAlertForIdAdded(id: String): Observable<RealtimeAlert> =
      whenRealtimeAlertAdded.filter { (hashCode, _) ->
            alertsHashCodeMap[id]?.contains(hashCode) ?: false
          }
          .map { (_, alert) -> alert }
}