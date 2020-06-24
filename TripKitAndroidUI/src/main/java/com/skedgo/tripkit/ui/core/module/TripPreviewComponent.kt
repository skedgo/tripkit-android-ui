package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = arrayOf(TripPreviewModule::class))
interface TripPreviewComponent {
    fun inject(fragment: ModeLocationTripPreviewItemFragment)
    fun inject(fragment: NearbyTripPreviewItemFragment)
    fun inject(fragment: ExternalActionTripPreviewItemFragment)
}