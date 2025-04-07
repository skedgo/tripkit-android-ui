package com.skedgo.tripkit.ui.model

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.common.model.BicycleAccessible
import com.skedgo.tripkit.common.model.WheelchairAccessible
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.time.ITimeRange
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.routing.VehicleMode
import com.skedgo.tripkit.ui.BuildConfig
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber

/**
 * (Aka Service)
 */
class TimetableEntry : Parcelable, IRealTimeElement, ITimeRange, WheelchairAccessible,
    BicycleAccessible {
    @Transient
    val stops: BehaviorSubject<List<StopInfo>> = BehaviorSubject.create()

    /**
     * For A2B-timetable-related stuff.
     */
    @Transient
    var startStop: ScheduledStop? = null

    /**
     * For A2B-timetable-related stuff.
     */
    @Transient
    var endStop: ScheduledStop? = null

    /**
     * For A2B-timetable-related stuff.
     */
    var pairIdentifier: String? = null

    @Transient
    var id: Long = 0

    @Transient
    var isFavourite: Boolean = false

    @SerializedName("realtimeVehicle")
    var realtimeVehicle: RealTimeVehicle? = null

    @SerializedName("stopCode")
    var stopCode: String? = null

    @SerializedName("modeInfo")
    var modeInfo: ModeInfo? = null

    @SerializedName("operator")
    override var operator: String? = null

    @SerializedName("endStopCode")
    override var endStopCode: String? = null

    @SerializedName("serviceTripID")
    override var serviceTripId: String? = null

    @SerializedName("serviceNumber")
    var serviceNumber: String? = null

    @SerializedName("serviceName")
    var serviceName: String? = null

    @SerializedName("serviceDirection")
    var serviceDirection: String? = null

    @SerializedName("realTimeStatus")
    var realTimeStatus: RealTimeStatus? = null

    @SerializedName("realTimeDeparture")
    var realTimeDeparture: Int = -1

    @SerializedName("realTimeArrival")
    var realTimeArrival: Int = -1

    @SerializedName("alerts")
    var alerts: List<RealtimeAlert>? = null

    @SerializedName("serviceColor")
    var serviceColor: ServiceColor? = null

    @SerializedName("frequency")
    var frequency = 0

    @SerializedName("searchString")
    var searchString: String? = null

    @SerializedName("alertHashCodes")
    var alertHashCodes: ArrayList<Long>? = null

    @SerializedName("wheelchairAccessible")
    override var wheelchairAccessible: Boolean? = null

    @SerializedName("bicycleAccessible")
    override var bicycleAccessible: Boolean? = null

    @SerializedName("start_stop_short_name")
    var startStopShortName: String? = null

    @SerializedName("startPlatform")
    var startPlatform: String? = null

    /**
     * Replacement: [.modeInfo].
     */
    @Deprecated("")
    @SerializedName("mode")
    var mode: VehicleMode? = null

    /**
     * This field is primarily used to interact with Gson or Parcel.
     */
    @SerializedName("startTime")
    private var serializedStartSecs: Long = 0

    /**
     * This field is primarily used to interact with Gson or Parcel.
     */
    @SerializedName("endTime")
    private var serializedEndSecs: Long = 0

    /**
     * Service time is initially the same as "startTime". If is a realtime service, here we save the
     * service time, while startTime will have the real arriving time.
     */
    var serviceTime: Long = 0

    override var startStopCode: String?
        get() = stopCode
        set(value) {
            stopCode = value
        }
    override val startTimeInSeconds: Long
        get() = serializedStartSecs
    override var startTimeInSecs: Long
        get() = serializedStartSecs
        set(value) {
            serializedStartSecs = value
        }
    override var endTimeInSecs: Long
        get() = serializedEndSecs
        set(value) {
            serializedEndSecs = value
        }

    var isCancelled: Boolean = false

    init {
        // For debug purpose only.
        if (BuildConfig.DEBUG) {
            stops.toFlowable(BUFFER).subscribe { stops: List<StopInfo> ->
                Timber.w(
                    "LoadStops",
                    "Got " + stops.size + " stops for: " + serviceNumber + " - " + this@TimetableEntry
                )
            }
        }
    }

    val isFrequencyBased: Boolean
        get() = frequency > 0

    fun hasAlerts(): Boolean {
        return alerts?.isNotEmpty() == true
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(id)
        out.writeString(stopCode)
        out.writeString(serviceTripId)
        out.writeString(serviceNumber)
        out.writeString(serviceName)
        out.writeString(if (realTimeStatus != null) realTimeStatus!!.name else null)
        out.writeLong(startTimeInSecs)
        out.writeLong(endTimeInSecs)
        out.writeParcelable(serviceColor, 0)
        out.writeInt(frequency)
        out.writeInt(if (isFavourite) 1 else 0)
        out.writeList(alerts)
        out.writeString(searchString)
        out.writeString(endStopCode)
        out.writeParcelable(startStop, 0)
        out.writeParcelable(endStop, 0)
        out.writeString(if (mode == null) null else mode.toString())
        out.writeString(pairIdentifier)
        out.writeString(operator)
        out.writeParcelable(realtimeVehicle, 0)
        out.writeLong(serviceTime)
        out.writeParcelable(modeInfo, 0)
        out.writeString(serviceDirection)
        out.writeValue(wheelchairAccessible)
        out.writeValue(bicycleAccessible)
        out.writeValue(startStopShortName)
        out.writeList(alertHashCodes)
        out.writeString(startPlatform)
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            out.writeBoolean(isCancelled)
        } else {
            out.writeInt(if (isCancelled) 1 else 0)
        }
    }

    /**
     * For example, in order to determine a past service trip.
     */
    fun isBefore(pointSecs: Long): Boolean {
        return if (serializedEndSecs > 0) {
            serializedEndSecs < pointSecs
        } else {
            // Some services don't have arrival time.
            serializedStartSecs < pointSecs
        }
    }

    /**
     * For debug purpose only.
     */
    override fun toString(): String {
        // Trim the package part to print out something less verbal.
        return TimetableEntry::class.java.simpleName + hashCode()
    }

    companion object {
        @JvmField
        val CREATOR: Creator<TimetableEntry> = object : Creator<TimetableEntry> {
            override fun createFromParcel(`in`: Parcel): TimetableEntry {
                val service = TimetableEntry()

                service.id = `in`.readLong()
                service.stopCode = `in`.readString()
                service.serviceTripId = `in`.readString()
                service.serviceNumber = `in`.readString()
                service.serviceName = `in`.readString()
                service.realTimeStatus = RealTimeStatus.from(`in`.readString())
                service.startTimeInSecs = `in`.readLong()
                service.endTimeInSecs = `in`.readLong()
                service.serviceColor = `in`.readParcelable(ServiceColor::class.java.classLoader)
                service.frequency = `in`.readInt()
                service.isFavourite = `in`.readInt() == 1
                service.alerts = `in`.readArrayList(RealtimeAlert::class.java.classLoader) as ArrayList<RealtimeAlert>?
                service.searchString = `in`.readString()
                service.endStopCode = `in`.readString()
                service.startStop = `in`.readParcelable(ScheduledStop::class.java.classLoader)
                service.endStop = `in`.readParcelable(ScheduledStop::class.java.classLoader)
                service.mode = VehicleMode.from(`in`.readString())
                service.pairIdentifier = `in`.readString()
                service.operator = `in`.readString()
                service.realtimeVehicle =
                    `in`.readParcelable(RealTimeVehicle::class.java.classLoader)
                service.serviceTime = `in`.readLong()
                service.modeInfo = `in`.readParcelable(ModeInfo::class.java.classLoader)
                service.serviceDirection = `in`.readString()
                service.wheelchairAccessible =
                    `in`.readValue(Boolean::class.java.classLoader) as Boolean?
                service.bicycleAccessible =
                    `in`.readValue(Boolean::class.java.classLoader) as Boolean?
                service.startStopShortName = `in`.readString()
                service.alertHashCodes = `in`.readArrayList(Long::class.java.classLoader) as ArrayList<Long>?
                service.serviceColor = `in`.readParcelable(ServiceColor::class.java.classLoader)
                service.startPlatform = `in`.readString()
                service.isCancelled = if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                    `in`.readBoolean()
                } else {
                    `in`.readInt() == 1
                }
                return service
            }

            override fun newArray(size: Int): Array<TimetableEntry?> {
                return arrayOfNulls(size)
            }
        }
    }
}