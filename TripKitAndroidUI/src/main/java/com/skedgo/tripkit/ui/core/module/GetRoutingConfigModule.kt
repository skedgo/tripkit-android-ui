package skedgo.tripgo.agenda.legacy

import com.skedgo.tripkit.ui.core.module.CyclingSpeedRepositoryModule
import com.skedgo.tripkit.ui.core.module.PreferredTransferTimeRepositoryModule
import com.skedgo.tripkit.ui.core.module.UnitsRepositoryModule
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routing.settings.GetRoutingConfigImpl
import dagger.Module
import dagger.Provides

@Module(includes=[CyclingSpeedRepositoryModule::class,
  WalkingSpeedRepositoryModule::class,
  UnitsRepositoryModule::class,
  PreferredTransferTimeRepositoryModule::class])
class GetRoutingConfigModule {
  @Provides internal fun getRoutingConfig(getRoutingConfigImpl: GetRoutingConfigImpl)
      : GetRoutingConfig = getRoutingConfigImpl
}