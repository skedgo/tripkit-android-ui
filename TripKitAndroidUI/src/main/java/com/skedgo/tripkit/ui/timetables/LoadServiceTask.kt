package com.skedgo.tripkit.ui.timetables

import android.database.Cursor
import android.util.SparseArray
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus.Companion.from
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask.ServiceLineInfo
import java.util.concurrent.Callable
import kotlin.Int
import kotlin.String

class LoadServiceTask(private val mStop: ScheduledStop?, private val mCursor: Cursor?) : Callable<Pair<List<StopInfo>, List<ServiceLineInfo>>> {
    private val mIdToLatLngArray = SparseArray<Pair<Int, List<LatLng>>>()

    fun getStopFor(code: String): ScheduledStop? {
        return when {
            mStop?.code == code -> mStop
            mStop?.hasChildren() == true -> mStop.children?.find { it.code == code }
            else -> null
        }
    }

    override fun call(): Pair<List<StopInfo>, List<ServiceLineInfo>> {
        if (mCursor == null || mCursor.count == 0) {
            return Pair(emptyList(), emptyList())
        }

        val stopInfoList = mutableListOf<StopInfo>()
        if (LoadServiceTaskCursorCols.id < 0) {
            LoadServiceTaskCursorCols.init(mCursor)
        }
        val alreadyAddedStopCodes = mutableSetOf<String>()

        repeat(mCursor.count) {
            mCursor.moveToPosition(it)
            val stopCode = mCursor.getString(LoadServiceTaskCursorCols.stopCode)
            if (!alreadyAddedStopCodes.add(stopCode)) return@repeat

            val id = mCursor.getInt(LoadServiceTaskCursorCols.id)
            val address = mCursor.getString(LoadServiceTaskCursorCols.address)
            val name = mCursor.getString(LoadServiceTaskCursorCols.name)
            val bearing = mCursor.getInt(LoadServiceTaskCursorCols.bearing)
            val realTimeStatus = from(mCursor.getString(LoadServiceTaskCursorCols.realTimeStatus))
            val depart = mCursor.getLong(LoadServiceTaskCursorCols.departureTime)
            val arrive = mCursor.getLong(LoadServiceTaskCursorCols.arrivalTime)
            val color = mCursor.getInt(LoadServiceTaskCursorCols.serviceColor)
            val latitude = mCursor.getDouble(LoadServiceTaskCursorCols.lat)
            val longitude = mCursor.getDouble(LoadServiceTaskCursorCols.lon)
            val wheelchairAccessible = mCursor.getInt(LoadServiceTaskCursorCols.wheelchairAccessible)

            val stop = ServiceStop().apply {
                lat = latitude
                lon = longitude
                this.bearing = bearing
                this.name = name
                this.address = address
                this.code = stopCode
                this.setDepartureSecs(depart)
                this.arrivalTime = arrive
                this.setWheelchairAccessible(wheelchairAccessible.takeIf { it != -1 }?.let { it == 1 })
                this.type = mStop?.let { getStopFor(stopCode)?.type }
            }

            val sortByArrive = depart == 0L || (mCursor.position == 0 && depart != 0L)

            if (mIdToLatLngArray.get(id) == null) {
                val waypointEncoding = mCursor.getString(LoadServiceTaskCursorCols.waypoints)
                if (!waypointEncoding.isNullOrEmpty()) {
                    mIdToLatLngArray.put(id, Pair(color, PolyUtil.decode(waypointEncoding)))
                }
            }

            stopInfoList.add(StopInfo(id, realTimeStatus, sortByArrive, stop, color, false))
        }

        stopInfoList.sortBy { if (it.sortByArrive) it.stop.arrivalTime else it.stop.departureSecs() }
        calculateTravelledInfo(stopInfoList)
        val serviceLineInfos = getServiceLineInfos()

        return Pair(stopInfoList, serviceLineInfos)
    }

    private fun getServiceLineInfos(): List<ServiceLineInfo> {
        val serviceLineInfos = mutableListOf<ServiceLineInfo>()
        var travelled = false

        for (i in 0 until mIdToLatLngArray.size()) {
            val value = mIdToLatLngArray.valueAt(i)
            val index = value.second.indexOfFirst { Location(it.latitude, it.longitude).distanceTo(mStop) < 10 }

            if (index == -1) {
                serviceLineInfos.add(ServiceLineInfo(value.second, value.first, travelled))
            } else {
                serviceLineInfos.add(ServiceLineInfo(value.second.subList(0, index + 1), value.first, false))
                serviceLineInfos.add(ServiceLineInfo(value.second.subList(index, value.second.size), value.first, true))
            }
            travelled = true
        }
        return serviceLineInfos
    }

    private fun calculateTravelledInfo(stopInfoList: List<StopInfo>) {
        var travelled = false
        stopInfoList.forEach {
            if (it.stop.code == mStop?.code) travelled = true
            it.travelled = travelled
        }
    }
}