package com.skedgo.tripkit.ui.core.module

//import com.skedgo.tripgo.sdk.booking.TripPreviewTicketFooterFragment
//import com.skedgo.tripgo.sdk.booking.drt.DrtFragment
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.ModeLocationTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.nearby.NearbyTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.standard.StandardTripPreviewItemFragment
import com.skedgo.tripkit.ui.trippreview.v2.TripPreviewParentFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [ServiceDetailsModule::class])
interface TripPreviewComponent {
    fun inject(fragment: StandardTripPreviewItemFragment)
    fun inject(fragment: ModeLocationTripPreviewItemFragment)
    fun inject(fragment: NearbyTripPreviewItemFragment)
    fun inject(fragment: ExternalActionTripPreviewItemFragment)
    fun inject(fragment: ServiceTripPreviewItemFragment)
    fun inject(fragment: TripPreviewParentFragment)
}