package com.skedgo.tripkit.ui.data.realtime

import com.skedgo.tripkit.common.agenda.IRealTimeElement
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.realtime.RealTimeRepository
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

private const val LATEST_ENDPOINT = "latest.json"

class RealTimeRepositoryImpl @Inject constructor(
    private val latestApi: LatestApi,
    private val regionService: RegionService
) : RealTimeRepository {

    override fun getUpdates(
        region: String,
        elements: List<IRealTimeElement>
    ): Single<List<RealTimeVehicle>> =
        regionService.getRegionByNameAsync(region)
            .flatMap {
                Observable.fromIterable(it.urLs)
            }
            .map {
                it.toHttpUrlOrNull()!!
                    .newBuilder()
                    .addPathSegment(LATEST_ENDPOINT)
                    .build()
                    .toString()
            }
            .map { url ->
                val latestServices = elements.map {
                    ImmutableLatestService.builder()
                        .operator(it.operator)
                        .serviceTripID(it.serviceTripId)
                        .startStopCode(it.startStopCode)
                        .endStopCode(it.endStopCode)
                        .startTime(it.startTimeInSecs)
                        .build()
                }

                val requestBody = ImmutableLatestRequestBody.builder()
                    .region(region)
                    .services(latestServices)
                    .build()

                latestApi.request(url, requestBody)
            }
            .scan { a, b -> a.onErrorResumeNext { b } }
            .lastOrError()
            .flatMap { it }
            .map { latestResponse ->
                latestResponse.services().map {
                    it.toRealTimeVehicle()
                }
            }
}