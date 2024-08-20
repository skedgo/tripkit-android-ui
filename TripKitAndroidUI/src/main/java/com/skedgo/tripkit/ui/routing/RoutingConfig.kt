package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.ui.routing.settings.CyclingSpeed
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeed
import com.skedgo.tripkit.ui.routing.settings.WeightingProfile
import java.time.Duration

data class RoutingConfig(
    val preferredTransferTime: Duration = Duration.ofMinutes(3),
    val walkingSpeed: WalkingSpeed = WalkingSpeed.Medium,
    val cyclingSpeed: CyclingSpeed = CyclingSpeed.Medium,
    val unit: String = "auto",
    val shouldUseConcessionPricing: Boolean = false,
    val isOnWheelchair: Boolean = false,
    val weightingProfile: WeightingProfile = WeightingProfile()
)
