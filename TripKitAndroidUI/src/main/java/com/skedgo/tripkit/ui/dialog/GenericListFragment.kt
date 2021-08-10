package com.skedgo.tripkit.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.skedgo.tripkit.ui.databinding.DialogGenericListBinding

class GenericListFragment(private val listener: Listener) : DialogFragment(), GenericListAdapter.OnSelectListener {

    private lateinit var binding: DialogGenericListBinding
    private lateinit var selection: List<String>
    private lateinit var adapter: GenericListAdapter
    private lateinit var title: String

    private var isMultiSelect = true
    private val selectedList = ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogGenericListBinding.inflate(inflater, container, false)

        setupSelectionRecyclerView()
        setupLabel()
        setupListener()
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun setupLabel() {
        binding.txtTitle.text = title
    }

    private fun setupSelectionRecyclerView() {
        adapter = GenericListAdapter(this)
        binding.rvSelection.layoutManager = LinearLayoutManager(requireActivity())
        binding.rvSelection.adapter = adapter

        adapter.setSelectionList(selection)
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun setSelection(selection: ArrayList<String>) {
        this.selection = selection
    }

    fun toggleMultiSelect(isMultiSelect: Boolean) {
        this.isMultiSelect = isMultiSelect
    }

    private fun setupListener() {
        binding.btnClose.setOnClickListener {
            dismissAllowingStateLoss()
            listener.onClose(this)
        }

        binding.btnClose.setOnClickListener {
            listener.onAccept(selectedList, this)
        }
    }

    interface Listener {
        fun onAccept(selectedList: List<String>, fragment: GenericListFragment)
        fun onClose(fragment: GenericListFragment)
    }

    override fun onSelect(selection: String) {
        if (isMultiSelect) {
            if (selectedList.contains(selection)) {
                selectedList.remove(selection)
            } else {
                selectedList.add(selection)
            }
        } else {
            if (selectedList.isNotEmpty()) {
                selectedList.clear()
            }
            selectedList.add(selection)
        }
        adapter.notifyDataSetChanged()
    }

    override fun isSelected(selection: String): Boolean {
        return selectedList.contains(selection)
    }
}