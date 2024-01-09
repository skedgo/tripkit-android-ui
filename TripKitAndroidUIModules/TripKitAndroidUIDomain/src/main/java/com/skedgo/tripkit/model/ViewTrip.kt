package com.skedgo.tripkit.model

import android.os.Parcelable
import com.skedgo.tripkit.common.model.Query
import kotlinx.parcelize.Parcelize

@Parcelize
data class ViewTrip (
      val query: Query,
      val tripGroupUUID: String,
      val displayTripID: Long,
      val sortOrder: Int) : Parcelable {
  fun tripGroupUUID(): String = tripGroupUUID
  fun query(): Query = query
  fun shouldCheckForOutdated(): Boolean = true
  fun sortOrder(): Int = sortOrder
  }
