package com.skedgo.tripkit.ui.core.module

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import com.skedgo.tripkit.ui.tripresult.PreferredTransferTimeRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class PreferredTransferTimeRepositoryModule {
    @Provides
    fun preferredTransferTimeRepository(
        resources: Resources,
        prefs: SharedPreferences
    ): PreferredTransferTimeRepository = PreferredTransferTimeRepositoryImpl(resources, prefs)
}
