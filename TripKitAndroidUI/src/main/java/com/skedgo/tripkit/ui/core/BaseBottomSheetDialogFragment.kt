package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment<V : ViewDataBinding> : BottomSheetDialogFragment() {

    companion object {
        const val ARG_SHOW_BACKGROUND_OVERLAY = "ARG_SHOW_BACKGROUND_OVERLAY"
    }

    protected lateinit var binding: V
    protected lateinit var baseView: View

    @get:LayoutRes
    protected abstract val layoutRes: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        baseView = binding.root
        return baseView
    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
//        return object : BottomSheetDialog(requireContext(), theme) {
//            override fun onAttachedToWindow() {
//                super.onAttachedToWindow()
//
//                window?.apply {
//                    // Let clicks fall through to window below
//                    clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//                    setDimAmount(0f)
//                    setFlags(
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                    )
//
//                    // ⚠️ This is key: disable focus so lower windows receive touch
//                    attributes.flags = attributes.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                }
//            }
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
////        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
////            override fun onGlobalLayout() {
////                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
////
////                val dialog = dialog as? BottomSheetDialog ?: return
////                val bottomSheet = dialog.findViewById<FrameLayout>(
////                    com.google.android.material.R.id.design_bottom_sheet
////                ) ?: return
////
////                bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
////                bottomSheet.requestLayout()
////
////                val behavior = BottomSheetBehavior.from(bottomSheet)
////                behavior.peekHeight = 0
////                behavior.state = BottomSheetBehavior.STATE_EXPANDED
////            }
////        })
//    }
}
