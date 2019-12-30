package com.skedgo.tripkit.ui.timetables

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.provider.ServiceStopsProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class FetchService @Inject constructor(private val context: Context,
                                       private val getWheelchairAccessible: GetWheelchairAccessible) {
  fun execute(service: TimetableEntry, stop: ScheduledStop): Completable {
    return Completable
        .fromAction {
          val serviceTripId = service.serviceTripId
          val region = TripKitUI.getInstance().regionService().getRegionByLocationAsync(stop)
                  .onErrorResumeNext(Observable.empty()).blockingFirst(null)
          val timeInSeconds = service.serviceTime
          val shapeValuesList = ArrayList<ContentValues>()
          val locationValuesList = ArrayList<ContentValues>()
          val stopValuesList = ArrayList<ContentValues>()
          val shapes = ServiceStopFetcher.getShapes(context, serviceTripId, region, timeInSeconds)

          val random = Random()
          for (shape in shapes.orEmpty()) {
            val shapeId = random.nextInt(Integer.MAX_VALUE)

            val shapeValues = ContentValues()
            shapeValues.put(DbFields.ID.name, shapeId)
            shapeValues.put(DbFields.SERVICE_TRIP_ID.name, serviceTripId)
            shapeValues.put(DbFields.TRAVELLED.name, if (shape.isTravelled) 1 else 0)
            shapeValues.put(DbFields.HAS_SERVICE_STOPS.name, if (shape.stops != null && !shape.stops!!.isEmpty()) 1 else 0)
            shapeValues.put(DbFields.SERVICE_COLOR.name, if (shape.serviceColor == null) Color.BLACK else shape.serviceColor.color)
            shapeValues.put(DbFields.WAYPOINT_ENCODING.name, shape.encodedWaypoints)

            shapeValuesList.add(shapeValues)
            if (shape.stops != null && shape.stops!!.isNotEmpty()) {
              for (serviceStop in shape.stops!!) {
                val serviceId = random.nextInt(Integer.MAX_VALUE)

                val stopValues = ContentValues()
                stopValues.put(DbFields.SERVICE_SHAPE_ID.name, shapeId)
                stopValues.put(DbFields.ID.name, serviceId)
                stopValues.put(DbFields.STOP_CODE.name, serviceStop.code)
                stopValues.put(DbFields.DEPARTURE_TIME.name, serviceStop.departureSecs())
                stopValues.put(DbFields.ARRIVAL_TIME.name, serviceStop.arrivalTime)
                stopValues.put(DbFields.STOP_TYPE.name, if (serviceStop.type == null) null else serviceStop.type.toString())
                stopValues.put(DbFields.WHEELCHAIR_ACCESSIBLE.name, getWheelchairAccessible.invoke(serviceStop))

                var timeToUse = serviceStop.departureSecs()
                if (timeToUse == 0L) {
                  timeToUse = serviceStop.arrivalTime
                }

                stopValues.put(DbFields.JULIAN_DAY.name, if (timeToUse == 0L) 0 else TimeUtils.getJulianDay(timeToUse * TimeUtils.InMillis.SECOND))

                stopValuesList.add(stopValues)

                val locationValues = ContentValues()
                locationValues.put(DbFields.SERVICE_STOP_ID.name, serviceId)
                locationValues.put(DbFields.NAME.name, serviceStop.name)
                locationValues.put(DbFields.ADDRESS.name, serviceStop.address)
                locationValues.put(DbFields.LAT.name, serviceStop.lat)
                locationValues.put(DbFields.LON.name, serviceStop.lon)
                locationValues.put(DbFields.BEARING.name, serviceStop.bearing)
                locationValues.put(DbFields.LOCATION_TYPE.name, Location.TYPE_SERVICE_STOP)
                locationValues.put(DbFields.EXACT.name, 1)
                locationValues.put(DbFields.IS_DYNAMIC.name, 0)

                locationValuesList.add(locationValues)
              }
            }
          }
          // FIXME These db operations need to be in a single transaction
          if (!shapeValuesList.isEmpty()) {
            context.contentResolver.bulkInsert(ServiceStopsProvider.SHAPES_URI, shapeValuesList.toTypedArray())
          }

          if (!stopValuesList.isEmpty()) {
            context.contentResolver.bulkInsert(ServiceStopsProvider.STOPS_URI, stopValuesList.toTypedArray())
          }

          if (!locationValuesList.isEmpty()) {
            context.contentResolver.bulkInsert(ServiceStopsProvider.LOCATIONS_URI, locationValuesList.toTypedArray())
          }

          context.contentResolver.notifyChange(ServiceStopsProvider.STOPS_BY_SERVICE_URI, null)
        }
        .subscribeOn(Schedulers.io())
  }
}
