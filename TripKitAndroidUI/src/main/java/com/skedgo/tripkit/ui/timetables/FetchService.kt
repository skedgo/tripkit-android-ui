package com.skedgo.tripkit.ui.timetables

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.Pair
import android.util.SparseArray
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask.ServiceLineInfo
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.Random
import javax.inject.Inject

class FetchService @Inject constructor(
    private val context: Context,
    private val getModeAccessibility: GetModeAccessibility
) {
    fun execute(service: TimetableEntry, stop: Location): Completable {
        return Completable
            .fromAction {
                val serviceTripId = service.serviceTripId
                val region = TripKitUI.getInstance().regionService().getRegionByLocationAsync(stop)
                    .onErrorResumeNext(Observable.empty()).blockingFirst(null)
                val timeInSeconds = service.serviceTime
                val shapeValuesList = ArrayList<ContentValues>()
                val locationValuesList = ArrayList<ContentValues>()
                val stopValuesList = ArrayList<ContentValues>()
                val shapes =
                    ServiceStopFetcher.getShapes(context, serviceTripId, region, timeInSeconds)

                val random = Random()
                for (shape in shapes.orEmpty()) {
                    val shapeId = random.nextInt(Integer.MAX_VALUE)

                    val shapeValues = ContentValues()
                    shapeValues.put(DbFields.ID.name, shapeId)
                    shapeValues.put(DbFields.SERVICE_TRIP_ID.name, serviceTripId)
                    shapeValues.put(DbFields.TRAVELLED.name, if (shape.isTravelled) 1 else 0)
                    shapeValues.put(
                        DbFields.HAS_SERVICE_STOPS.name,
                        if (shape.stops != null && !shape.stops!!.isEmpty()) 1 else 0
                    )
                    shapeValues.put(
                        DbFields.SERVICE_COLOR.name,
                        if (shape.serviceColor == null) Color.BLACK else shape.serviceColor.color
                    )
                    shapeValues.put(DbFields.WAYPOINT_ENCODING.name, shape.encodedWaypoints)

                    shapeValuesList.add(shapeValues)
                    if (shape.stops?.isNotEmpty() == true) {
                        for (serviceStop in shape.stops!!) {
                            val serviceId = random.nextInt(Integer.MAX_VALUE)

                            val stopValues = ContentValues()
                            stopValues.put(DbFields.SERVICE_SHAPE_ID.name, shapeId)
                            stopValues.put(DbFields.ID.name, serviceId)
                            stopValues.put(DbFields.STOP_CODE.name, serviceStop.code)
                            stopValues.put(
                                DbFields.DEPARTURE_TIME.name,
                                serviceStop.departureSecs()
                            )
                            stopValues.put(DbFields.ARRIVAL_TIME.name, serviceStop.arrivalTime)
                            stopValues.put(
                                DbFields.STOP_TYPE.name,
                                if (serviceStop.type == null) null else serviceStop.type.toString()
                            )
                            stopValues.put(
                                DbFields.WHEELCHAIR_ACCESSIBLE.name,
                                getModeAccessibility.wheelchair(serviceStop)
                            )
                            stopValues.put(
                                DbFields.BICYCLE_ACCESSIBLE.name,
                                getModeAccessibility.bicycle(serviceStop)
                            )

                            var timeToUse = serviceStop.departureSecs()
                            if (timeToUse == 0L) {
                                timeToUse = serviceStop.arrivalTime
                            }

                            stopValues.put(
                                DbFields.JULIAN_DAY.name,
                                if (timeToUse == 0L) 0 else TimeUtils.getJulianDay(timeToUse * TimeUtils.InMillis.SECOND)
                            )

                            stopValuesList.add(stopValues)

                            val locationValues = ContentValues()
                            locationValues.put(DbFields.SERVICE_STOP_ID.name, serviceId)
                            locationValues.put(DbFields.NAME.name, serviceStop.name)
                            locationValues.put(DbFields.ADDRESS.name, serviceStop.address)
                            locationValues.put(DbFields.LAT.name, serviceStop.lat)
                            locationValues.put(DbFields.LON.name, serviceStop.lon)
                            locationValues.put(DbFields.BEARING.name, serviceStop.bearing)
                            locationValues.put(
                                DbFields.LOCATION_TYPE.name,
                                Location.TYPE_SERVICE_STOP
                            )
                            locationValues.put(DbFields.EXACT.name, 1)
                            locationValues.put(DbFields.IS_DYNAMIC.name, 0)

                            locationValuesList.add(locationValues)
                        }
                    }
                }
                // FIXME These db operations need to be in a single transaction
                if (!shapeValuesList.isEmpty()) {
                    context.contentResolver.bulkInsert(
                        ServiceStopsProvider.SHAPES_URI,
                        shapeValuesList.toTypedArray()
                    )
                }

                if (!stopValuesList.isEmpty()) {
                    context.contentResolver.bulkInsert(
                        ServiceStopsProvider.STOPS_URI,
                        stopValuesList.toTypedArray()
                    )
                }

                if (!locationValuesList.isEmpty()) {
                    context.contentResolver.bulkInsert(
                        ServiceStopsProvider.LOCATIONS_URI,
                        locationValuesList.toTypedArray()
                    )
                }

                context.contentResolver.notifyChange(
                    ServiceStopsProvider.STOPS_BY_SERVICE_URI,
                    null
                )
            }
            .subscribeOn(Schedulers.io())
    }

    private val mIdToLatLngArray = SparseArray<Pair<Int, List<LatLng>>>()

    fun executeWithResponse(
        service: TimetableEntry,
        stop: ScheduledStop
    ): Single<Pair<List<StopInfo>, List<ServiceLineInfo>>> {
        val serviceTripId = service.serviceTripId
        val region = TripKitUI.getInstance().regionService().getRegionByLocationAsync(stop)
            .onErrorResumeNext(Observable.empty()).blockingFirst(null)
        val timeInSeconds = service.serviceTime
        val shapes =
            ServiceStopFetcher.getShapes(context, serviceTripId, region, timeInSeconds)

        val stopInfoList = mutableListOf<StopInfo>()

        for (shape in shapes.orEmpty()) {
            if (shape.stops?.isNotEmpty() == true) {

                shape.stops?.forEachIndexed { index, stopItem ->
                    var sortByArrive = true
                    if (index == 0 && stopItem.departureSecs() != 0L) {
                        sortByArrive = false
                    } else {
                        if (stopItem.departureSecs() != 0L) {
                            sortByArrive = false
                        }
                    }

                    if (getStopFor(stop, stopItem.code) != null) {
                        stopItem.type = stop.type
                    }

                    if (mIdToLatLngArray.get(stopItem.id.toInt()) == null) {
                        val waypointEncoding: String = shape.encodedWaypoints
                        if (waypointEncoding.isNotEmpty()) {
                            mIdToLatLngArray.put(
                                stopItem.id.toInt(),
                                Pair<Int, List<LatLng>>(
                                    shape.serviceColor.color,
                                    PolyUtil.decode(waypointEncoding)
                                )
                            )
                        }
                    }

                    stopInfoList.add(
                        StopInfo(
                            stopItem.id.toInt(),
                            null,
                            sortByArrive,
                            stopItem,
                            shape.serviceColor.color,
                            false
                        )
                    )
                }
            }
        }

        stopInfoList.sortWith(Comparator { lhs: StopInfo, rhs: StopInfo ->
            val val1 =
                if (lhs.sortByArrive) lhs.stop.arrivalTime else lhs.stop.departureSecs()
            val val2 =
                if (rhs.sortByArrive) rhs.stop.arrivalTime else rhs.stop.departureSecs()
            (val1 - val2).toInt()
        })

        calculateTravelledInfo(stop, stopInfoList)

        val serviceLineInfos = getServiceLineInfos(stop)

        return Single.just(Pair(stopInfoList, serviceLineInfos))
    }

    private fun getStopFor(stop: ScheduledStop, code: String): ScheduledStop? {
        if (TextUtils.equals(stop.code, code)) {
            return stop
        } else if (stop.hasChildren()) {
            for (stopChild in stop.children) {
                if (TextUtils.equals(stopChild.code, code)) {
                    return stopChild
                }
            }
        }

        return null
    }

    private fun calculateTravelledInfo(stop: ScheduledStop, stopInfoList: List<StopInfo>) {
        var travelled = false
        for (stopInfo in stopInfoList) {
            if (stopInfo.stop.code == stop.code) {
                travelled = true
            }
            stopInfo.travelled = travelled
        }
    }

    private fun getServiceLineInfos(stop: ScheduledStop): List<ServiceLineInfo> {
        var travelled = false
        val serviceLineInfos: MutableList<ServiceLineInfo> = java.util.ArrayList()
        for (i in 0 until mIdToLatLngArray.size()) {
            val value = mIdToLatLngArray.valueAt(i)
            var index = -1
            for (j in value.second.indices) {
                val latLng = value.second[j]
                if (Location(
                        latLng.latitude,
                        latLng.longitude
                    ).distanceTo(stop) < 10 /* in meters */) {
                    index = j
                    travelled = true
                    break
                }
            }
            if (index == -1) {
                serviceLineInfos.add(ServiceLineInfo(value.second, value.first, travelled))
            } else {
                serviceLineInfos.add(
                    ServiceLineInfo(
                        value.second.subList(0, index + 1),
                        value.first,
                        false
                    )
                )
                serviceLineInfos.add(
                    ServiceLineInfo(
                        value.second.subList(index, value.second.size),
                        value.first,
                        true
                    )
                )
            }
        }
        return serviceLineInfos
    }
}
