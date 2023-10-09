package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.GridLayoutManager
import com.skedgo.tripkit.ui.databinding.TripPreviewPagerDirectionsItemBinding
import com.skedgo.tripkit.ui.databinding.TripPreviewStepBinding
import com.skedgo.tripkit.ui.trippreview.directions.DirectionsTripPreviewItemStepViewModel
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter

private const val TAG = "TripPreviewSCRVAdapter"

class TripPreviewStepCustomRecyclerViewAdapter<T> : BindingRecyclerViewAdapter<T>() {

    lateinit var context: Context

    override fun onCreateBinding(
        inflater: LayoutInflater, @LayoutRes layoutId: Int,
        viewGroup: ViewGroup
    ): ViewDataBinding {
        return super.onCreateBinding(inflater, layoutId, viewGroup).apply {
            Log.e(TAG, "created binding: $this")
        }
    }

    override fun onBindBinding(
        binding: ViewDataBinding,
        variableId: Int, @LayoutRes layoutRes: Int,
        position: Int,
        item: T
    ) {
        super.onBindBinding(binding, variableId, layoutRes, position, item)

        context = binding.root.context

        if (item is DirectionsTripPreviewItemStepViewModel) {
            with(binding as TripPreviewStepBinding) {
                val adapter = RoadTagLabelListAdapter()
                binding.rvTags.adapter = adapter
                adapter.collection = item.generateRoadTagItems()
            }
        }
    }
}