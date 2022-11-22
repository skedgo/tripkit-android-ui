package com.technologies.tripkituisample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.technologies.tripkituisample.autocompletesample.RegionRoutingAutoCompleteSampleActivity
import com.technologies.tripkituisample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.bAutoCompleteRoutesRegion.setOnClickListener {
            startActivity(Intent(this, RegionRoutingAutoCompleteSampleActivity::class.java))
        }
    }
}