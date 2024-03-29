package com.skedgo.tripkit.ui.tripresults

import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList

object GroupDiffCallback : DiffObservableList.Callback<TripResultViewModel> {
  override fun areItemsTheSame(lhs: TripResultViewModel?, rhs: TripResultViewModel?): Boolean
      = lhs!! == rhs!!

  override fun areContentsTheSame(lhs: TripResultViewModel?, rhs: TripResultViewModel?): Boolean
          = lhs!! == rhs!!
}
