package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import dagger.Module
import dagger.Provides

@Module
class TripKitPreferenceModule {
    @Provides
    internal fun tripGoSharedPreference(context: Context): TransportModeSharedPreference {
        return TransportModeSharedPreference(context)
    }
}