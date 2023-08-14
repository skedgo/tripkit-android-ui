package com.skedgo.tripkit.ui.controller.routeviewcontroller

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.BaseFragment
import com.skedgo.tripkit.ui.databinding.FragmentTkuiRouteBinding
import com.skedgo.tripkit.ui.search.LocationSearchFragment

class TKUIRouteFragment : BaseFragment<FragmentTkuiRouteBinding>() {

    private var bounds: LatLngBounds? = null
    private var near: LatLng? = null

    var origin: Location? = null
    var destination: Location? = null

    private var locationSearchCardFragment: LocationSearchFragment? = null

    override val layoutRes: Int
        get() = R.layout.fragment_tkui_route

    override fun onCreated(savedInstance: Bundle?) {

    }

    override val observeAccessibility: Boolean = false

    override fun getDefaultViewForAccessibility(): View? = null
}