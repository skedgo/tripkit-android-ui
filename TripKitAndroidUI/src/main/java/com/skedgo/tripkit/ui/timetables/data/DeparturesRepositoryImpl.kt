package com.skedgo.tripkit.ui.timetables.data

import com.skedgo.rxtry.printThrowableStackTrace
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.model.DeparturesResponse
import com.skedgo.tripkit.ui.timetables.domain.DeparturesRepository
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

private const val DEPARTURES_ENDPOINT = "departures.json"

class DeparturesRepositoryImpl @Inject constructor(
    private val departuresApi: DeparturesApi,
    private val regionService: RegionService
) : DeparturesRepository {

    override fun getTimetableEntries(
        region: String,
        embarkationStopCodes: List<String>,
        disembarkationStopCodes: List<String>?,
        timeInSecs: Long,
        limit: Int
    ): Single<DeparturesResponse> =
        regionService.getRegionByNameAsync(region)
            .flatMap { regionObj ->
                Observable.fromIterable(regionObj.getURLs() ?: emptyList())
                    .concatMapDelayError { baseUrl ->
                        val url = baseUrl.toHttpUrlOrNull()!!
                            .newBuilder()
                            .addPathSegment(DEPARTURES_ENDPOINT)
                            .build()
                            .toString()

                        val requestBody = ImmutableDepartureRequestBody.builder()
                            .embarkationStops(embarkationStopCodes)
                            .disembarkationStops(disembarkationStopCodes)
                            .regionName(region)
                            .limit(limit)
                            .timeInSecs(timeInSecs)
                            .includeStops(false)
                            .build()

                        departuresApi.request(url, requestBody)
                            .doOnError { it.printThrowableStackTrace() }
                            .toObservable()
                    }
            }
            .firstOrError()
            .map { it.postProcess(embarkationStopCodes, disembarkationStopCodes) }

    override fun getTimetableEntries(
        region: String,
        embarkationStopCodes: List<String>,
        disembarkationStopCodes: List<String>?,
        timeInSecs: Long,
        limit: Int,
        filters: List<DepartureFilter>,
        includeStops: Boolean
    ): Single<DeparturesResponse> =
        regionService.getRegionByNameAsync(region)
            .flatMap { regionObj ->
                Observable.fromIterable(regionObj.getURLs() ?: emptyList())
                    .concatMapDelayError { baseUrl ->
                        val url = baseUrl.toHttpUrlOrNull()!!
                            .newBuilder()
                            .addPathSegment(DEPARTURES_ENDPOINT)
                            .build()
                            .toString()

                        val requestBody = ImmutableDepartureRequestBody.builder()
                            .embarkationStops(embarkationStopCodes)
                            .disembarkationStops(disembarkationStopCodes)
                            .regionName(region)
                            .limit(limit)
                            .timeInSecs(timeInSecs)
                            .filters(filters)
                            .includeStops(includeStops)
                            .build()

                        departuresApi.request(url, requestBody)
                            .doOnError { it.printThrowableStackTrace() }
                            .toObservable()
                    }
            }
            .firstOrError()
            .map { it.postProcess(embarkationStopCodes, disembarkationStopCodes) }
}