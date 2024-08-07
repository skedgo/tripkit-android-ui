package com.skedgo.tripkit.ui.generic.action_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemActionBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class ActionListAdapter @Inject constructor() :
    RecyclerView.Adapter<ActionListAdapter.Holder>(),
    AutoUpdatableAdapter {

    var collection: List<Action> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.label == n.label }
    }

    var clickListener: (Action) -> Unit = { _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder.from(
            parent,
            R.layout.item_action
        )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            item = collection[position]
            executePendingBindings()
            holder.itemView.setOnClickListener {
                clickListener.invoke(collection[position])
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemActionBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ItemActionBinding>(
                        inflater, layout,
                        parent, false
                    )
                return Holder(binding)
            }
        }
    }
}
