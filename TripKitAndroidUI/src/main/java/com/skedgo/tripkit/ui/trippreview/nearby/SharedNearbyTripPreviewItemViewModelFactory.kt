package com.skedgo.tripkit.ui.trippreview.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import java.lang.UnsupportedOperationException
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class SharedNearbyTripPreviewItemViewModelFactory @Inject constructor(private val sharedNearbyTripPreviewItemViewModel: Provider<SharedNearbyTripPreviewItemViewModel>)
    : ViewModelProvider.Factory  {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass == SharedNearbyTripPreviewItemViewModel::class.java) {
            return sharedNearbyTripPreviewItemViewModel.get() as T
        }

        throw UnsupportedOperationException()
    }

}
