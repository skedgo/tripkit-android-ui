package com.skedgo.tripkit.ui.trippreview.drt

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ItemDrtTicketBinding
import com.skedgo.tripkit.ui.utils.isTalkBackOn
import com.skedgo.tripkit.ui.utils.talkBackSpeak
import com.skedgo.tripkit.ui.utils.updateClickActionAccessibilityLabel
import me.tatarka.bindingcollectionadapter2.BindingRecyclerViewAdapter

/**
 * Custom adapter to access the viewBinding from the adapter of [DrtTicketViewModel]
 * that's using me.tatarka.bindingcollectionadapter2
 */
class DrtTicketCustomRecyclerViewAdapter<T> : BindingRecyclerViewAdapter<T>() {

    lateinit var context: Context

    override fun onCreateBinding(
        inflater: LayoutInflater, @LayoutRes layoutId: Int,
        viewGroup: ViewGroup
    ): ViewDataBinding {
        return super.onCreateBinding(inflater, layoutId, viewGroup)
    }

    override fun onBindBinding(
        binding: ViewDataBinding,
        variableId: Int, @LayoutRes layoutRes: Int,
        position: Int,
        item: T
    ) {
        super.onBindBinding(binding, variableId, layoutRes, position, item)

        context = binding.root.context

        if (item is DrtTicketViewModel) {
            with(binding as ItemDrtTicketBinding) {
                // this will change accessibility read out from "Increase, Button, Double Tap to Activate"
                // to "Increase, Button, Double Tap to increase"
                itemDrtIbIncrement.updateClickActionAccessibilityLabel(context.getString(R.string.talkback_increment))
                itemDrtIbIncrement.setOnClickListener {
                    item.onIncrementValue()
                    readOutTicketCount(item.name.value.orEmpty(), (item.value.value ?: 0).toInt())
                }
                // this will change accessibility read out from "Decrease, Button, Double Tap to Activate"
                // to "Decrease, Button, Double Tap to decrease
                itemDrtIbDecrement.updateClickActionAccessibilityLabel(context.getString(R.string.talkback_decrement))
                itemDrtIbDecrement.setOnClickListener {
                    item.onDecrementValue()
                    readOutTicketCount(item.name.value.orEmpty(), (item.value.value ?: 0).toInt())
                }
                itemDrtSelectLayout.setOnClickListener {
                    item.onSelect()
                    readOutTicketCount(item.name.value.orEmpty(), (item.value.value ?: 0).toInt())
                }
            }
        }
    }

    private fun readOutTicketCount(name: String, count: Int) {
        if (context.isTalkBackOn()) {
            val description = "$count $name "
            context.talkBackSpeak(
                context.resources.getQuantityString(
                    R.plurals.riders,
                    count,
                    description
                )
            )
        }
    }
}