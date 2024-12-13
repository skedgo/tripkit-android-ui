package com.skedgo.tripkit.ui.data

import android.database.Cursor
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.routing.ServiceColor
import com.skedgo.tripkit.routing.VehicleMode
import com.skedgo.tripkit.ui.model.TimetableEntry
import javax.inject.Inject

class CursorToServiceConverter @Inject constructor(private val gson: Gson) :
    CursorToEntityConverter<TimetableEntry> {
    private val mServiceColumnIndices = ServiceColumnIndices()
    private lateinit var mCursor: Cursor

    override fun apply(cursor: Cursor): TimetableEntry {
        setCursor(cursor)

        val service = TimetableEntry()
        service.id = id
        service.pairIdentifier = pairIdentifier
        service.stopCode = stopCode
        service.endStopCode = endStopCode
        service.mode = mode
        service.startTimeInSecs = startTimeInSecs
        service.endTimeInSecs = endTimeInSecs
        service.frequency = frequency
        service.serviceNumber = serviceNumber
        service.serviceName = serviceName
        service.serviceTripId = serviceTripId
        service.serviceColor = ServiceColor(
            serviceColorRed,
            serviceColorGreen,
            serviceColorBlue
        )
        service.operator = serviceOperator
        service.realTimeStatus = realTimeStatus
        if (cursor.getColumnIndex("realTimeDeparture") != -1) {
            service.realTimeDeparture = cursor.getInt(cursor.getColumnIndex("realTimeDeparture"))
        }

        if (cursor.getColumnIndex("realTimeArrival") != -1) {
            service.realTimeArrival = cursor.getInt(cursor.getColumnIndex("realTimeArrival"))
        }
        service.isFavourite(isFavourite)
        service.searchString = searchString
        service.serviceTime = serviceTime
        service.setWheelchairAccessible(wheelchairAccessible)
        service.setBicycleAccessible(bicycleAccessible)
        service.startStopShortName = startStopShortName
        val modeInfoJson = cursor.getString(cursor.getColumnIndex(DbFields.MODE_INFO.name))
        if (modeInfoJson != null) {
            val modeInfo = gson.fromJson(modeInfoJson, ModeInfo::class.java)
            service.modeInfo = modeInfo
        }

        // TODO: What about hasAlerts()?
        service.serviceDirection =
            cursor.getString(cursor.getColumnIndex(DbFields.SERVICE_DIRECTION.name))
        service.startPlatform =
            cursor.getString(cursor.getColumnIndex(DbFields.START_PLATFORM.name))
        return service
    }

    fun setCursor(cursor: Cursor) {
        mCursor = cursor

        // Cache column indices if necessary
        mServiceColumnIndices.getColumnIndices(cursor)
    }

    val id: Long
        get() = mCursor.getInt(mServiceColumnIndices.idIndex).toLong()

    val pairIdentifier: String
        get() = mCursor.getString(mServiceColumnIndices.pairIdentifierIndex) ?: ""

    val stopCode: String
        get() = mCursor.getString(mServiceColumnIndices.stopCodeIndex) ?: ""

    val endStopCode: String
        get() = mCursor.getString(mServiceColumnIndices.endStopCodeIndex) ?: ""

    val mode: VehicleMode?
        get() = VehicleMode.from(mCursor.getString(mServiceColumnIndices.modeIndex))

    val startTimeInSecs: Long
        get() = mCursor.getLong(mServiceColumnIndices.startTimeIndex)

    val endTimeInSecs: Long
        get() = mCursor.getLong(mServiceColumnIndices.endTimeIndex)

    val frequency: Int
        get() = mCursor.getInt(mServiceColumnIndices.frequencyIndex)

    val serviceNumber: String
        get() = mCursor.getString(mServiceColumnIndices.serviceNumberIndex) ?: ""

    val serviceName: String
        get() = mCursor.getString(mServiceColumnIndices.serviceNameIndex) ?: ""

    val serviceTripId: String
        get() = mCursor.getString(mServiceColumnIndices.serviceTripIdIndex) ?: ""

    val serviceColorRed: Int
        get() = mCursor.getInt(mServiceColumnIndices.serviceColorRedIndex)

    val serviceColorGreen: Int
        get() = mCursor.getInt(mServiceColumnIndices.serviceColorGreenIndex)

    val serviceColorBlue: Int
        get() = mCursor.getInt(mServiceColumnIndices.serviceColorBlueIndex)

    val realTimeStatus: RealTimeStatus?
        get() = RealTimeStatus.from(mCursor.getString(mServiceColumnIndices.realTimeStatusIndex))

    val isFavourite: Boolean
        get() = mCursor.getInt(mServiceColumnIndices.favouriteIndex) > 0

    val searchString: String
        get() = mCursor.getString(mServiceColumnIndices.searchStringIndex) ?: ""

    val serviceOperator: String
        get() = mCursor.getString(mServiceColumnIndices.serviceOperator) ?: ""

    val serviceTime: Long
        get() = mCursor.getLong(mServiceColumnIndices.serviceTimeIndex)

    val wheelchairAccessible: Boolean?
        get() {
            val wheelchairAccessible = mCursor.getInt(mServiceColumnIndices.wheelchairAccessible)
            return when (wheelchairAccessible) {
                0 -> false
                1 -> true
                else -> null
            }
        }

    val bicycleAccessible: Boolean?
        get() {
            val bicycleAccessible = mCursor.getInt(mServiceColumnIndices.bicycleAccessible)
            return when (bicycleAccessible) {
                0 -> false
                1 -> true
                else -> null
            }
        }

    val startStopShortName: String
        get() = mCursor.getString(mServiceColumnIndices.startStopShortName) ?: ""

    /**
     * NOTE: Don't hard code column indices
     */
    class ServiceColumnIndices {
        var idIndex: Int = -1
        var pairIdentifierIndex: Int = -1
        var stopCodeIndex: Int = -1
        var endStopCodeIndex: Int = -1
        var modeIndex: Int = -1
        var startTimeIndex: Int = -1
        var endTimeIndex: Int = -1
        var julianDayIndex: Int = -1
        var frequencyIndex: Int = -1
        var serviceNumberIndex: Int = -1
        var serviceNameIndex: Int = -1
        var serviceTripIdIndex: Int = -1
        var serviceColorRedIndex: Int = -1
        var serviceColorBlueIndex: Int = -1
        var serviceColorGreenIndex: Int = -1
        var realTimeStatusIndex: Int = -1
        var favouriteIndex: Int = -1
        var hasAlertsIndex: Int = -1
        var searchStringIndex: Int = -1
        var serviceTimeIndex: Int = -1
        var serviceOperator: Int = -1
        var wheelchairAccessible: Int = -1
        var bicycleAccessible: Int = -1
        var startStopShortName: Int = -1
        var startPlatform: Int = -1

        fun getColumnIndices(cursor: Cursor) {
            // The '_id' column always appears on query result.
            // So we utilize it to check if all the indices are retrieved.
            if (idIndex != -1) {
                // Okay, previously cached.
                // Let's ignore the rest!
                return
            }

            idIndex = cursor.getColumnIndex(DbFields.ID.name)
            pairIdentifierIndex = cursor.getColumnIndex(DbFields.PAIR_IDENTIFIER.name)
            stopCodeIndex = cursor.getColumnIndex(DbFields.STOP_CODE.name)
            endStopCodeIndex = cursor.getColumnIndex(DbFields.END_STOP_CODE.name)
            modeIndex = cursor.getColumnIndex(DbFields.MODE.name)
            startTimeIndex = cursor.getColumnIndex(DbFields.START_TIME.name)
            endTimeIndex = cursor.getColumnIndex(DbFields.END_TIME.name)
            julianDayIndex = cursor.getColumnIndex(DbFields.JULIAN_DAY.name)
            frequencyIndex = cursor.getColumnIndex(DbFields.FREQUENCY.name)
            serviceNumberIndex = cursor.getColumnIndex(DbFields.SERVICE_NUMBER.name)
            serviceNameIndex = cursor.getColumnIndex(DbFields.SERVICE_NAME.name)
            serviceTripIdIndex = cursor.getColumnIndex(DbFields.SERVICE_TRIP_ID.name)
            serviceColorRedIndex = cursor.getColumnIndex(DbFields.SERVICE_COLOR_RED.name)
            serviceColorBlueIndex = cursor.getColumnIndex(DbFields.SERVICE_COLOR_BLUE.name)
            serviceColorGreenIndex = cursor.getColumnIndex(DbFields.SERVICE_COLOR_GREEN.name)
            realTimeStatusIndex = cursor.getColumnIndex(DbFields.REAL_TIME_STATUS.name)
            favouriteIndex = cursor.getColumnIndex(DbFields.FAVOURITE.name)
            hasAlertsIndex = cursor.getColumnIndex(DbFields.HAS_ALERTS.name)
            searchStringIndex = cursor.getColumnIndex(DbFields.SEARCH_STRING.name)
            serviceTimeIndex = cursor.getColumnIndex(DbFields.SERVICE_TIME.name)
            serviceOperator = cursor.getColumnIndex(DbFields.SERVICE_OPERATOR.name)
            wheelchairAccessible = cursor.getColumnIndex(DbFields.WHEELCHAIR_ACCESSIBLE.name)
            bicycleAccessible = cursor.getColumnIndex(DbFields.BICYCLE_ACCESSIBLE.name)
            startStopShortName = cursor.getColumnIndex(DbFields.START_STOP_SHORT_NAME.name)
            startPlatform = cursor.getColumnIndex(DbFields.START_PLATFORM.name)
        }
    }
}