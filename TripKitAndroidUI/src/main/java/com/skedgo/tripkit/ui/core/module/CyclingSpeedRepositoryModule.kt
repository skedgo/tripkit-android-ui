package skedgo.tripgo.agenda.legacy

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import dagger.Module
import dagger.Provides

@Module
class CyclingSpeedRepositoryModule {
  @Provides fun cyclingSpeedRepository(
      resources: Resources,
      prefs: SharedPreferences
  ): CyclingSpeedRepository
      = CyclingSpeedRepositoryImpl(resources, prefs)
}
