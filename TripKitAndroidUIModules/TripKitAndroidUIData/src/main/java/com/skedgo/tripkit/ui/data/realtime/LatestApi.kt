package com.skedgo.tripkit.ui.data.realtime

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface LatestApi {
    @POST
    fun request(
        @Url url: String,
        @Body latestRequestBody: LatestRequestBody
    ): Single<LatestResponse>
}