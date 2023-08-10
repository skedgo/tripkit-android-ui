package com.skedgo.tripkit.ui.locationpointer

import com.skedgo.tripkit.ui.core.module.ActivityScope
import dagger.Subcomponent


@ActivityScope
@Subcomponent
interface LocationPointerComponent {
    fun inject(fragment: LocationPointerFragment)
}