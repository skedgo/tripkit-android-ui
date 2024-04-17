package com.skedgo.tripkit.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.ui.databinding.DialogGenericWebviewBinding
import com.skedgo.tripkit.ui.utils.AccessibilityDefaultViewManager

class GenericWebViewDialogFragment : DialogFragment(), GenericWebViewDialogFragmentHandler {

    private val accessibilityDefaultViewManager: AccessibilityDefaultViewManager by lazy {
        AccessibilityDefaultViewManager(context)
    }

    private val viewModel: GenericWebViewViewModel by viewModels()

    private lateinit var binding: DialogGenericWebviewBinding

    private var onConfirm: (() -> Unit)? = null
    private var onClose: (() -> Unit)? = null

    override fun onConfirm() {
        onConfirm?.invoke()
        dialog?.dismiss()
    }

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
        binding = DialogGenericWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBinding()
        checkArgs()

        accessibilityDefaultViewManager.setDefaultViewForAccessibility(binding.genericWebViewTvTitle)
        accessibilityDefaultViewManager.setAccessibilityObserver()
    }

    override fun onResume() {
        super.onResume()

        accessibilityDefaultViewManager.focusAccessibilityDefaultView(false)
    }

    private fun initBinding() {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handler = this
    }

    private fun checkArgs() {
        arguments?.let {
            if (it.containsKey(ARGS_TERMS_LINK))
                loadTermsView(it.getString(ARGS_TERMS_LINK))

            if (it.containsKey(ARGS_TITLE)) {
                viewModel.setTitle(it.getString(ARGS_TITLE, ""))
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadTermsView(url: String?) {
        url?.let {
            binding.genericWebViewWvTerms.apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    settings.safeBrowsingEnabled = true
                loadUrl(it)
            }
        }
    }

    companion object {

        private const val ARGS_TITLE = "_title"
        private const val ARGS_TERMS_LINK = "_terms_link"

        fun newInstance(
            title: String = "",
            onConfirmCallback: (() -> Unit)? = null,
            onCloseCallback: (() -> Unit)? = null,
            termsLink: String? = null,
        ): GenericWebViewDialogFragment {
            return GenericWebViewDialogFragment().apply {
                arguments = bundleOf(
                    ARGS_TITLE to title,
                    ARGS_TERMS_LINK to termsLink
                )
                this.onConfirm = onConfirmCallback
                this.onClose = onCloseCallback
            }
        }
    }


}
