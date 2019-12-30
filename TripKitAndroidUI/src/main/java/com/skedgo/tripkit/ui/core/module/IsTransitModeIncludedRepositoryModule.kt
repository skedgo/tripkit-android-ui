package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.skedgo.tripkit.ui.core.modeprefs.IsTransitModeIncludedRepository
import com.skedgo.tripkit.ui.core.modeprefs.IsTransitModeIncludedRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class IsTransitModeIncludedRepositoryModule {
  @Provides
  fun isTransitModeIncludedRepository(context: Context): IsTransitModeIncludedRepository =
      IsTransitModeIncludedRepositoryImpl(
          context.getSharedPreferences(
              "IsTransitModeIncludedPrefs",
              Context.MODE_PRIVATE
          )
      )
}
