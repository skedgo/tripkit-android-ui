package com.technologies.tripkituisample.homeviewcontroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityHomeViewControllerBinding

class HomeViewControllerActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeViewControllerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home_view_controller)

        val homeControllerFragment = TKUIHomeViewControllerFragment
            .load(
                activity = this@HomeViewControllerActivity,
                containerId = R.id.homeFragment,
                defaultLocation = LatLng(-27.470125, 153.021072)
            ) {
                if(it == 0) {
                    binding.etSearch.visibility = View.VISIBLE
                } else {
                    binding.etSearch.visibility = View.GONE
                }
            }

        binding.etSearch.setOnClickListener {
            homeControllerFragment.loadSearchCardFragment()
        }
    }
}