package com.technologies.tripkituisample.location_search

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.ui.search.LocationSearchFragment
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityLocationSearchBinding

class LocationSearchActivity : AppCompatActivity() {

    lateinit var binding: ActivityLocationSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_location_search)

        binding.ibBack.setOnClickListener {
            this@LocationSearchActivity.finish()
        }

        val mockBounds = LatLngBounds(
            LatLng(-34.02712944455638, 151.04664601385593),
            LatLng(-33.67627303081665, 151.31631027907133)
        )

        val mockNear = LatLng(-33.85188179467262, 151.1814783141017)

        val locationSearchFragment = LocationSearchFragment.Builder()
            .withBounds(mockBounds)
            .near(mockNear)
            .withHint(getString(R.string.where_do_you_want_to_go_question))
            .allowDropPin(true)
            .showBackButton(false)
            .build().apply {

            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, locationSearchFragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }
}