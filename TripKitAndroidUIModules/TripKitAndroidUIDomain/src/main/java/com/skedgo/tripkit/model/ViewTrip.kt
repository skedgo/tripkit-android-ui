package com.skedgo.tripkit.model

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.skedgo.tripkit.common.model.Query
import kotlinx.android.parcel.Parcelize

sealed class ViewTrip : Parcelable {
    data class FromExternal(val tripGroupUUID: String)

    @Parcelize
    data class FromRoutesScreen(
        val query: Query,
        val tripGroupUUID: String,
        val displayTripID: Long,
        val sortOrder: Int
    ) : ViewTrip(), Parcelable {
        fun tripGroupUUID(): String = tripGroupUUID
        fun query(): Query = query
        fun shouldCheckForOutdated(): Boolean = true
        fun sortOrder(): Int = sortOrder
    }
}