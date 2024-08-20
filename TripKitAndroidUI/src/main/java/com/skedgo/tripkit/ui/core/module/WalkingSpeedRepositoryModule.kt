package skedgo.tripgo.agenda.legacy

import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class WalkingSpeedRepositoryModule {
    @Provides
    fun walkingSpeedRepository(
        resources: Resources,
        prefs: SharedPreferences
    ): WalkingSpeedRepository = WalkingSpeedRepositoryImpl(resources, prefs)
}