package com.skedgo.tripkit.ui.poidetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.UnsupportedOperationException
import javax.inject.Inject
import javax.inject.Provider


class PoiDetailsViewModelFactory @Inject constructor(private val poiDetailsViewModelProvider: Provider<PoiDetailsViewModel>)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == PoiDetailsViewModel::class.java) {
            return poiDetailsViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}