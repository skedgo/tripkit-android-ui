package com.skedgo.tripkit.ui.realtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.core.module.ActivityScope
import javax.inject.Inject
import javax.inject.Provider

@ActivityScope
class RealTimeViewModelFactory @Inject constructor(
    private val realTimeChoreographerVMProvider: Provider<RealTimeChoreographerViewModel>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == RealTimeChoreographerViewModel::class.java) {
            return realTimeChoreographerVMProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}