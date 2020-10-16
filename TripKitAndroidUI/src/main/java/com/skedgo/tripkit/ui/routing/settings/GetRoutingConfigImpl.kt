package com.skedgo.tripkit.ui.routing.settings

import android.util.Log
import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import com.skedgo.tripkit.ui.routing.RoutingConfig
import javax.inject.Inject

internal class GetRoutingConfigImpl @Inject constructor(
        private val walkingSpeedRepository: WalkingSpeedRepository,
        private val cyclingSpeedRepository: CyclingSpeedRepository,
        private val tripPreferences: TripPreferences,
        private val preferredTransferTimeRepository: PreferredTransferTimeRepository,
        private val prioritiesRepository: PrioritiesRepository
) : GetRoutingConfig {
  override suspend fun execute(): RoutingConfig {
      return RoutingConfig (
            preferredTransferTime = preferredTransferTimeRepository.getPreferredTransferTime(),
            walkingSpeed = walkingSpeedRepository.getWalkingSpeed(),
            cyclingSpeed = cyclingSpeedRepository.getCyclingSpeed(),
            shouldUseConcessionPricing = tripPreferences.isConcessionPricingPreferred,
            isOnWheelchair = tripPreferences.isWheelchairPreferred,
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