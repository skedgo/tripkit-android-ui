package com.skedgo.tripkit.ui.poidetails

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.ARG_IS_FAVORITE
import com.skedgo.tripkit.ui.ARG_LOCATION
import com.skedgo.tripkit.ui.ARG_SHOW_CLOSE_BUTTON
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.databinding.PoiDetailsFragmentBinding
import timber.log.Timber
import javax.inject.Inject


const val BUTTON_GO = 1
const val BUTTON_FAVORITE = 2
class PoiDetailsFragment : BaseTripKitFragment()  {
    @Inject lateinit var viewModelFactory: PoiDetailsViewModelFactory
    lateinit var viewModel: PoiDetailsViewModel

    val buttonClick = PublishRelay.create<Int>()
    override fun onAttach(context: Context) {
        TripKitUI.getInstance().inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PoiDetailsViewModel::class.java)
    }

    fun toggleFavorite(isFavorite: Boolean) {
        viewModel.setFavorite(requireContext(), isFavorite)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val isFavorite = arguments?.getBoolean(ARG_IS_FAVORITE, false) ?: false
        val showCloseButton = arguments?.getBoolean(ARG_SHOW_CLOSE_BUTTON, false) ?: false
        val location = arguments?.getParcelable(ARG_LOCATION) as Location?

        viewModel.showCloseButton.set(showCloseButton)
        viewModel.setFavorite(requireContext(), isFavorite)

        location?.let { viewModel.start(it) }

        val binding = PoiDetailsFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.goButton.setOnClickListener { buttonClick.accept(BUTTON_GO) }
        binding.favoriteButton.setOnClickListener { buttonClick.accept(BUTTON_FAVORITE) }

        binding.closeButton.setOnClickListener(onCloseButtonListener)
        return binding.root
    }

    fun updateLocation(location: Location){
        viewModel.start(location)
    }

    class Builder(val location: Location) {
        private var showCloseButton = false
        private var isFavorite = false
        fun showCloseButton(showCloseButton: Boolean): Builder {
            this.showCloseButton = showCloseButton
            return this
        }

        fun isFavorite(favorite: Boolean): Builder {
            isFavorite = favorite
            return this
        }

        fun build() = PoiDetailsFragment().apply {
                arguments = Bundle().apply {
                    this.putBoolean(ARG_SHOW_CLOSE_BUTTON, showCloseButton)
                    this.putParcelable(ARG_LOCATION, location)
                    this.putBoolean(ARG_IS_FAVORITE, isFavorite)
                }
            }
    }

}