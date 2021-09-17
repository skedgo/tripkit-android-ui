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
import com.skedgo.tripkit.ui.databinding.DialogUpdateBinding

class UpdateModalDialog : DialogFragment() {
    private lateinit var binding: DialogUpdateBinding

    private var title: String? = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogUpdateBinding.inflate(inflater, container, false)
        binding.txtUpdate.text = title
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
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setCancelable(false)
        }
    }

    companion object {
        fun newInstance(title: String): UpdateModalDialog {
            val args = Bundle()
            val fragment = UpdateModalDialog()
            fragment.arguments = args
            fragment.title = title
            return fragment
        }
    }
}