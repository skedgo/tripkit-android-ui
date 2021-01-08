package com.skedgo.tripgo.sdk.agenda.data

import com.skedgo.tripkit.ui.data.ConfigDto
import com.skedgo.tripkit.ui.data.ConfigDtos
import com.skedgo.tripkit.ui.routing.RoutingConfig

fun RoutingConfig.toConfigDto(): ConfigDto = ConfigDtos.of(
    v = "11",
    tt = preferredTransferTime.toMinutes().toInt(),
    ws = walkingSpeed.value,
    cs = cyclingSpeed.value,
    conc = shouldUseConcessionPricing,
    wheelchair = isOnWheelchair,
    wp = weightingProfile.toWeightingProfileDto()
)