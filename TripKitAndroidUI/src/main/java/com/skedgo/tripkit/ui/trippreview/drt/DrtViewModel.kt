package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import javax.inject.Inject

class DrtViewModel @Inject constructor() : RxViewModel() {

    val onItemChangeStream = MutableSharedFlow<DrtItemViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemsDiffCallBack)
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    object DrtItemsDiffCallBack : DiffUtil.ItemCallback<DrtItemViewModel>() {
        override fun areItemsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label == newItem.label

        override fun areContentsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label.value == newItem.label.value
                        && oldItem.value.value == newItem.value.value
    }

    init {
        onItemChangeStream.onEach {
            when(it.label.value){
                "Mobility Options" -> {}
                "Purpose" -> {}
                "Add note" -> {}
            }
        }.launchIn(viewModelScope)
    }


    fun generateDrtItems(){
        val result = mutableListOf<DrtItemViewModel>()

        val mobilityOptions = DrtItemViewModel()
        mobilityOptions.setIcon(R.drawable.ic_person)
        mobilityOptions.setLabel("Mobility Options")
        mobilityOptions.setValue("Tap Change to make selections")
        mobilityOptions.setRequired(true)
        mobilityOptions.onChangeStream = onItemChangeStream

        val purpose = DrtItemViewModel()
        purpose.setIcon(R.drawable.ic_flag)
        purpose.setLabel("Purpose")
        purpose.setValue("Tap Change to make selections")
        purpose.setRequired(true)
        purpose.onChangeStream = onItemChangeStream

        val addNote = DrtItemViewModel()
        addNote.setIcon(R.drawable.ic_edit)
        addNote.setLabel("Add note")
        addNote.setValue("Tap Change to add notes")
        addNote.setRequired(true)
        addNote.onChangeStream = onItemChangeStream

        items.add(mobilityOptions)
        items.add(purpose)
        items.add(addNote)

    }

}