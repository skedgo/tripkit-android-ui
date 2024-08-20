package com.skedgo.tripkit.ui.core.module

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class CyclingSpeedRepositoryModule {
    @Provides
    fun cyclingSpeedRepository(
        resources: Resources,
        prefs: SharedPreferences
    ): CyclingSpeedRepository = CyclingSpeedRepositoryImpl(resources, prefs)
}
