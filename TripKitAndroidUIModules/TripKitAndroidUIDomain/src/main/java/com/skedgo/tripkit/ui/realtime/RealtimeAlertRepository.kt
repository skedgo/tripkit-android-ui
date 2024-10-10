package com.skedgo.tripkit.ui.realtime

import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import io.reactivex.Observable

interface RealtimeAlertRepository {

    fun addAlerts(alerts: List<RealtimeAlert>)

    fun addAlertHashCodesForId(id: String, alertHashCodes: List<Long>)

    fun getAlerts(id: String): List<RealtimeAlert>?

    fun onAlertForIdAdded(id: String): Observable<RealtimeAlert>
}