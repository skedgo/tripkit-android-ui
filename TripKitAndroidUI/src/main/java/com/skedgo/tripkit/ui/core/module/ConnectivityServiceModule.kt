package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.data.connectivity.ConnectivityService
import com.skedgo.tripkit.data.connectivity.ConnectivityServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ConnectivityServiceModule {
    @Provides
    fun connectivityService(context: Context): ConnectivityService =
        ConnectivityServiceImpl(context)
}
