package com.skedgo.tripkit.ui.poidetails

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.ARG_LOCATION
import com.skedgo.tripkit.ui.ARG_SHOW_CLOSE_BUTTON
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.databinding.PoiDetailsFragmentBinding
import timber.log.Timber
import javax.inject.Inject


class PoiDetailsFragment : BaseTripKitFragment()  {
    @Inject lateinit var viewModelFactory: PoiDetailsViewModelFactory
    lateinit var viewModel: PoiDetailsViewModel

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PoiDetailsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        val location = arguments?.getParcelable(ARG_LOCATION) as Location?

        viewModel.showCloseButton.set(showCloseButton)
        location?.let { viewModel.start(it) }

        val binding = PoiDetailsFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.closeButton.setOnClickListener(onCloseButtonListener)
        return binding.root
    }

    class Builder(val location: Location) {
        private var showCloseButton = false

        fun showCloseButton(showCloseButton: Boolean): Builder {
            this.showCloseButton = showCloseButton
            return this
        }

        fun build() = PoiDetailsFragment().apply {
                arguments = Bundle().apply {
                    this.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)
                    this.putParcelable(ARG_LOCATION, location)
                }
            }
    }

}