package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import com.skedgo.tripkit.ui.core.module.FragmentScope
import java.lang.UnsupportedOperationException
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class TripPreviewHeaderViewModelFactory @Inject constructor(private val tripPreviewHeaderViewModelProvider: Provider<TripPreviewHeaderViewModel>)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass == TripPreviewHeaderViewModel::class.java) {
            return tripPreviewHeaderViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}