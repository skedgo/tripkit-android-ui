package com.skedgo.tripkit.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ViewGenericListDisplayItemBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class GenericListDisplayAdapter @Inject constructor() :
        RecyclerView.Adapter<GenericListDisplayAdapter.Holder>(),
        AutoUpdatableAdapter {

    internal var collection: List<GenericListItem> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.label == n.label }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder.from(
                    parent,
                    R.layout.view_generic_list_display_item
            )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            val listItem = collection[position]
            item = listItem
            executePendingBindings()
        }
    }

    private fun updateSelected(selectedItem: GenericListItem) {
        collection.map {
            it.selected = it.label == selectedItem.label
        }
        notifyDataSetChanged()
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ViewGenericListDisplayItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                        DataBindingUtil.inflate<ViewGenericListDisplayItemBinding>(
                                inflater, layout,
                                parent, false
                        )
                return Holder(binding)
            }
        }
    }
}
