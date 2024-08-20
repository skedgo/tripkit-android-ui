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
import com.google.gson.Gson
import com.skedgo.tripkit.ui.databinding.DialogGenericListDisplayBinding
import com.skedgo.tripkit.ui.utils.fromJson
import com.skedgo.tripkit.ui.utils.observe

class GenericListDisplayDialogFragment : DialogFragment(), GenericListDisplayDialogFragmentHandler {

    private val viewModel: GenericListViewModel by viewModels()

    private lateinit var binding: DialogGenericListDisplayBinding
    private lateinit var adapter: GenericListDisplayAdapter

    private var onClose: (() -> Unit)? = null

    override fun onClose() {
        onClose?.invoke()
        dialog?.dismiss()
    }

    override fun onStart() {
        super.onStart()

        dialog?.apply {
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogGenericListDisplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBinding()
        initViews()
        initObserver()
        checkArgs()
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
    }

    private fun initViews() {
        adapter = GenericListDisplayAdapter()
        binding.genericListRvSelection.adapter = adapter
    }


    private fun checkArgs() {
        arguments?.let {
            if (it.containsKey(ARGS_TITLE)) {
                viewModel.setTitle(it.getString(ARGS_TITLE, ""))
            }

            if (it.containsKey(ARGS_LIST_SELECTION)) {
                viewModel.setListSelection(Gson().fromJson(it.getString(ARGS_LIST_SELECTION, "")))
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

        fun newInstance(
            selection: List<GenericListItem>,
            title: String = "",
            onCloseCallback: (() -> Unit)? = null
        ): GenericListDisplayDialogFragment {
            return GenericListDisplayDialogFragment().apply {
                arguments = bundleOf(
                    ARGS_TITLE to title,
                    ARGS_LIST_SELECTION to Gson().toJson(selection)
                )
                this.onClose = onCloseCallback
            }
        }
    }
}