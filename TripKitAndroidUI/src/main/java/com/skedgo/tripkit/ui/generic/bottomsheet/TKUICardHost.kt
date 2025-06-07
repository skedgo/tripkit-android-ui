package com.skedgo.tripkit.ui.generic.bottomsheet

interface TKUICardHost {
    fun getBottomSheetCardManager(): TKUICardViewControllerManager
    fun popBackStack()
}