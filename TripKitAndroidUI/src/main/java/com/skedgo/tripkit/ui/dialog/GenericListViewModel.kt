package com.skedgo.tripkit.ui.dialog

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
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