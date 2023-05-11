package com.technologies.tripkituisample.autocompleter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.regionrouting.data.RegionRoute
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ItemRegionRouteBinding

class AutocompleterAdapter(
    val items: List<RegionRoute>
): RecyclerView.Adapter<AutocompleterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AutocompleterAdapter.ViewHolder.from(
            parent,
            R.layout.item_region_route
        )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            item = items[position]
            executePendingBindings()
        }
    }

    class ViewHolder(val binding: ItemRegionRouteBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): AutocompleterAdapter.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ItemRegionRouteBinding>(
                        inflater, layout,
                        parent, false
                    )
                return ViewHolder(binding)
            }
        }
    }

}