package com.skedgo.tripkit.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.ui.databinding.ViewholderGenericListBinding
import kotlin.collections.ArrayList

@Deprecated("Use GenericListAdapter")
class GenericListAdapterOld(private val onClickListener: OnSelectListener) :
        RecyclerView.Adapter<GenericListViewHolder>() {
    interface OnSelectListener {
        fun onSelect(selection: String)
        fun isSelected(selection: String): Boolean
    }

    private val selectionList = ArrayList<String>()

    fun setSelectionList(selectionList: List<String>) {
        this.selectionList.clear()
        this.selectionList.addAll(selectionList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericListViewHolder {
        val binding: ViewholderGenericListBinding =
                ViewholderGenericListBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        return GenericListViewHolder(binding, onClickListener)
    }

    override fun getItemCount(): Int = selectionList.size

    override fun onBindViewHolder(holder: GenericListViewHolder, position: Int) =
            holder.bind(selectionList[position])
}

class GenericListViewHolder(
        private val binding: ViewholderGenericListBinding,
        private val onSelectListener: GenericListAdapterOld.OnSelectListener
) :
        RecyclerView.ViewHolder(binding.root) {

    private lateinit var selection: String

    fun bind(selection: String) {
        binding.txtLabel.text = selection

        binding.isChecked = onSelectListener.isSelected(selection)

        binding.root.setOnClickListener {
            onSelectListener.onSelect(selection)
            binding.isChecked = onSelectListener.isSelected(selection)
        }
    }
}