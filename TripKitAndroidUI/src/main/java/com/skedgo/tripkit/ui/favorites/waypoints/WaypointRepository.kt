package com.skedgo.tripkit.ui.favorites.waypoints

import com.skedgo.network.Resource
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.utils.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst

interface WaypointRepository {

    fun insertWaypoints(tripId: String, waypoints: List<Waypoint>): Flow<Resource<Boolean>>
    fun deleteTripWaypoints(tripId: String): Flow<Resource<Boolean>>
    fun getTripWaypoints(tripId: String): Flow<List<Waypoint>>
    fun getTripGroup(waypoints: List<Waypoint>): Flow<TripGroup?>

    class WaypointRepositoryImpl(
        private val getRoutingConfig: GetRoutingConfig,
        private val getTripFromWaypoints: GetTripFromWaypoints,
        private val tripGroupRepository: TripGroupRepository,
        private val waypointsDao: WaypointsDao
    ) : WaypointRepository {

        override fun insertWaypoints(
            tripId: String,
            waypoints: List<Waypoint>
        ): Flow<Resource<Boolean>> {
            return flow {
                safeCall<Boolean> {
                    waypointsDao.deleteTripWaypoints(tripId)

                    val waypointEntities = waypoints.mapIndexed { index, waypoint ->
                        waypoint.toWaypointEntity(tripId, index)
                    }

                    waypointsDao.insertAll(waypointEntities)
                    emit(Resource.success(data = true))
                }
            }.flowOn(Dispatchers.IO)
        }

        override fun deleteTripWaypoints(
            tripId: String
        ): Flow<Resource<Boolean>> {
            return flow {
                safeCall<Boolean> {
                    waypointsDao.deleteTripWaypoints(tripId)
                    emit(Resource.success(data = true))
                }
            }.flowOn(Dispatchers.IO)
        }

        override fun getTripWaypoints(tripId: String): Flow<List<Waypoint>> {
            return flow<List<Waypoint>> {
                try {
                    val waypointsEntities =
                        waypointsDao.getAllWaypoints().filter { it.tripId == tripId }
                            .sortedBy { it.order }

                    emit(waypointsEntities.map { it.toWaypoint() })
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(emptyList())
                }
            }.flowOn(Dispatchers.IO)
        }

        override fun getTripGroup(waypoints: List<Waypoint>): Flow<TripGroup?> {
            return flow<TripGroup?> {
                try {
                    val config = getRoutingConfig.execute()
                    val waypointResponse =
                        getTripFromWaypoints.executeAsFlow(config, waypoints)
                            .firstOrNull()
                    waypointResponse?.tripGroup?.let { tripGroup ->
                        tripGroupRepository.addTripGroups(tripGroup.uuid(), listOf(tripGroup))
                            .await()
                        emit(tripGroupRepository.getTripGroup(tripGroup.uuid()).awaitFirst())
                    }
                } catch (e: Exception) {
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)
        }


    }
}