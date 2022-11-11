package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routing.autocompleter.sample.RouteAutoCompleteSampleFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [ServiceDetailsModule::class, AutoCompleteRoutingModule::class])
interface AutoCompleteRoutingComponent {
    fun inject(fragment: RouteAutoCompleteSampleFragment)
}