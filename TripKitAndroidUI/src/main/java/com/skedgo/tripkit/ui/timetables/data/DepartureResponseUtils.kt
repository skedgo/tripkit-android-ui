package com.skedgo.tripkit.ui.timetables.data

import com.skedgo.tripkit.common.StyleManager
import com.skedgo.tripkit.ui.model.DeparturesResponse

fun DeparturesResponse.postProcess(embarkationStops: List<String>,
                                   disembarkationStops: List<String>?): DeparturesResponse =
    this.apply {
      // Must proceed this so that services can have theirs proper stop code
      processEmbarkationStopList()

      if (!disembarkationStops.isNullOrEmpty() &&
          embarkationStops.isNotEmpty() &&
          !serviceList.isNullOrEmpty()) {
        // We've just fetched A2B timetable.
        // So, we need to update service's pairIdentifier so that
        // next time, we can query services for a given pair of A2B.
        val startStopCode = embarkationStops[0]
        val endStopCode = disembarkationStops[0]
        for (service in serviceList!!) {
          service.pairIdentifier = String.format(
              StyleManager.FORMAT_PAIR_IDENTIFIER,
              startStopCode,
              endStopCode
          )

        }
      } // Else, we've fetched timetable for a stop

      // Save original service time (for real time services)
      if (!serviceList.isNullOrEmpty()) {
        for (service in serviceList!!) {
          service.serviceTime = service.startTimeInSecs
        }
      }
    }
