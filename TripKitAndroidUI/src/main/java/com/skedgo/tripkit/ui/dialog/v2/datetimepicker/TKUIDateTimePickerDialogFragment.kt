package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.viewModels
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseDialog
import com.skedgo.tripkit.ui.databinding.FragmentTkuiDateTimePickerDialogBinding

class TKUIDateTimePickerDialogFragment : BaseDialog<FragmentTkuiDateTimePickerDialogBinding>() {

    companion object {
        fun newInstance(config: DateTimePickerConfig) = TKUIDateTimePickerDialogFragment().apply {
            viewModel.setup(config)
        }
    }

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_date_time_picker_dialog
    override val isFullScreen: Boolean
        get() = true
    override val isFullWidth: Boolean
        get() = true
    override val isTransparentBackground: Boolean
        get() = true

    private val viewModel: TKUIDateTimePickerDialogViewModel by viewModels()

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().timePickerComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreated(savedInstanceState: Bundle?) {
        TODO("Not yet implemented")
    }
}