package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routeinput.RouteInputViewModel
import dagger.Module
import dagger.Provides


@Module
class RouteInputViewModule {
    @Provides
    internal fun routeInputViewModel()
            : RouteInputViewModel = RouteInputViewModel()

}