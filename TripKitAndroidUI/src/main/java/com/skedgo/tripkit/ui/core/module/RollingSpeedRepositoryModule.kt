package com.skedgo.tripkit.ui.core.module

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepositoryImpl
import com.skedgo.tripkit.ui.routing.settings.RollingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.RollingSpeedRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class RollingSpeedRepositoryModule {
    @Provides
    fun rollingSpeedRepository(
        resources: Resources,
        prefs: SharedPreferences
    ): RollingSpeedRepository = RollingSpeedRepositoryImpl(resources, prefs)
}
