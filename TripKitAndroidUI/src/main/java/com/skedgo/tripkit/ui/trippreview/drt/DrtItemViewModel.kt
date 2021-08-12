package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class DrtItemViewModel : ViewModel() {

    var onChangeStream: MutableSharedFlow<DrtItemViewModel>? = null

    private val _icon = MutableLiveData<Int>()
    val icon: LiveData<Int> = _icon

    private val _label = MutableLiveData<String>()
    val label: LiveData<String> = _label

    private val _values = MutableLiveData<List<String>>()
    val values: LiveData<List<String>> = _values

    private val _required = MutableLiveData<Boolean>()
    val required: LiveData<Boolean> = _required

    private val _viewMode = MutableLiveData<Boolean>()
    val viewMode: LiveData<Boolean> = _viewMode

    private val _itemId = MutableLiveData<String>()
    val itemId: LiveData<String> = _itemId

    fun setIcon(value: Int) {
        _icon.value = value
    }

    fun setLabel(value: String) {
        _label.value = value
    }

    fun setValue(values: List<String>) {
        _values.value = values
    }

    fun setRequired(value: Boolean) {
        _required.value = value
    }

    fun onChange() {
        viewModelScope.launch {
            onChangeStream?.emit(this@DrtItemViewModel)
        }
    }

    fun setViewMode(value: Boolean) {
        _viewMode.value = value
    }

    fun setItemId(value: String) {
        _itemId.value = value
    }
}