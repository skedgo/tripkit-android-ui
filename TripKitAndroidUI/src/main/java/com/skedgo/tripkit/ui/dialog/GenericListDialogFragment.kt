package com.skedgo.tripkit.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.Gson
import com.skedgo.tripkit.ui.databinding.DialogGenericListBinding
import com.skedgo.tripkit.ui.dialog.GenericListDialogFragment.*
import com.skedgo.tripkit.ui.utils.fromJson
import com.skedgo.tripkit.ui.utils.observe

class GenericListDialogFragment : DialogFragment(), GenericListDialogFragmentHandler {

    private val viewModel: GenericListViewModel by viewModels()

    private lateinit var binding: DialogGenericListBinding
    private lateinit var adapter: GenericListAdapter

    private var onConfirm: ((List<GenericListItem>) -> Unit)? = null
    private var onClose: (() -> Unit)? = null


    override fun onConfirm() {
        onConfirm?.invoke(adapter.collection.filter { it.selected })
        dialog?.dismiss()
    }

    override fun onClose() {
        onClose?.invoke()
        dialog?.dismiss()
    }

    override fun onStart() {
        super.onStart()

        dialog?.apply {
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DialogGenericListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBinding()
        initViews()
        checkArgs()
        initObserver()
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
    }

    private fun initViews() {
        adapter = GenericListAdapter()
        binding.genericListRvSelection.adapter = adapter
        binding.genericListRvSelection.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }


    private fun checkArgs() {
        arguments?.let {
            if (it.containsKey(ARGS_TITLE)) {
                viewModel.setTitle(it.getString(ARGS_TITLE, ""))
            }

            if (it.containsKey(ARGS_LIST_SELECTION)) {
                viewModel.setListSelection(Gson().fromJson(it.getString(ARGS_LIST_SELECTION, "")))
            }

            if (it.containsKey(ARGS_IS_SINGLE_SELECTION)) {
                if (::adapter.isInitialized) {
                    adapter.isSingleSelection = it.getBoolean(ARGS_IS_SINGLE_SELECTION)
                }
            }
        }
    }

    private fun initObserver() {
        viewModel.apply {
            observe(selection) {
                it?.let { adapter.collection = it }
            }
        }
    }

    companion object {

        private const val ARGS_TITLE = "_title"
        private const val ARGS_LIST_SELECTION = "_list_selection"
        private const val ARGS_IS_SINGLE_SELECTION = "_is_single_selection"

        fun newInstance(
                selection: List<GenericListItem>,
                isSingleSelection: Boolean,
                title: String = "",
                onConfirmCallback: ((List<GenericListItem>) -> Unit)? = null,
                onCloseCallback: (() -> Unit)? = null
        ): GenericListDialogFragment {
            return GenericListDialogFragment().apply {
                arguments = bundleOf(
                        ARGS_TITLE to title,
                        ARGS_LIST_SELECTION to Gson().toJson(selection),
                        ARGS_IS_SINGLE_SELECTION to isSingleSelection
                )
                this.onConfirm = onConfirmCallback
                this.onClose = onCloseCallback
            }
        }
    }

}