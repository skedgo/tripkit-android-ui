package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.UnsupportedOperationException
import javax.inject.Inject
import javax.inject.Provider


class TripPreviewHeaderViewModelFactory @Inject constructor(private val timetableViewModelProvider: Provider<TripPreviewHeaderViewModel>)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass == TripPreviewHeaderViewModel::class.java) {
            return timetableViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}