package skedgo.tripgo.agenda.legacy

import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import com.skedgo.tripkit.ui.routing.RoutingConfig
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.PrioritiesRepository
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.WeightingProfile
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import javax.inject.Inject

/** TODO: Refactor to move this class to TripGoData. */
internal class GetRoutingConfigImpl @Inject constructor(
        private val walkingSpeedRepository: WalkingSpeedRepository,
        private val cyclingSpeedRepository: CyclingSpeedRepository,
        private val tripPreferences: TripPreferences,
        private val preferredTransferTimeRepository: PreferredTransferTimeRepository,
        private val prioritiesRepository: PrioritiesRepository
) : GetRoutingConfig {
  override fun execute(): Observable<RoutingConfig>
      = walkingSpeedRepository.getWalkingSpeed()
      .withLatestFrom(
          cyclingSpeedRepository.getCyclingSpeed(),
          preferredTransferTimeRepository.getPreferredTransferTime()
      ) {
        walkingSpeed, cyclingSpeed, preferredTransferTime ->
        RoutingConfig(
            preferredTransferTime = preferredTransferTime,
            walkingSpeed = walkingSpeed,
            cyclingSpeed = cyclingSpeed,
            shouldUseConcessionPricing = tripPreferences.isConcessionPricingPreferred,
                weightingProfile = WeightingProfile(
                        budgetPriority = prioritiesRepository.getBudgetPriority().blockingFirst(),
                        environmentPriority = prioritiesRepository.getEnvironmentPriority().blockingFirst(),
                        timePriority = prioritiesRepository.getTimePriority().blockingFirst(),
                        conveniencePriority = prioritiesRepository.getConveniencePriority().blockingFirst()
                ),
                isOnWheelchair = tripPreferences.isWheelchairPreferred
        )
      }
}