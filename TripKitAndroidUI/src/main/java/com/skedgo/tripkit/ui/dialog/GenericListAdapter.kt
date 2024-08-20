package com.skedgo.tripkit.ui.dialog

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.ViewGenericListItemBinding
import com.skedgo.tripkit.ui.utils.AutoUpdatableAdapter
import javax.inject.Inject
import kotlin.properties.Delegates

class GenericListAdapter @Inject constructor() :
    RecyclerView.Adapter<GenericListAdapter.Holder>(),
    AutoUpdatableAdapter {

    internal var collection: List<GenericListItem> by Delegates.observable(emptyList()) { prop, old, new ->
        autoNotify(old, new) { o, n -> o.label == n.label }
    }

    internal var isSingleSelection: Boolean = false
    internal var isViewOnlyMode: Boolean = false
    internal var clickListener: (GenericListItem) -> Unit = { _ -> }
    internal var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder.from(
            parent,
            R.layout.view_generic_list_item
        )

    override fun getItemCount() = collection.size

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.apply {
            val listItem = collection[position]
            item = listItem
            viewMode = isViewOnlyMode
            executePendingBindings()
            if (!isViewOnlyMode) {
                holder.itemView.setOnClickListener {
                    if (isSingleSelection) {
                        updateSelected(listItem)
                    } else {
                        listItem.selected = !listItem.selected
                        notifyItemChanged(position)
                    }
                    callback?.onSelect(position)
                    clickListener.invoke(listItem)
                }
            }
            clParent.accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo
                ) {

                    super.onInitializeAccessibilityNodeInfo(host, info)

                    if (!isViewOnlyMode) {

                        val description =
                            if (collection[position].selected) "unselect" else "select"

                        val customClick = AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfoCompat.ACTION_CLICK, description
                        )

                        info.addAction(customClick)
                    }
                }

                override fun sendAccessibilityEvent(host: View, eventType: Int) {

                    val currentContentDescription = genericListItemTvLabel.text

                    host?.contentDescription = "$currentContentDescription ${
                        if (collection[position].selected) "\n Selected" else ""
                    }"

                    super.sendAccessibilityEvent(host, eventType)
                }
            }
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

    class Holder(val binding: ViewGenericListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup, layout: Int): Holder {
                val inflater = LayoutInflater.from(parent.context)
                val binding =
                    DataBindingUtil.inflate<ViewGenericListItemBinding>(
                        inflater, layout,
                        parent, false
                    )
                return Holder(binding)
            }
        }
    }

    interface Callback {
        fun onSelect(position: Int)
    }
}
