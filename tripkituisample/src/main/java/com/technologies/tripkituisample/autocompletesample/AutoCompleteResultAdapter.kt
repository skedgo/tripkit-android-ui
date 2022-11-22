package com.technologies.tripkituisample.autocompletesample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ItemAutoCompleteResultBinding
import javax.inject.Inject
import kotlin.properties.Delegates

class AutoCompleteResultAdapter @Inject constructor() :
        RecyclerView.Adapter<AutoCompleteResultAdapter.Holder>(),
        AutoUpdatableAdapter {

    var collection: List<AutoCompleteResultItem> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.id == n.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder.from(
                    parent,
                    R.layout.item_auto_complete_result
            )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            item = collection[position]
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemAutoCompleteResultBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                        DataBindingUtil.inflate<ItemAutoCompleteResultBinding>(
                                inflater, layout,
                                parent, false
                        )
                return Holder(binding)
            }
        }
    }
}
