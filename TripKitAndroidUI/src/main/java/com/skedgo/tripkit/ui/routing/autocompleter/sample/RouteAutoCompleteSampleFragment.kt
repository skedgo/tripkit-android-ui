package com.skedgo.tripkit.ui.routing.autocompleter.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentRouteAutoCompletSampleBinding
import com.skedgo.tripkit.ui.routing.autocompleter.RouteAutocompleteViewModel
import javax.inject.Inject

class RouteAutoCompleteSampleFragment : BaseFragment<FragmentRouteAutoCompletSampleBinding>() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override val layoutRes: Int
        get() = R.layout.fragment_route_auto_complet_sample

    private val viewModel: RouteAutocompleteViewModel by viewModels { viewModelFactory }

    override fun onCreated(savedInstance: Bundle?) {
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.regionRoutes.observe(this) {
            Log.e("RouteAutoCompleteSample", gson.toJson(it))
        }
    }

    override val observeAccessibility: Boolean
        get() = false

    override fun getDefaultViewForAccessibility(): View? = null

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().autoCompleteRoutingComponent().inject(this)
        super.onAttach(context)
    }
}