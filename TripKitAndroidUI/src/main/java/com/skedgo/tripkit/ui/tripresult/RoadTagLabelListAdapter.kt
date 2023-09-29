package com.skedgo.tripkit.ui.tripresult

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemRoadTagLabelBinding
import com.skedgo.tripkit.ui.databinding.ViewGenericListItemBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class RoadTagLabelListAdapter @Inject constructor() :
    RecyclerView.Adapter<RoadTagLabelListAdapter.Holder>(),
    AutoUpdatableAdapter {

    internal var collection: List<RoadTagChartItem> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.label == n.label }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder.from(
            parent,
            R.layout.item_road_tag_label
        )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            val roadTagChartItem = collection[position]
            item = roadTagChartItem
            executePendingBindings()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemRoadTagLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ItemRoadTagLabelBinding>(
                        inflater, layout,
                        parent, false
                    )
                return Holder(binding)
            }
        }
    }
}
