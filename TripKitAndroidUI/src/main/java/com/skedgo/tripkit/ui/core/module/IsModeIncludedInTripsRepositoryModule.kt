package com.skedgo.tripkit.ui.core.module
import android.content.Context
import android.content.SharedPreferences
import com.skedgo.tripkit.ui.core.modeprefs.IsModeIncludedInTripsRepositoryImpl
import com.skedgo.tripkit.ui.routingresults.IsModeIncludedInTripsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class IsModeIncludedInTripsRepositoryModule {
  companion object {
    const val IsModeIncludedInTripsPrefs = "IsModeIncludedInTripsPrefs"
  }

  @Provides
  internal fun isModeIncludedInTripsRepository(impl: IsModeIncludedInTripsRepositoryImpl)
      : IsModeIncludedInTripsRepository = impl

  @Provides
  @Named(IsModeIncludedInTripsPrefs)
  fun isModeIncludedInTripsSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(
        "IsModeIncludedInTripsPrefs",
        Context.MODE_PRIVATE
    )
  }
}