package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class TripKitPreferenceModule {
    @Provides
    internal fun tripGoSharedPreference(context: Context): TransportModeSharedPreference {
        return TransportModeSharedPreference(context)
    }
}