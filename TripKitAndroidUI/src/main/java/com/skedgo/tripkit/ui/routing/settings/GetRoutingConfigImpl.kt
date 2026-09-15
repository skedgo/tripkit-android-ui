package com.skedgo.tripkit.ui.routing.settings

import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import com.skedgo.tripkit.ui.routing.RoutingConfig
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import javax.inject.Inject

internal class GetRoutingConfigImpl @Inject constructor(
    private val walkingSpeedRepository: WalkingSpeedRepository,
    private val cyclingSpeedRepository: CyclingSpeedRepository,
    private val rollingSpeedRepository: RollingSpeedRepository,
    private val unitsRepository: UnitsRepository,
    private val tripPreferences: TripPreferences,
    private val preferredTransferTimeRepository: PreferredTransferTimeRepository,
    private val prioritiesRepository: PrioritiesRepository,
    private val transportModeSharedPreference: TransportModeSharedPreference
) : GetRoutingConfig {
    override suspend fun execute(): RoutingConfig {
        return RoutingConfig(
            preferredTransferTime = preferredTransferTimeRepository.getPreferredTransferTime(),
            walkingSpeed = walkingSpeedRepository.getWalkingSpeed(),
            unit = unitsRepository.getUnit(),
            cyclingSpeed = cyclingSpeedRepository.getCyclingSpeed(),
            rollingSpeed = rollingSpeedRepository.getRollingSpeed(),
            shouldUseConcessionPricing = tripPreferences.isConcessionPricingPreferred(),
            // The wheelchair flag must follow the wheelchair *transport mode* selection, exactly
            // like the A2B routing request does in TripResultListViewModel.load(). Reading
            // TripPreferences.isWheelchairPreferred() here made a walking trip come back as a
            // wheelchair trip after picking a service from the timetable card.
            isOnWheelchair = transportModeSharedPreference.isWheelchairModeSelected(),
            weightingProfile = WeightingProfile(
                budgetPriority = prioritiesRepository.getBudgetPriority(),
                environmentPriority = prioritiesRepository.getEnvironmentPriority(),
                timePriority = prioritiesRepository.getTimePriority(),
                conveniencePriority = prioritiesRepository.getConveniencePriority(),
                exercisePriority = prioritiesRepository.getExercisePriority()
            )
        )
    }
}