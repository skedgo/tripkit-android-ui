package com.skedgo.tripkit.ui.timetables.data

import com.skedgo.tripkit.ui.model.DeparturesResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DeparturesApi {
  @POST
  fun request(@Url url: String, @Body departureRequestBody: DepartureRequestBody): Single<DeparturesResponse>
}