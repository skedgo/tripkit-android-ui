package com.technologies.tripkituisample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.technologies.tripkituisample.autocompleter.AutocompleterActivity
import com.technologies.tripkituisample.databinding.ActivityMainBinding
import com.technologies.tripkituisample.homeviewcontroller.HomeViewControllerActivity
import com.technologies.tripkituisample.location_search.LocationSearchActivity
import com.technologies.tripkituisample.map.MapActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        applySampleWindowInsets()
        binding.appearanceGroup.check(when (SampleAppearance.mode(this)) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> R.id.appearanceLight
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> R.id.appearanceDark
            else -> R.id.appearanceSystem
        })
        binding.appearanceGroup.setOnCheckedChangeListener { _, id ->
            SampleAppearance.select(this, when (id) {
                R.id.appearanceLight -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                R.id.appearanceDark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
        }

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

        binding.bLocationSearch.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, LocationSearchActivity::class.java)
            )
        }

        binding.bMap.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, HomeViewControllerActivity::class.java)
            )
        }
    }
}
