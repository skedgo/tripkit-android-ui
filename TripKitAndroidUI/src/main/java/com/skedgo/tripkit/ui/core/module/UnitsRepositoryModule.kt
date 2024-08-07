package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.routing.settings.UnitsRepository
import com.skedgo.tripkit.ui.routing.settings.UnitsRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class UnitsRepositoryModule {
    @Provides
    fun unitsRepository(
        context: Context
    ): UnitsRepository = UnitsRepositoryImpl(
        context.resources,
        context.getSharedPreferences("tripgo_preferences", Context.MODE_PRIVATE)
    )
}
