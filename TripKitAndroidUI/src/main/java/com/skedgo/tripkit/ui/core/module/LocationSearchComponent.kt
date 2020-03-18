package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.search.LocationSearchFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent
interface LocationSearchComponent {
    fun inject(fragment: LocationSearchFragment)
}