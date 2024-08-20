package com.skedgo.tripkit.ui.tripresults

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class TripResultListViewModelFactory @Inject constructor(private val tripResultListViewModelProvider: Provider<TripResultListViewModel>) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TripResultListViewModel::class.java) {
            return tripResultListViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}