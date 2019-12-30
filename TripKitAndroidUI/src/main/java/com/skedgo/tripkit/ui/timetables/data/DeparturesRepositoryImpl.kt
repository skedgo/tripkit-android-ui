package com.skedgo.tripkit.ui.timetables.data

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
      timeInSecs: Long
  ): Single<DeparturesResponse> =
      regionService.getRegionByNameAsync(region)
          .flatMap { Observable.fromIterable(it.urLs) }
          .map {
            it.toHttpUrlOrNull()!!
                .newBuilder()
                .addPathSegment(DEPARTURES_ENDPOINT)
                .build()
                .toString()
          }
          .map { url ->
              // TODO Likely need to provide a configuration - see TripGo's ConfigCreator
            val requestBody = ImmutableDepartureRequestBody.builder()
                .embarkationStops(embarkationStopCodes)
                .disembarkationStops(disembarkationStopCodes)
                .regionName(region)
                .timeInSecs(timeInSecs)
                .build()

            departuresApi.request(url, requestBody)
          }
          .scan { a, b -> a.onErrorResumeNext { b } }
          .lastOrError()
          .flatMap { it }
          .map { it.postProcess(embarkationStopCodes, disembarkationStopCodes) }
}