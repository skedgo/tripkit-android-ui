package com.skedgo.tripkit.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.DialogPdfDisplayBinding

class TermsOfUseDialogFragment(private val listener: Listener) : DialogFragment() {

    private lateinit var binding: DialogPdfDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogPdfDisplayBinding.inflate(inflater, container, false)

        setupWebView()
        setupListener()
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog
        if (dialog != null) {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun setupWebView() {
        binding.pdfView.fromAsset(getString(R.string.terms_of_use_file_name))
            .onPageScroll { _, positionOffset ->
                if (positionOffset >= 1) {
                    binding.btnAccept.isEnabled = true
                }
            }.load()
    }

    private fun setupListener() {
        binding.btnAccept.setOnClickListener {
            it.isEnabled = false
            listener.onAccept()
        }
    }

    interface Listener {
        fun onAccept()
    }
}