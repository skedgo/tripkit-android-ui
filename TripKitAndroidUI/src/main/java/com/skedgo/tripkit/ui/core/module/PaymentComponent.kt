package com.skedgo.tripkit.ui.core.module

import com.skedgo.tripkit.ui.payment.PaymentSummaryFragment
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [ServiceDetailsModule::class])
interface PaymentComponent {
    fun inject(fragment: PaymentSummaryFragment)
}