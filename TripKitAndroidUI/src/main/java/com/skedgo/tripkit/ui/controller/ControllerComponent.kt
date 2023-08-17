package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeBottomSheetFragment
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.routeviewcontroller.TKUIRouteFragment
import com.skedgo.tripkit.ui.controller.timetableviewcontroller.TKUITimetableControllerFragment
import com.skedgo.tripkit.ui.controller.tripdetailsviewcontroller.TKUITripDetailsViewControllerFragment
import com.skedgo.tripkit.ui.controller.trippreviewcontroller.TKUITripPreviewFragment
import com.skedgo.tripkit.ui.controller.tripresultcontroller.TKUITripResultsFragment
import com.skedgo.tripkit.ui.core.module.ActivityScope
import com.skedgo.tripkit.ui.core.module.LocationStuffModule
import com.skedgo.tripkit.ui.core.module.ServiceDetailsModule
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [LocationStuffModule::class, ServiceDetailsModule::class])
interface ControllerComponent {
    fun inject(fragment: TKUIHomeViewControllerFragment)
    fun inject(fragment: TKUIHomeBottomSheetFragment)
    fun inject(fragment: TKUILocationSearchViewControllerFragment)
    fun inject(fragment: TKUITimetableControllerFragment)
    fun inject(fragment: TKUIRouteFragment)
    fun inject(fragment: TKUITripResultsFragment)
    fun inject(fragment: TKUITripDetailsViewControllerFragment)
    fun inject(fragment: TKUITripPreviewFragment)
}