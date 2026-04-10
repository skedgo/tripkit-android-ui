package com.skedgo.tripkit.ui.tripresults

import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList

object GroupDiffCallback : DiffObservableList.Callback<TripResultViewModel> {
    override fun areItemsTheSame(lhs: TripResultViewModel?, rhs: TripResultViewModel?): Boolean {
        if (lhs === rhs) return true
        if (lhs == null || rhs == null) return false
        return lhs.group.uuid() == rhs.group.uuid()
    }

    override fun areContentsTheSame(lhs: TripResultViewModel?, rhs: TripResultViewModel?): Boolean {
        if (lhs === rhs) return true
        if (lhs == null || rhs == null) return false
        return lhs == rhs
    }
}
