package com.skedgo.tripkit.ui.routing.settings

import com.skedgo.tripkit.TripPreferences
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import com.skedgo.tripkit.ui.utils.TransportModeSharedPreference
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration

/**
 * Regression coverage for the "walking trip re-opens as a wheelchair trip" bug.
 *
 * [RoutingConfig.isOnWheelchair] ends up as the `config.wheelchair` flag of the `waypoint.json`
 * re-plan that runs when a service is picked from the timetable card. It must agree with the A2B
 * routing request, which derives the flag from the wheelchair *transport mode* selection
 * (`TripResultListViewModel.load()`), and must ignore the unrelated "wheelchair information"
 * option ([TripPreferences.isWheelchairPreferred]).
 */
class GetRoutingConfigImplTest {

    private val walkingSpeedRepository: WalkingSpeedRepository = mockk()
    private val cyclingSpeedRepository: CyclingSpeedRepository = mockk()
    private val rollingSpeedRepository: RollingSpeedRepository = mockk()
    private val unitsRepository: UnitsRepository = mockk()
    private val tripPreferences: TripPreferences = mockk()
    private val preferredTransferTimeRepository: PreferredTransferTimeRepository = mockk()
    private val prioritiesRepository: PrioritiesRepository = mockk()
    private val transportModeSharedPreference: TransportModeSharedPreference = mockk()

    private lateinit var getRoutingConfig: GetRoutingConfigImpl

    @Before
    fun setUp() {
        every { walkingSpeedRepository.getWalkingSpeed() } returns WalkingSpeed.Medium
        every { cyclingSpeedRepository.getCyclingSpeed() } returns CyclingSpeed.Medium
        every { rollingSpeedRepository.getRollingSpeed() } returns RollingSpeed.Medium
        every { unitsRepository.getUnit() } returns "auto"
        every { tripPreferences.isConcessionPricingPreferred() } returns false
        coEvery { preferredTransferTimeRepository.getPreferredTransferTime(any()) } returns
            Duration.ofMinutes(3)
        coEvery { prioritiesRepository.getBudgetPriority() } returns Priority.Budget()
        coEvery { prioritiesRepository.getEnvironmentPriority() } returns Priority.Environment()
        coEvery { prioritiesRepository.getTimePriority() } returns Priority.Time()
        coEvery { prioritiesRepository.getConveniencePriority() } returns Priority.Convenience()
        coEvery { prioritiesRepository.getExercisePriority() } returns Priority.Exercise()

        getRoutingConfig = GetRoutingConfigImpl(
            walkingSpeedRepository,
            cyclingSpeedRepository,
            rollingSpeedRepository,
            unitsRepository,
            tripPreferences,
            preferredTransferTimeRepository,
            prioritiesRepository,
            transportModeSharedPreference
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `walking selected keeps the config off wheelchair even when wheelchair information is on`() =
        runTest {
            every { transportModeSharedPreference.isWheelchairModeSelected() } returns false
            every { tripPreferences.isWheelchairPreferred() } returns true

            assertThat(getRoutingConfig.execute().isOnWheelchair).isFalse()
        }

    @Test
    fun `walking selected keeps the config off wheelchair`() = runTest {
        every { transportModeSharedPreference.isWheelchairModeSelected() } returns false
        every { tripPreferences.isWheelchairPreferred() } returns false

        assertThat(getRoutingConfig.execute().isOnWheelchair).isFalse()
    }

    @Test
    fun `wheelchair mode selected puts the config on wheelchair`() = runTest {
        every { transportModeSharedPreference.isWheelchairModeSelected() } returns true
        every { tripPreferences.isWheelchairPreferred() } returns false

        assertThat(getRoutingConfig.execute().isOnWheelchair).isTrue()
    }
}
