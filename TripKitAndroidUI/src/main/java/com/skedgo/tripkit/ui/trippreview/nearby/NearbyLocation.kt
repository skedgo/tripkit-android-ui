package com.skedgo.tripkit.ui.trippreview.nearby

import com.skedgo.tripkit.routing.ModeInfo


data class NearbyLocation(val lat: Double,
                          val lng: Double,
                          val title: String?,
                          val address: String?,
                          val website: String?,
                          val modeInfo: ModeInfo?)