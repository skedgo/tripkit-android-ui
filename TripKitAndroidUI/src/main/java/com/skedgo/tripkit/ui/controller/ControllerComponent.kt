package com.skedgo.tripkit.ui.controller

import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeBottomSheetFragment
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.skedgo.tripkit.ui.controller.locationsearchcontroller.TKUILocationSearchViewControllerFragment
import com.skedgo.tripkit.ui.controller.timetableviewcontroller.TKUITimetableControllerFragment
import com.skedgo.tripkit.ui.core.module.ActivityScope
import dagger.Subcomponent

@ActivityScope
@Subcomponent
interface ControllerComponent {
    fun inject(fragment: TKUIHomeViewControllerFragment)
    fun inject(fragment: TKUIHomeBottomSheetFragment)
    fun inject(fragment: TKUILocationSearchViewControllerFragment)
    fun inject(fragment: TKUITimetableControllerFragment)
}