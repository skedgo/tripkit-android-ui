package com.technologies.tripkituisample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.technologies.tripkituisample.autocompleter.AutocompleterActivity
import com.technologies.tripkituisample.databinding.ActivityMainBinding
import com.technologies.tripkituisample.location_search.LocationSearchActivity
import com.technologies.tripkituisample.routingresultview.RoutingResultActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.bAutocompleter.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, AutocompleterActivity::class.java)
            )
        }

        binding.bLocationSearch.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, LocationSearchActivity::class.java)
            )
        }

        binding.bRoutingResult.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, RoutingResultActivity::class.java)
            )
        }
    }
}