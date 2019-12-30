package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.ui.routing.settings.CyclingSpeed
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeed
import com.skedgo.tripkit.ui.routing.settings.WeightingProfile
import org.joda.time.Minutes

data class RoutingConfig(
        val preferredTransferTime: Minutes = Minutes.THREE,
        val walkingSpeed: WalkingSpeed = WalkingSpeed.Medium,
        val cyclingSpeed: CyclingSpeed = CyclingSpeed.Medium,
        val shouldUseConcessionPricing: Boolean = false,
        val isOnWheelchair: Boolean = false,
        val weightingProfile: WeightingProfile = WeightingProfile()
)
