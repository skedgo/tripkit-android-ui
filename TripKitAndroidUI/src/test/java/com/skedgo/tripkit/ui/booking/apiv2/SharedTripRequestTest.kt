package com.skedgo.tripkit.ui.booking.apiv2

import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import com.skedgo.tripkit.AddCustomHeaders
import com.skedgo.tripkit.configuration.GetAppVersion
import com.skedgo.tripkit.configuration.Key
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class SharedTripRequestTest {
    @Test fun `shared trip requests JSON alongside the standard wildcard accept header`(): Unit = runBlocking {
        val version = mockk<GetAppVersion>()
        every { version.execute() } returns Observable.just("test")
        val captured = CompletableDeferred<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(AddCustomHeaders(version, { Locale.US }, null, null, { Key.ApiKey("test") }, null))
            .addInterceptor { chain ->
                captured.complete(chain.request())
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body("{\"groups\":[]}".toResponseBody("application/json".toMediaType())).build()
            }.build()
        try {
            val api = Retrofit.Builder().baseUrl("https://tripgo.com/")
                .client(client).addCallAdapterFactory(NetworkResponseAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(BookingV2TrackingApi::class.java)
            api.getTripGroup("https://tripgo.com/trip/saved-id?original=kept", mapOf("v" to "11"))
            val request = captured.await()
            assertThat(request.headers.values("Accept")).contains("application/json", "*/*")
            assertThat(request.url.encodedPath).isEqualTo("/trip/saved-id")
            assertThat(request.url.queryParameter("original")).isEqualTo("kept")
            assertThat(request.url.queryParameter("v")).isEqualTo("11")
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
}
