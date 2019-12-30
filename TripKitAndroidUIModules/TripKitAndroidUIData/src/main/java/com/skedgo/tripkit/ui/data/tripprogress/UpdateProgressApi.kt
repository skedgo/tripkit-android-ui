package com.skedgo.tripkit.ui.data.tripprogress
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import io.reactivex.Observable

interface UpdateProgressApi {
  /**
   * Note: Use [Void] just to ignore parsing empty response.
   * See https://stackoverflow.com/a/33228322/2563009.
   */
  @POST
  fun execute(@Url url: String, @Body body: UpdateProgressBody): Observable<Void>
}