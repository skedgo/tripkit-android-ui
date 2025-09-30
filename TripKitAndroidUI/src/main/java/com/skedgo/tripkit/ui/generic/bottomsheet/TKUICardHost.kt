package com.skedgo.tripkit.ui.generic.bottomsheet

import com.google.android.gms.maps.GoogleMap

interface TKUICardHost {
    fun getBottomSheetCardManager(): TKUICardViewControllerManager
    fun popBackStack(immediate: Boolean = false, tag: String? = null)
    fun getMap(): GoogleMap?
}