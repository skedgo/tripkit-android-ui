package com.skedgo.tripkit.ui.core.module

import com.google.gson.Gson
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.ui.data.tripprogress.UpdateProgressApi
import com.skedgo.tripkit.ui.data.tripprogress.UpdateTripProgressImpl
import com.skedgo.tripkit.ui.tripprogress.UpdateTripProgress
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
class TripProgressModule {
    @Provides
    fun updateTripProgress(
        gson: Gson,
        httpClient: OkHttpClient
    ): UpdateTripProgress {
        val markTripAsPlannedApi: UpdateProgressApi = updateProgressApi(gson, httpClient)
        return UpdateTripProgressImpl(markTripAsPlannedApi)
    }

    private fun updateProgressApi(gson: Gson, httpClient: OkHttpClient): UpdateProgressApi =
        Retrofit.Builder()
            /* This base url is ignored as the api relies on @Url. */
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient)
            .build()
            .create(UpdateProgressApi::class.java)
}
