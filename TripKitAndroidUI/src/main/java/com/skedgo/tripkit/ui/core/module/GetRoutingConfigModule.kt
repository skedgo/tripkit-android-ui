package skedgo.tripgo.agenda.legacy

import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routing.settings.GetRoutingConfigImpl
import dagger.Module
import dagger.Provides

@Module class GetRoutingConfigModule {
  @Provides internal fun getRoutingConfig(getRoutingConfigImpl: GetRoutingConfigImpl)
      : GetRoutingConfig = getRoutingConfigImpl
}