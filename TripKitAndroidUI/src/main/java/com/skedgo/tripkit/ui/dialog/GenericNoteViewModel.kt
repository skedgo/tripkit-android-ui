package com.skedgo.tripkit.ui.dialog

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class GenericNoteViewModel : ViewModel() {

    val fragmentTitle = ObservableField<String>()
    val inputValue = ObservableField<String>()
    val viewOnly = ObservableField<Boolean>()

}