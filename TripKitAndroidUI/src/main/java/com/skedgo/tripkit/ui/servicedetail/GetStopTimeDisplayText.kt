package com.skedgo.tripkit.ui.servicedetail

import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.data.regions.RegionService
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import com.skedgo.tripkit.datetime.PrintTime
import javax.inject.Inject

open class GetStopTimeDisplayText @Inject constructor(
    val regionService: RegionService,
    val printTime: PrintTime
) {

    open fun execute(stop: ServiceStop): Observable<String> {
        return Observable
            .fromCallable { stop.displayTime }
            .let {
                Observable.combineLatest(it,
                    regionService.getRegionByLocationAsync(stop),
                    BiFunction
                    { time: Long, region: Region -> time to region })
            }
            .flatMap { (time, region) ->
                printTime.execute(DateTime(time, DateTimeZone.forID(region.timezone)))
                    .toObservable()
            }

    }
}