package com.skedgo.tripkit.ui.locationpointer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.AndroidGeocoder
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.controller.utils.LocationField
import com.skedgo.tripkit.ui.core.BaseTripKitFragment
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.databinding.FragmentLocationPointerBinding
import javax.inject.Inject

class LocationPointerFragment() : BaseTripKitFragment() {

    private var onCloseListener: View.OnClickListener? = null
    private var closeButton: ImageButton? = null

    @Inject
    lateinit var viewModelFactory: LocationPointerViewModelFactory
    private val viewModel: LocationPointerViewModel by viewModels { viewModelFactory }
    private var map: GoogleMap? = null
    private var geocoder: AndroidGeocoder? = null
    private var cachedMarker: Marker? = null
    private var locationField: LocationField = LocationField.NONE

    private lateinit var binding: FragmentLocationPointerBinding
    private var listener: LocationPointerListener? = null

    companion object {
        fun newInstance() = LocationPointerFragment()
    }

    override fun onAttach(context: Context) {
        TripKitUI.getInstance().locationPointerComponent().inject(this)
        geocoder = AndroidGeocoder(context)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setLocationText(getString(R.string.choose_on_map))
    }

    fun setMap(map: GoogleMap, listener: LocationPointerListener? = null) {
        this.map = map
        this.map!!.setOnCameraMoveStartedListener {
            if (it == REASON_GESTURE) {
                viewModel.mapMoveStarted()
            }
        }
        this.map!!.setOnCameraIdleListener {
            viewModel.mapIdleThrottle.onNext(map.cameraPosition.target)
        }
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLocationPointerBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        closeButton = binding.closeButton
        closeButton?.setOnClickListener {
            listener?.onClose()
        }

        onCloseListener?.let {
            binding.closeButton.setOnClickListener(it)
        }

        binding.iconInfo.setOnClickListener {
            map?.let {
                val location = Location(
                    it.cameraPosition.target.latitude,
                    it.cameraPosition.target.longitude
                )
                location.address = viewModel.locationText.value

                listener?.loadPoiDetails(location)
            }
        }

        binding.root.post {
            map?.setPadding(0, 0, 0, binding.root.measuredHeight)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.doneClicked.observable.subscribe {
            if (viewModel.currentLatLng.latitude != 0.0 && viewModel.currentLatLng.longitude != 0.0) {
                val newLocation = Location(
                    viewModel.currentLatLng.latitude,
                    viewModel.currentLatLng.longitude
                ).apply {
                    address = viewModel.currentAddress
                }
                viewModel.saveLocation(newLocation)
                listener?.onDone(newLocation, locationField)
            }
        }.addTo(autoDisposable)
    }

    fun setOnCloseListener(listener: View.OnClickListener) {
        this.onCloseListener = listener
        closeButton?.setOnClickListener(listener)
    }

    fun setListener(listener: LocationPointerListener) {
        this.listener = listener
    }

    fun setLocationField(locationField: LocationField) {
        this.locationField = locationField
    }

    interface LocationPointerListener {
        fun onDone(location: Location, field: LocationField = LocationField.NONE)
        fun loadPoiDetails(location: Location)
        fun onClose()
    }
}
