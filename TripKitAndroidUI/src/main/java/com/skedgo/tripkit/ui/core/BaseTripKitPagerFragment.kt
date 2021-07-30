package com.skedgo.tripkit.ui.core

open class BaseTripKitPagerFragment: BaseTripKitFragment() {

    internal var onPreviousPage: (() -> Unit)? = null
    internal var onNextPage: (() -> Unit)? = null

}