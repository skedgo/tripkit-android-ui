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

    private val _viewModeOnly = MutableLiveData<Boolean>()
    val viewModeOnly: LiveData<Boolean> = _viewModeOnly

    fun setViewModeOnly(value: Boolean) {
        _viewModeOnly.value = value
    }

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setListSelection(value: List<GenericListItem>) {
        _selection.value = value
    }

    fun setSelectedItems(selectedValues: List<String>) {
        _selection.value?.let {
            val result = it.toMutableList()
            if (_viewModeOnly.value != true) {
                result.filter { item ->
                    selectedValues.any { selectedValue -> item.label == selectedValue }
                }.map {
                    it.selected = true
                }
            } else {
                result.clear()
                result.addAll(
                        it.filter { item ->
                            selectedValues.any { selectedValue -> item.label == selectedValue }
                        }
                )
            }

            _selection.value = result
        }
    }
}