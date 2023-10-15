package com.skedgo.tripkit.ui.tripresult

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.routing.RoadTag
import com.skedgo.tripkit.routing.getRoadTagLabel
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemFakeGraphBinding
import com.skedgo.tripkit.ui.databinding.ItemRoadTagLabelBinding
import com.skedgo.tripkit.ui.databinding.ViewGenericListItemBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class RoadTagChartAdapter @Inject constructor() :
    RecyclerView.Adapter<RoadTagChartAdapter.Holder>(),
    AutoUpdatableAdapter {

    internal var collection: List<RoadTagChart> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.items == n.items }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder.from(
            parent,
            R.layout.item_fake_graph
        )

    override fun getItemCount() = collection.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            val roadTagChart = collection[position]
            item = roadTagChart

            RoadTagChartItemAdapter().let { adapter ->
                rvRoadTags.adapter = adapter
                adapter.collection = roadTagChart.items
            }

            executePendingBindings()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }

    class Holder(val binding: ItemFakeGraphBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ItemFakeGraphBinding>(
                        inflater, layout,
                        parent, false
                    )
                return Holder(binding)
            }
        }
    }
}
