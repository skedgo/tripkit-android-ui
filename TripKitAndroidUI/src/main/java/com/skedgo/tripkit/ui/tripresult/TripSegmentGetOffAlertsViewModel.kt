package com.skedgo.tripkit.ui.tripresult

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.afollestad.materialdialogs.MaterialDialog
import com.skedgo.tripkit.ui.utils.isPermissionGranted
import java.util.UUID
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.skedgo.TripKit
import com.skedgo.tripkit.TripUpdater
import com.skedgo.tripkit.hasValidSubscribeUrl
import com.skedgo.tripkit.validatedSubscriptionUrl
import com.skedgo.tripkit.checkIfLocationProviderIsEnabled
import com.skedgo.tripkit.notification.cancelChannelNotifications
import com.skedgo.tripkit.routing.GeoLocation
import com.skedgo.tripkit.routing.GeofenceBroadcastReceiver
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.MessageType
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripAlarmBroadcastReceiver
import com.skedgo.tripkit.routing.dateTimeZone
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.routing.settings.RemindersRepository
import com.skedgo.tripkit.ui.utils.removeQueryParamFromUrl
import com.skedgo.tripkit.ui.utils.requestPermissionGently
import com.skedgo.tripkit.ui.utils.showConfirmationPopUpDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.runBlocking
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import org.joda.time.DateTime
import timber.log.Timber
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

    val _getOffAlertStateOn = MutableLiveData<Boolean>(defaultValue)
    val getOffAlertStateOn: LiveData<Boolean> = _getOffAlertStateOn

    var alertStateToggleCustomValidation: ((Context, Boolean) -> Unit)? = null

    private val _isVisible = MutableLiveData<Boolean>()
    val isVisible: LiveData<Boolean> = _isVisible

    private val configs = TripKit.getInstance().configs()

    internal var alertStateListener: (Boolean) -> Unit = { _ -> }

    private val disposable = CompositeDisposable()
    private var enablingAlerts = false
    private var disposed = false
    private var pendingActivation: Disposable? = null
    private var pendingSubscriptionUrl: String? = null
    private var lastSubscriptionUrl = validatedSubscriptionUrl(trip.subscribeURL)
    private val permissionRequests = CompositeDisposable()
    private var permissionPending = false
    private var activationGeneration = 0L

    internal var showGeofencesOnMap: (List<Pair<LatLng, Double>>) -> Unit = { _ -> }

    init {
        val isOn = GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid())
        _getOffAlertStateOn.postValue(isOn)
        _isVisible.postValue(configs.hasGetOffAlerts())
    }

    val items = DiffObservableList<TripSegmentGetOffAlertDetailViewModel>(
        TripSegmentGetOffAlertDetailViewModel.diffCallback()
    )
    val itemBinding by lazy {
        ItemBinding.of<TripSegmentGetOffAlertDetailViewModel>(
            BR.viewModel,
            R.layout.item_alert_detail
        )
    }

    fun setGetOffAlertStateOn(isOn: Boolean) {
        _getOffAlertStateOn.postValue(isOn)
    }

    fun validate() {
        _getOffAlertStateOn.postValue(GetOffAlertCache.isTripAlertStateOn(trip.getTripUuid()))
    }

    fun setup(context: Context, details: List<TripSegmentGetOffAlertDetailViewModel>) {
        refreshSubscriptionCapability(context)
        items.update(details)
    }

    private fun refreshSubscriptionCapability(context: Context) {
        val endpoint = validatedSubscriptionUrl(trip.subscribeURL)
        val restoringLocalAlerts = items.isEmpty() && endpoint == null && _getOffAlertStateOn.value == true
        if (endpoint == lastSubscriptionUrl && !restoringLocalAlerts) return
        val hadServerCapability = lastSubscriptionUrl != null
        lastSubscriptionUrl = endpoint
        // An acknowledgement belongs to the endpoint requested, never a refreshed capability.
        if (enablingAlerts && endpoint != pendingSubscriptionUrl) {
            pendingActivation?.dispose()
            enablingAlerts = false
        }
        val boardingTitle = context.getString(R.string.get_off_alerts_vehicle_is_approaching_your_boarding_stop)
        items.update(items.map { detail ->
            if (detail.title == boardingTitle) {
                TripSegmentGetOffAlertDetailViewModel(detail.icon, detail.title, hasValidSubscribeUrl(trip))
            } else detail
        })
        if ((hadServerCapability || restoringLocalAlerts) && endpoint == null && _getOffAlertStateOn.value == true) {
            // Replace the alarm too: its serialized trip must not retain the previous URL.
            cancelStartTripAlarms(context)
            GeoLocation.clearGeofences()
            showGeofencesOnMap.invoke(emptyList())
            setupAlarmsAndGeofences(context)
        }
    }

    fun setAlertState(context: Context, isOn: Boolean) {
        alertStateToggleCustomValidation?.invoke(context, isOn) ?: run {
            onAlertChange(context, isOn)
        }
    }

    fun onAlertChange(context: Context, isOn: Boolean) {
        if (disposed) return
        if (!isOn) {
            cancelPermissionRequest()
            disableAlerts(context)
            return
        }
        if (permissionPending || enablingAlerts || _getOffAlertStateOn.value == true) return

        permissionPending = true
        val generation = ++activationGeneration
        val activity = findActivity(context)
        if (activity != null) {
            val observer = object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    cancelPermissionRequest()
                }
            }
            activity.lifecycle.addObserver(observer)
            permissionRequests.add(Disposables.fromAction { activity.lifecycle.removeObserver(observer) })
        }
        fun current() = !disposed && permissionPending && generation == activationGeneration
        showProminentDisclosure(context) { accepted ->
            if (!current()) return@showProminentDisclosure
            if (!accepted) {
                cancelPermissionRequest()
                return@showProminentDisclosure
            }
            checkPermissions(context) { granted ->
                if (!current()) return@checkPermissions
                cancelPermissionRequest()
                if (granted) enableAlerts(context)
            }
        }
    }

    private fun cancelPermissionRequest() {
        permissionPending = false
        activationGeneration++
        permissionRequests.clear()
        if (_getOffAlertStateOn.value != true) _getOffAlertStateOn.postValue(false)
    }

    private fun findActivity(context: Context): FragmentActivity? = when (context) {
        is FragmentActivity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    private fun enableAlerts(context: Context) {
        if (disposed || enablingAlerts || _getOffAlertStateOn.value == true) return
        val subscribeUrl = validatedSubscriptionUrl(trip.subscribeURL)
        if (subscribeUrl == null) {
            activateLocalAlerts(context)
            return
        }

        // A cached trip can still offer a server subscription after the user has signed out.
        // Do not claim activation or fall back to local alerts if that operation cannot succeed.
        enablingAlerts = true
        pendingSubscriptionUrl = subscribeUrl
        Observable.fromCallable {
            if (configs.userTokenProvider()?.call().isNullOrBlank()) {
                throw SecurityException()
            }
            subscribeUrl
        }
            .subscribeOn(Schedulers.io())
            .flatMap { tripUpdater.tripSubscription(it).firstOrError().toObservable() }
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                enablingAlerts = false
                pendingSubscriptionUrl = null
            }
            .subscribe({
                if (!enablingAlerts) return@subscribe
                if (validatedSubscriptionUrl(trip.subscribeURL) != subscribeUrl) return@subscribe
                activateLocalAlerts(context)
                // Refresh the unsubscribe URL after the server has acknowledged the subscription.
                trip.updateURL?.takeIf { it.isNotBlank() }?.let { updateUrl ->
                    tripUpdater.getUpdateAsync(updateUrl.removeQueryParamFromUrl(URL_PARAM_HASH))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            this.trip = it
                            refreshSubscriptionCapability(context)
                        }, { Timber.w("[GetOffAlerts] Subscription trip refresh failed") })
                        .addTo(disposable)
                }
            }, { error ->
                Timber.w("[GetOffAlerts] Server subscription failed; alerts remain off")
                _getOffAlertStateOn.postValue(false)
                context.showConfirmationPopUpDialog(
                    title = context.getString(R.string.error_encountered),
                    message = context.getString(
                        if (error is SecurityException) R.string.authentication_error
                        else R.string.an_unexpected_network_error_has_occurred_dot_please_retry_dot
                    ),
                    positiveLabel = context.getString(R.string.ok)
                )
            }).also { pendingActivation = it }.addTo(disposable)
    }

    private fun activateLocalAlerts(context: Context) {
        Timber.d("[GetOffAlerts] Permissions approved. Enabling alerts.")
        trip.let {
            GetOffAlertCache.setTripAlertOnState(
                it.getTripUuid(), it.group?.uuid().orEmpty(), true
            )
        }

        _getOffAlertStateOn.postValue(true)
        alertStateListener.invoke(true)

        cancelStartTripAlarms(context)
        cancelNotifications(context)
        GeoLocation.clearGeofences()
        showGeofencesOnMap.invoke(emptyList())

        setupAlarmsAndGeofences(context)
    }

    private fun disableAlerts(context: Context) {
        pendingActivation?.dispose()
        enablingAlerts = false
        Timber.d("[GetOffAlerts] Disabling alerts.")
        trip.let {
            GetOffAlertCache.setTripAlertOnState(
                it.getTripUuid(), it.group?.uuid().orEmpty(), false
            )
        }

        cancelStartTripAlarms(context)
        cancelNotifications(context)
        GeoLocation.clearGeofences()
        showGeofencesOnMap.invoke(emptyList())

        trip.unsubscribeURL?.let { unsubscribeUrl ->
            tripUpdater.tripSubscription(unsubscribeUrl)
                .subscribe({
                    // Do nothing
                }, { e ->
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }).addTo(disposable)
        }

        _getOffAlertStateOn.postValue(false)
        alertStateListener.invoke(false)
    }

    private fun checkPermissions(context: Context, onResult: (Boolean) -> Unit) {
        val generation = activationGeneration
        checkNotificationPermission(context) { isAccepted ->
            if (!permissionPending || generation != activationGeneration) return@checkNotificationPermission
            Timber.d("[GetOffAlerts] Notification permission result: accepted=%s", isAccepted)
            if (!isAccepted) {
                Timber.w("[GetOffAlerts] Reverting switch OFF: POST_NOTIFICATIONS denied.")
                showGoToSettingsDialog(
                    context = context,
                    message = context.getString(R.string.permission_request_notificaiton_open_settings_explanation)
                )
                onResult(false)
                return@checkNotificationPermission
            }
            // Time-based alarms do not need location; only supplied geofences do.
            if (eligibleGeofences().isEmpty()) {
                onResult(true)
                return@checkNotificationPermission
            }
            val isLocationProviderEnabled = context.checkIfLocationProviderIsEnabled()
            if (!isLocationProviderEnabled) {
                Timber.w("[GetOffAlerts] Reverting switch OFF: location provider disabled.")
                context.showConfirmationPopUpDialog(
                    title = context.getString(R.string.location_provider_disabled),
                    message = context.getString(R.string.device_location_is_turned_off),
                    positiveLabel = context.getString(R.string.close),
                    positiveCallback = {
                        Timber.w("[GetOffAlerts] Reverting switch OFF: user acknowledged location provider disabled dialog.")
                    }
                )
                onResult(false)
            } else {
                checkAccessFineLocationPermission(context, onResult)
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkNotificationPermission(context: Context, isAccepted: (Boolean) -> Unit) {
        context.requestPermissionGently(
            listOf(android.Manifest.permission.POST_NOTIFICATIONS),
            { permissionStatus ->
                isAccepted.invoke(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    permissionStatus?.granted?.isNotEmpty() == true)
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
        showPermissionDialog(context, null, context.getString(R.string.msg_prominent_disclosure), onActionClicked)
    }

    private fun showPermissionDialog(context: Context, title: String?, message: String, onResult: (Boolean) -> Unit) {
        var completed = false
        fun finish(granted: Boolean) {
            if (completed) return
            completed = true
            onResult(granted)
        }
        val dialog = MaterialDialog(context).cancelable(true).show {
            title?.let { title(text = it) }
            message(text = message)
            positiveButton(text = context.getString(R.string.continue_)) { finish(true) }
            negativeButton(text = context.getString(R.string.cancel)) { finish(false) }
        }
        dialog.setOnCancelListener { finish(false) }
        dialog.setOnDismissListener { finish(false) }
        permissionRequests.add(Disposables.fromAction {
            completed = true
            dialog.dismiss()
        })
    }

    private fun showGoToSettingsDialog(context: Context, message: String) {
        context.showConfirmationPopUpDialog(
            title = context.getString(R.string.permission_required),
            message = message,
            positiveLabel = context.getString(R.string.permission_go_to_settings),
            positiveCallback = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            },
            negativeLabel = context.getString(R.string.cancel)
        )
    }

    /*
    * There's an issue getting automatically rejected when asking ACCESS_FINE_LOCATION and
    * ACCESS_BACKGROUND_LOCATION at the same time. So will be asking one permission
    * after the other.
    */
    private fun checkAccessFineLocationPermission(context: Context, onResult: (Boolean) -> Unit) {
        val generation = activationGeneration
        ExcuseMe.couldYouGive(context).permissionFor(
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) {
            if (!permissionPending || generation != activationGeneration) return@permissionFor
            Timber.d("[GetOffAlerts] Fine location permission result: granted=%s denied=%s", it.granted, it.denied)
            if (it.denied.isNotEmpty()) {
                Timber.w("[GetOffAlerts] Reverting switch OFF: ACCESS_FINE_LOCATION denied.")
                showGoToSettingsDialog(
                    context = context,
                    message = context.getString(R.string.permission_go_to_settings_message)
                )
                onResult(false)
            } else {
                checkBackgroundLocationPermission(context, onResult)
            }
        }
    }

    private fun checkBackgroundLocationPermission(context: Context, onResult: (Boolean) -> Unit) {
        if (!permissionPending) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.isPermissionGranted(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            onResult(true)
            return
        }
        showPermissionDialog(context,
            context.getString(R.string.confirmation_allow_background_location_title),
            context.getString(R.string.trip_alert_background_location_explanation)) { openSettings ->
            if (!permissionPending) return@showPermissionDialog
            if (!openSettings) {
                onResult(false)
                return@showPermissionDialog
            }
            val activity = findActivity(context)
            if (activity == null) {
                onResult(false)
                return@showPermissionDialog
            }
            // Consume one Settings result; unchanged permissions are cancellation, never a retry.
            val launcher = activity.activityResultRegistry.register(
                "trip-alert-location-" + UUID.randomUUID(),
                ActivityResultContracts.StartActivityForResult()
            ) {
                onResult(context.isPermissionGranted(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
            permissionRequests.add(Disposables.fromAction { launcher.unregister() })
            launcher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)))
        }
    }

    private fun eligibleGeofences() = trip.segmentList.orEmpty().flatMap { it.geofences.orEmpty() }
        .filter { hasValidSubscribeUrl(trip) || it.messageType != MessageType.VEHICLE_IS_APPROACHING.name }

    private fun setupAlarmsAndGeofences(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var pendingIntent: PendingIntent? = null
        var startSegmentStartTimeInSecs = 0L
        val reminderInMinutes =
            runBlocking { remindersRepository.getTripNotificationReminderMinutes() }

        trip.segmentList?.minByOrNull { it.startTimeInSecs }?.let { startSegment ->
            startSegmentStartTimeInSecs = startSegment.startTimeInSecs
            val alarmIntent = Intent(context, TripAlarmBroadcastReceiver::class.java)
            alarmIntent.putExtra(TripAlarmBroadcastReceiver.ACTION_START_TRIP_EVENT, true)
            alarmIntent.putExtra(
                TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP,
                Gson().toJson(trip)
            )
            trip.group?.let {
                alarmIntent.putExtra(
                    TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP_GROUP_UUID,
                    it.uuid()
                )
            }
            pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                0 or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val reminder = TimeUnit.MINUTES.toSeconds(reminderInMinutes)

        val currentDateTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(
            DateTime(System.currentTimeMillis(), trip.from?.dateTimeZone).millis
        )

        if (startSegmentStartTimeInSecs > currentDateTimeInSeconds &&
            (startSegmentStartTimeInSecs - currentDateTimeInSeconds) >= reminder) {
            pendingIntent?.let { intent ->
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    TimeUnit.SECONDS.toMillis(startSegmentStartTimeInSecs) - TimeUnit.MINUTES.toMillis(
                        reminderInMinutes
                    ),
                    intent
                )
            }
        }
        eligibleGeofences().takeIf { it.isNotEmpty() }?.let { geofences ->
            GeoLocation.createGeoFences(
                trip,
                geofences.map { geofence ->
                    geofence.computeAndSetTimeline(trip.endDateTime.millis)
                    geofence
                }
            ) { added ->
                if (added && configs.showGeofences()) {
                    showGeofencesOnMap.invoke(
                        geofences.map {
                            LatLng(it.center.lat, it.center.lng) to it.radius
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
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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

    internal fun disposeAlerts() {
        disposed = true
        cancelPermissionRequest()
        permissionRequests.dispose()
        enablingAlerts = false
        disposable.dispose()
    }

    override fun onCleared() {
        disposeAlerts()
        super.onCleared()
    }
}

class TripSegmentGetOffAlertDetailViewModel @Inject internal constructor(
    val icon: Drawable?,
    val title: String,
    val isEnabled: Boolean = true
) : RxViewModel() {
    companion object {
        fun diffCallback() =
            object : DiffUtil.ItemCallback<TripSegmentGetOffAlertDetailViewModel>() {
                override fun areItemsTheSame(
                    oldItem: TripSegmentGetOffAlertDetailViewModel,
                    newItem: TripSegmentGetOffAlertDetailViewModel
                ): Boolean =
                    oldItem.title == newItem.title

                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(
                    oldItem: TripSegmentGetOffAlertDetailViewModel,
                    newItem: TripSegmentGetOffAlertDetailViewModel
                ): Boolean =
                    oldItem.icon == newItem.icon
                        && oldItem.title == newItem.title
                        && oldItem.isEnabled == newItem.isEnabled
            }
    }
}
