package com.skedgo.tripkit.ui.tripresult

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.gson.Gson
import com.skedgo.TripKit
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.notification.cancelChannelNotifications
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.utils.removeQueryParamFromUrl
import com.skedgo.tripkit.ui.utils.requestPermissionGently
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.runBlocking
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class TripSegmentGetOffAlertsViewModel @Inject internal constructor(
        var trip: Trip,
        defaultValue: Boolean = false,
        private val tripUpdater: TripUpdater,
        private val remindersRepository: RemindersRepository
) : RxViewModel() {

    companion object {
        const val URL_PARAM_HASH = "hash"
    }

    private val _getOffAlertStateOn = MutableLiveData<Boolean>(defaultValue)
    val getOffAlertStateOn: LiveData<Boolean> = _getOffAlertStateOn

    private val _isVisible = MutableLiveData<Boolean>()
    val isVisible: LiveData<Boolean> = _isVisible

    private val configs = TripKit.getInstance().configs()

    internal var alertStateListener: (Boolean) -> Unit = { _ -> }

    private val disposable = CompositeDisposable()

    init {
        val isOn = GetOffAlertCache.isTripAlertStateOn(trip.tripUuid)
        _getOffAlertStateOn.postValue(isOn)
        _isVisible.postValue(configs.hasGetOffAlerts())
    }

    val items = DiffObservableList<TripSegmentGetOffAlertDetailViewModel>(TripSegmentGetOffAlertDetailViewModel.diffCallback())
    val itemBinding = ItemBinding.of<TripSegmentGetOffAlertDetailViewModel>(BR.viewModel, R.layout.item_alert_detail)

    fun validate() {
        _getOffAlertStateOn.postValue(GetOffAlertCache.isTripAlertStateOn(trip.tripUuid))
    }

    fun setup(context: Context, details: List<TripSegmentGetOffAlertDetailViewModel>) {
        items.clear()
        items.update(details)
    }

    fun setAlertState(isOn: Boolean) {
        _getOffAlertStateOn.postValue(isOn)
    }

    fun onAlertChange(context: Context, isOn: Boolean) {
        trip.let {
            GetOffAlertCache.setTripAlertOnState(
                it.tripUuid, it.group.uuid(), isOn
            )
        }

        cancelStartTripAlarms(context) //this will cancel previous alarm that was setup
        cancelNotifications(context) //will cancel trip start and geofence notifications
        GeoLocation.clearGeofences()

        if (isOn) {
            checkPermissionAndShowProminentDisclosure(context) { isAccepted ->
                if(isAccepted) {
                    checkAccessFineLocationPermission(context)
                } else {
                    _getOffAlertStateOn.postValue(false)
                }
            }
        } else {
            trip.unsubscribeURL?.let { unsubscribeUrl ->
                tripUpdater.tripSubscription(unsubscribeUrl)
                    .subscribe({
                        //Do nothing
                    }, { e ->
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }).addTo(disposable)
            }
        }

        alertStateListener.invoke(isOn)
        _getOffAlertStateOn.postValue(isOn)
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissionAndShowProminentDisclosure(
        context: Context,
        isAccepted: (Boolean) -> Unit
    ) {
        context.requestPermissionGently(
            listOf(android.Manifest.permission.POST_NOTIFICATIONS),
            { permissionStatus ->
                permissionStatus?.let {
                    if (it.granted.isNotEmpty()) {
                        showProminentDisclosure(context, isAccepted)
                    } else {
                        isAccepted.invoke(false)
                    }
                } ?: kotlin.run {
                    showProminentDisclosure(context, isAccepted)
                }
            },
            title = context.getString(R.string.permission_request_title),
            description = context.getString(
                R.string.permission_request_notificaiton,
                context.getString(R.string.app_name)
            ),
            openSettingsTitle = context.getString(
                R.string.permission_request_notificaiton,
                context.getString(R.string.app_name)
            ),
            openSettingsExplanation = context.getString(R.string.permission_request_notificaiton_open_settings_explanation),
            Build.VERSION_CODES.TIRAMISU
        )
    }

    private fun showProminentDisclosure(context: Context, onActionClicked: (Boolean) -> Unit) {
        context.showConfirmationPopUpDialog(
            message = context.getString(R.string.msg_prominent_disclosure),
            positiveLabel = context.getString(R.string.continue_),
            positiveCallback = {
                onActionClicked.invoke(true)
            },
            negativeLabel = context.getString(R.string.cancel),
            negativeCallback = {
                onActionClicked.invoke(false)
            }
        )
    }

    /*
    * There's an issue getting automatically rejected when asking ACCESS_FINE_LOCATION and
    * ACCESS_BACKGROUND_LOCATION at the same time. So will be asking one permission
    * after the other.
    */
    private fun checkAccessFineLocationPermission(context: Context) {
        ExcuseMe.couldYouGive(context).permissionFor(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) {

            if (it.denied.isNotEmpty()) {
                _getOffAlertStateOn.postValue(false)
            } else {
                checkBackgroundLocationPermission(context)
            }
        }
    }

    private fun checkBackgroundLocationPermission(context: Context) {
        ExcuseMe.couldYouGive(context).permissionFor(
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) {

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            var pendingIntent: PendingIntent? = null
            var startSegmentStartTimeInSecs = 0L
            val reminderInMinutes = runBlocking { remindersRepository.getTripNotificationReminderMinutes() }

            trip.segments?.minByOrNull { it.startTimeInSecs }?.let { startSegment ->
                startSegmentStartTimeInSecs = startSegment.startTimeInSecs
                val alarmIntent = Intent(context, TripAlarmBroadcastReceiver::class.java)
                alarmIntent.putExtra(TripAlarmBroadcastReceiver.ACTION_START_TRIP_EVENT, true)
                alarmIntent.putExtra(TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP, Gson().toJson(trip))
                trip.group?.let {
                    alarmIntent.putExtra(
                        TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP_GROUP_UUID,
                        it.uuid()
                    )
                }
                pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0 or PendingIntent.FLAG_IMMUTABLE)
            }

            trip.subscribeURL?.let { url ->
                tripUpdater.tripSubscription(url)
                    .flatMap {
                        val updateUrl = trip.updateURL ?: ""
                        // remove hash to force get updated trip for getting the unsubscribeURL
                        if(updateUrl.contains(URL_PARAM_HASH)) {
                            updateUrl.removeQueryParamFromUrl(URL_PARAM_HASH)
                        }
                        tripUpdater.getUpdateAsync(updateUrl)
                    }
                    .subscribe({
                        this.trip = it
                    }, { e ->
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }).addTo(disposable)
            }

            if (it.denied.isNotEmpty()) {
                _getOffAlertStateOn.postValue(false)
                alarmManager.cancel(pendingIntent)
            } else {

                val reminder = TimeUnit.MINUTES.toSeconds(reminderInMinutes)

                val currentDateTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(
                    DateTime(System.currentTimeMillis(), trip.from.dateTimeZone).millis
                )

                if(startSegmentStartTimeInSecs > currentDateTimeInSeconds &&
                    (startSegmentStartTimeInSecs - currentDateTimeInSeconds) >= reminder) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        TimeUnit.SECONDS.toMillis(startSegmentStartTimeInSecs) - TimeUnit.MINUTES.toMillis(
                            reminderInMinutes
                        ),
                        pendingIntent
                    )
                }
                trip.segments?.mapNotNull { it.geofences }?.flatten()?.let { geofences ->
                    GeoLocation.createGeoFences(
                        trip,
                        geofences.map { geofence ->
                            geofence.computeAndSetTimeline(trip.endDateTime.millis)
                            geofence
                        }
                    )
                }
            }
        }
    }

    private fun cancelStartTripAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, TripAlarmBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelNotifications(context: Context) {
        context.cancelChannelNotifications(
            TripAlarmBroadcastReceiver.NOTIFICATION_TRIP_START_NOTIFICATION_ID
        )
        context.cancelChannelNotifications(
            GeofenceBroadcastReceiver.NOTIFICATION_VEHICLE_APPROACHING_NOTIFICATION_ID
        )
        context.cancelChannelNotifications(11111)
    }

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}

class TripSegmentGetOffAlertDetailViewModel @Inject internal constructor(
        val icon: Drawable?,
        val title: String,
        val isEnabled: Boolean = true
) : RxViewModel() {
    companion object {
        fun diffCallback() = object : DiffUtil.ItemCallback<TripSegmentGetOffAlertDetailViewModel>() {
            override fun areItemsTheSame(oldItem: TripSegmentGetOffAlertDetailViewModel, newItem: TripSegmentGetOffAlertDetailViewModel): Boolean =
                    oldItem.title == newItem.title

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: TripSegmentGetOffAlertDetailViewModel, newItem: TripSegmentGetOffAlertDetailViewModel): Boolean =
                    oldItem.icon == newItem.icon
                            && oldItem.title == newItem.title
        }
    }
}