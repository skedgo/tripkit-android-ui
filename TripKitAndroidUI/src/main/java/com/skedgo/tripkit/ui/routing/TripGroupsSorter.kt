package com.skedgo.tripkit.ui.routing

import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.core.modeprefs.TransportModePreference
import com.skedgo.tripkit.ui.tripresults.SortOrders
import io.reactivex.Observable
import com.skedgo.tripkit.routing.*
import java.util.*
import java.util.Collections.sort
import javax.inject.Inject

open class TripGroupsSorter @Inject internal constructor() {
  open fun sort(sortOrder: Int, groups: List<TripGroup>?, willArriveBy: Boolean) {
    if (groups != null) {
      when (sortOrder) {
        SortOrders.SORT_ORDER_PRICE -> sort(groups, TripGroupComparators.createPriceComparatorChain(willArriveBy))
        SortOrders.SORT_ORDER_DURATION -> sort(groups, TripGroupComparators.createDurationComparatorChain(willArriveBy))
        SortOrders.SORT_ORDER_PREFERRED -> sort(groups, TripGroupComparators.createPreferredComparatorChain(willArriveBy))
        else -> sort(
            groups,
            if (willArriveBy)
              TripGroupComparators.DEPARTURE_COMPARATOR_CHAIN
            else
              TripGroupComparators.ARRIVAL_COMPARATOR_CHAIN
        )
      }
    }
  }

  open fun updateTripGroupVisibilities(
      tripGroups: MutableList<TripGroup>,
      modePreferences: List<TransportModePreference>
  ) {
    if (modePreferences.isEmpty() || tripGroups.isEmpty()) {
      return
    }

    for (tripGroup in tripGroups) {
      tripGroup.visibility = GroupVisibility.FULL
    }

    val excludedModeIds = getExcludedModeIdsExceptWalking(modePreferences)
    removeGroupsWhichContainExcludedModes(tripGroups, excludedModeIds)
    removeWalkingOnlyGroups(tripGroups, modePreferences)

    val minimizedModeIds = getMinimizedModeIds(modePreferences)
    val minimizedModeIdsWithoutWalking = getMinimizedModeIdsWithoutWalking(minimizedModeIds)
    for (group in tripGroups) {
      val displayTrip = group.displayTrip!!

      group.visibility = GroupVisibility.FULL

      // Check special case for walking only.
      // Only filter out invisible walking when the whole trip is walking.
      val isWalkOnly = displayTrip.hasWalkOnly()
      if (isWalkOnly) {
        for (modeId in minimizedModeIds) {
          if (TransportMode.ID_WALK == modeId) {
            group.visibility = GroupVisibility.COMPACT
            break
          }
        }
      } else {
        val aDisabledModeStillExists = checkViolationInvisible(
            displayTrip.getModeIds(),
            minimizedModeIdsWithoutWalking
        )
        if (aDisabledModeStillExists) {
          group.visibility = GroupVisibility.COMPACT
        }
      }
    }

    // Select only the best COMPACT route group for each mode group.
    val goneGroups = LinkedList<TripGroup>()
    Observable.fromIterable(tripGroups)
        .filter { it.visibility == GroupVisibility.COMPACT }
        .groupBy {
          // Given two mode lists: [A, B, A] and [B, A, B, B]
          // We'll group them as a common group with key [A, B].
          HashSet(it.displayTrip!!.getModeIds())
        }
        .subscribe { observable ->
          // The first one is the best.
          observable.skip(1).toList().subscribe {
            worseGroups ->
            goneGroups.addAll(worseGroups)
          }
        }
    tripGroups.removeAll(goneGroups)
  }

  fun checkViolationInvisible(
      modeIds: List<String>,
      removalModeIdSet: HashSet<String>
  ): Boolean = modeIds.any { removalModeIdSet.contains(it) }

  fun getMinimizedModeIdsWithoutWalking(
      minimizedModeIds: List<String>
  ): HashSet<String> = minimizedModeIds
      .filter { !TransportMode.ID_WALK.equals(it, ignoreCase = true) }
      .toHashSet()

  internal fun removeWalkingOnlyGroups(
      tripGroups: MutableList<TripGroup>,
      modePreferences: List<TransportModePreference>
  ) {
    for (transportModePreference in modePreferences) {
      if (TransportMode.ID_WALK == transportModePreference.modeId && !transportModePreference.isIncluded) {
        // Walking mode has been excluded.
        val size = tripGroups.size
        for (i in size - 1 downTo 0) {
          val displayTrip = tripGroups[i].displayTrip!!
          if (displayTrip.hasWalkOnly()) {
            tripGroups.removeAt(i)
          }
        }
        break
      }
    }
  }

  internal fun getMinimizedModeIds(
      modePreferences: List<TransportModePreference>
  ): List<String> = modePreferences
      .filter { it.isIncluded && it.isMinimized }
      .map { it.modeId }

  internal fun removeGroupsWhichContainExcludedModes(
      tripGroups: MutableList<TripGroup>,
      excludedModeIds: List<String>
  ) {
    val size = tripGroups.size
    for (i in size - 1 downTo 0) {
      val tripGroup = tripGroups[i]
      if (tripGroup.containsAnyMode(excludedModeIds)) {
        tripGroups.remove(tripGroup)
      }
    }
  }

  internal fun getExcludedModeIdsExceptWalking(
      modePreferences: List<TransportModePreference>
  ): List<String> = modePreferences
      .filter { !it.isIncluded && TransportMode.ID_WALK != it.modeId }
      .map { it.modeId }
}
