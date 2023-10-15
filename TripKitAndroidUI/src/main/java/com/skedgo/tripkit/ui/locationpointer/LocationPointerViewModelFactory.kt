package com.skedgo.tripkit.ui.locationpointer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class LocationPointerViewModelFactory @Inject constructor(private val locationChooserViewModelProvider: Provider<LocationPointerViewModel>)
        : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == LocationPointerViewModel::class.java) {
            return locationChooserViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}