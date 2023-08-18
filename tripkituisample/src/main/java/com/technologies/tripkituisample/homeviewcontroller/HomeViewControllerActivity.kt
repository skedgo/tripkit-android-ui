package com.technologies.tripkituisample.homeviewcontroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import com.skedgo.geocoding.LatLng
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.controller.homeviewcontroller.TKUIHomeViewControllerFragment
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityHomeViewControllerBinding

class HomeViewControllerActivity : AppCompatActivity() {

    lateinit var binding: ActivityHomeViewControllerBinding

    private val favoriteProvider = SampleFavoritesSuggestionProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home_view_controller)

        addSampleFavoriteData()

        val homeControllerFragment = TKUIHomeViewControllerFragment
            .load(
                activity = this@HomeViewControllerActivity,
                containerId = R.id.homeFragment,
                defaultLocation = LatLng(-27.470125, 153.021072),
                favoriteSuggestionProvider = favoriteProvider
            ) {
                if (it == 0) {
                    binding.etSearch.visibility = View.VISIBLE
                } else {
                    binding.etSearch.visibility = View.GONE
                }
            }

        binding.etSearch.setOnClickListener {
            homeControllerFragment.loadSearchCardFragment()
        }
    }

    private fun addSampleFavoriteData() {
        favoriteProvider.saveFavorite(
            Location(-27.4822985, 152.9859037),
            0,
            SampleFavoritesSuggestionProvider.SampleAdditionalData(
                "Sample Favorite 1", "Favorite's Address 1"
            )
        )

        favoriteProvider.saveFavorite(
            Location(-27.4744549, 152.9889291),
            1,
            SampleFavoritesSuggestionProvider.SampleAdditionalData(
                "Sample Favorite 2", "Favorite's Address 2"
            )
        )

        favoriteProvider.saveFavorite(
            Location(-27.4750639, 152.9844771),
            1,
            SampleFavoritesSuggestionProvider.SampleAdditionalData(
                "Sample Favorite 3", "Favorite's Address 3"
            )
        )

        favoriteProvider.saveHome(
            Location(-27.3816409, 153.0118921),
            SampleFavoritesSuggestionProvider.SampleAdditionalData(
                "HOME", "Home Address"
            )
        )
    }
}