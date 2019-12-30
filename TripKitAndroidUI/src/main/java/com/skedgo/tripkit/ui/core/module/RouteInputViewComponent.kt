package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.routeinput.RouteInputView
import dagger.Subcomponent

@Subcomponent(modules = [RouteInputViewModule::class])
interface RouteInputViewComponent {
  fun inject(view: RouteInputView)
}