package com.skedgo.tripkit.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.ui.databinding.FragmentGenericNoteBinding
import com.skedgo.tripkit.ui.utils.AccessibilityDefaultViewManager

class GenericNoteDialogFragment : DialogFragment(), GenericNoteDialogFragmentHandler {

    private val accessibilityDefaultViewManager: AccessibilityDefaultViewManager by lazy {
        AccessibilityDefaultViewManager(context)
    }

    lateinit var binding: FragmentGenericNoteBinding

    private val viewModel: GenericNoteViewModel by viewModels()

    private var onDone: ((String) -> Unit)? = null

    override fun onDone() {
        onDone?.invoke(viewModel.inputValue.get().toString())
        dialog?.dismiss()
    }

    override fun onClose() {
        dialog?.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGenericNoteBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkArguments()
        accessibilityDefaultViewManager.setDefaultViewForAccessibility(binding.genericListTvTitle)
        accessibilityDefaultViewManager.setAccessibilityObserver()
    }

    override fun onResume() {
        super.onResume()

        accessibilityDefaultViewManager.focusAccessibilityDefaultView(false)
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

    private fun checkArguments() {
        arguments?.let {
            if (it.containsKey(ARG_TITLE)) {
                viewModel.fragmentTitle.set(it.getString(ARG_TITLE, ""))
            }

            if (it.containsKey(ARG_INPUT_VALUE)) {
                viewModel.inputValue.set(it.getString(ARG_INPUT_VALUE))
            }

            if (it.containsKey(ARG_VIEW_ONLY_MODE)) {
                viewModel.viewOnly.set(it.getBoolean(ARG_VIEW_ONLY_MODE, false))
            }
        }
    }

    companion object {

        private const val ARG_TITLE = "arg_detail_title"
        private const val ARG_INPUT_VALUE = "arg_input_value"
        private const val ARG_VIEW_ONLY_MODE = "arg_view_only_mode"

        @JvmStatic
        fun newInstance(
            fragmentTitle: String = "",
            inputValue: String = "",
            viewOnlyMode: Boolean = false,
            doneCallback: ((String) -> Unit)? = null
        ) = GenericNoteDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, fragmentTitle)
                putString(ARG_INPUT_VALUE, inputValue)
                putBoolean(ARG_VIEW_ONLY_MODE, viewOnlyMode)
            }
            onDone = doneCallback
        }
    }
}