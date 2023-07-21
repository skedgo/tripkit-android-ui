package com.technologies.tripkituisample.autocompleter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.gson.Gson
import com.technologies.tripkituisample.R
import com.technologies.tripkituisample.databinding.ActivityAutoCompleterBinding

class AutocompleterActivity : AppCompatActivity() {

    lateinit var binding: ActivityAutoCompleterBinding

    lateinit var autocompleterViewModel: AutocompleterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_auto_completer)
        autocompleterViewModel = ViewModelProvider(this).get(AutocompleterViewModel::class.java)

        binding.ibBack.setOnClickListener {
            this@AutocompleterActivity.finish()
        }

        initBindings()
    }

    private fun initBindings() {
        binding.lifecycleOwner = this
        binding.viewModel = autocompleterViewModel
        autocompleterViewModel.regionRoutes.observe(this) {
            binding.rvResults.adapter = AutocompleterAdapter(it)
        }
    }

}