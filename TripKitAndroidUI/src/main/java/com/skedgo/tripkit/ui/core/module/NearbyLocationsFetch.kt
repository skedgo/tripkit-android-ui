package com.skedgo.tripkit.ui.core.module

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Distinguishes the process-wide [com.skedgo.tripkit.data.locations.LocationsFetchCoordinator]
 * used by the Nearby trip-preview radius query from the `@ActivityScope` one that
 * [ScheduledStopServiceModule] provides for the home-map cell pipeline.
 *
 * The two share the coordinator implementation but never share keys: the home map issues
 * `POST locations.json` for grid cell ids, while Nearby issues `GET locations.json` for a
 * lat/lng/radius around a single trip segment. Keeping them as separate instances means the
 * home map's activity-scoped TTL semantics established by #25753 are untouched.
 */
@Qualifier
@Retention(RUNTIME)
annotation class NearbyLocationsFetch
