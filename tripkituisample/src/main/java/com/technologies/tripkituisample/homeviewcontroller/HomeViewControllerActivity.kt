package com.technologies.tripkituisample.homeviewcontroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.technologies.tripkituisample.R

class HomeViewControllerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_view_controller)

        TKUIHomeViewControllerFragment
            .load(
                activity = this@HomeViewControllerActivity,
                containerId = R.id.homeFragment,
                defaultLocation = LatLng(-27.470125, 153.021072)
            )
    }
}