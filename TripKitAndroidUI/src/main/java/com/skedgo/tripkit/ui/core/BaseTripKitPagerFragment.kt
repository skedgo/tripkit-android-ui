package com.skedgo.tripkit.ui.core

open class BaseTripKitPagerFragment: BaseTripKitFragment() {

    var onPreviousPage: (() -> Unit)? = null
    var onNextPage: (() -> Unit)? = null

}