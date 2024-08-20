package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class TripPreviewPagerFragmentViewModelFactory @Inject constructor(private val tripPreviewPagerFragmentViewModelProvider: Provider<TripPreviewPagerFragmentViewModel>) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TripPreviewPagerFragmentViewModel::class.java) {
            return tripPreviewPagerFragmentViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}

