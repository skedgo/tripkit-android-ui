package com.technologies.tripkituisample.autocompletesample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityRegionRoutingAutoCompleteSampleBinding

class RegionRoutingAutoCompleteSampleActivity : AppCompatActivity() {

    lateinit var binding: ActivityRegionRoutingAutoCompleteSampleBinding
    lateinit var viewModel: RegionRoutingAutoCompleteViewModel

    private val adapter: AutoCompleteResultAdapter by lazy {
        AutoCompleteResultAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_region_routing_auto_complete_sample)
        binding.lifecycleOwner = this
        viewModel = ViewModelProvider(this).get(RegionRoutingAutoCompleteViewModel::class.java)
        binding.viewModel = viewModel

        initObserver()
        initView()
    }

    private fun initObserver() {
        viewModel.regionRoutes.observe(this) {
            it?.let { regionRoutes ->
                adapter.collection = regionRoutes.map {
                    AutoCompleteResultItem(it.id, it.routeName, it.routeDescription)
                }
            }
        }
    }

    private fun initView() {
        binding.rvData.adapter = adapter
    }
}