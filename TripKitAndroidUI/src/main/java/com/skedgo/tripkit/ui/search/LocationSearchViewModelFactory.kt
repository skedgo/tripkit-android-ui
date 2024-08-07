package com.skedgo.tripkit.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class LocationSearchViewModelFactory @Inject constructor(private val locationSearchViewModelProvider: Provider<LocationSearchViewModel>) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == LocationSearchViewModel::class.java) {
            return locationSearchViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}