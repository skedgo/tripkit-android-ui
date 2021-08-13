package com.skedgo.tripkit.ui.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject

class GenericListViewModel @Inject constructor() : RxViewModel() {

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _selection = MutableLiveData<List<GenericListItem>>()
    val selection: LiveData<List<GenericListItem>> = _selection

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setListSelection(value: List<GenericListItem>) {
        _selection.value = value
    }

}