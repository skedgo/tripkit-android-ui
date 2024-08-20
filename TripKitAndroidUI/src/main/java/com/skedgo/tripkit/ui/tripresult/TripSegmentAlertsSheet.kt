package com.skedgo.tripkit.ui.tripresult

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.TripSegmentAlertDetailsBottomSheetBinding
import me.tatarka.bindingcollectionadapter2.ItemBinding


class TripSegmentAlertsSheetViewModel {
    val items: ObservableList<TripSegmentAlertsItemViewModel> = ObservableArrayList()
    val itemBinding = ItemBinding.of<TripSegmentAlertsItemViewModel>(
        BR.viewModel,
        R.layout.trip_segment_alert_details_item
    )
}

class TripSegmentAlertsSheet : BottomSheetDialogFragment() {
    var viewModel = TripSegmentAlertsSheetViewModel()

    companion object {
        fun newInstance(viewModels: List<TripSegmentAlertsItemViewModel>): TripSegmentAlertsSheet {
            val newSheet = TripSegmentAlertsSheet()
            viewModels.forEach {
                newSheet.viewModel.items.add(it)
            }
            return newSheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<TripSegmentAlertDetailsBottomSheetBinding>(
            inflater,
            R.layout.trip_segment_alert_details_bottom_sheet,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        binding.alertsList.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog?
            setupFullHeight(bottomSheetDialog!!)
        }
        return dialog
    }


    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout?
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.isHideable = false
            behavior.skipCollapsed = true
            val layoutParams = bottomSheet.layoutParams
            val windowHeight = getWindowHeight()
            if (layoutParams != null) {
                layoutParams.height = windowHeight
            }
            bottomSheet.layoutParams = layoutParams
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun getWindowHeight(): Int { // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }


}