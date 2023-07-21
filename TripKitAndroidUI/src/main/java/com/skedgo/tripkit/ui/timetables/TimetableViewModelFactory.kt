package com.skedgo.tripkit.ui.timetables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.UnsupportedOperationException
import javax.inject.Inject
import javax.inject.Provider


class TimetableViewModelFactory @Inject constructor(private val timetableViewModelProvider: Provider<TimetableViewModel>)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == TimetableViewModel::class.java) {
            return timetableViewModelProvider.get() as T
        }

        throw UnsupportedOperationException()
    }
}