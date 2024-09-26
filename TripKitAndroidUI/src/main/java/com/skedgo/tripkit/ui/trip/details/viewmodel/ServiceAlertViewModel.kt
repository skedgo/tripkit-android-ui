package com.skedgo.tripkit.ui.trip.details.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import io.reactivex.Observable
import javax.inject.Inject

open class ServiceAlertViewModel @Inject constructor(private val context: Context) {
    val title: ObservableField<String> = ObservableField()
    val hasAlerts: ObservableBoolean = ObservableBoolean(false)
    val alertIcon = ObservableField<Drawable?>()
    private val showAlert: PublishRelay<Unit> = PublishRelay.create()
    open val showAlertsObservable: Observable<Unit> = showAlert.hide()

    open fun setAlerts(alerts: List<RealtimeAlert>?) {

        hasAlerts.set(alerts.isNullOrEmpty().not())
        with(alerts) {
            val size = this.orEmpty().size
            if (this.orEmpty().size == 1) {
                title.set(this!!.first().title())
            } else {
                title.set("$size ${context.getString(R.string.alerts)}")
            }
        }
        alerts.getMostSevereAlert()
            ?.let {
                if (it.severity() == RealtimeAlert.SEVERITY_ALERT) {
                    R.drawable.ic_alert_red_overlay
                } else {
                    R.drawable.ic_alert_yellow_overlay
                }
            }
            ?.let { ContextCompat.getDrawable(context, it) }
            .let {
                alertIcon.set(it)
            }
    }

    fun onShow() = showAlert.accept(Unit)
}