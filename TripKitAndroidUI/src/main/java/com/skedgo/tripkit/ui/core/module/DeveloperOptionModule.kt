package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.core.settings.DeveloperPreferenceRepository
import com.skedgo.tripkit.ui.core.settings.DeveloperPreferenceRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class DeveloperOptionModule {
    @Provides
    fun developerPreferenceRepository(context: Context): DeveloperPreferenceRepository {
        val preferences = context.getSharedPreferences(
                "DeveloperPreferences2",
                Context.MODE_PRIVATE
        )
        return DeveloperPreferenceRepositoryImpl(context, preferences)
    }
}