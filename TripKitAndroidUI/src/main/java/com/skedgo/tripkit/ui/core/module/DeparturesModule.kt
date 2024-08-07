package com.skedgo.tripkit.ui.core.module

import com.google.gson.Gson
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.timetables.data.DeparturesApi
import com.skedgo.tripkit.ui.timetables.data.DeparturesRepositoryImpl
import com.skedgo.tripkit.ui.timetables.domain.DeparturesRepository
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class DeparturesModule {

    @Provides
    @Singleton
    fun departuresRepository(impl: DeparturesRepositoryImpl): DeparturesRepository = impl

    @Provides
    internal fun departuresApi(
        gson: Gson,
        httpClient: OkHttpClient,
        schedulers: SchedulerFactory
    ): DeparturesApi = Retrofit.Builder()
        /* This base url is ignored as the api relies on @Url. */
        .baseUrl(ServerManager.configuration.apiTripGoUrl)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(schedulers.ioScheduler))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(httpClient)
        .build()
        .create(DeparturesApi::class.java)
}