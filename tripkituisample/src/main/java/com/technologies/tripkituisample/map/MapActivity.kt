package com.technologies.tripkituisample.map

import android.os.Bundle
import android.os.Handler
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.ui.core.BaseActivity
import com.skedgo.tripkit.ui.map.home.TripKitMapFragment
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityMapBinding

class MapActivity : BaseActivity<ActivityMapBinding>() {

    override val layoutRes: Int
        get() = R.layout.activity_map

    override fun onCreated(instance: Bundle?) {

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as TripKitMapFragment
        mapFragment.getMapAsync { _ ->
            Handler().postDelayed({
                mapFragment.moveToLatLng(
                    LatLng(-27.470125, 153.021072)
                )
            }, 2000)

        }
    }
}