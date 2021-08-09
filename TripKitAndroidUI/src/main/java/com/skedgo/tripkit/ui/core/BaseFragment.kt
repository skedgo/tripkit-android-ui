package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.gson.Gson

/**
 * To act as a super class for all other fragments.
 * Passing ViewDataBinding to include initialization which is common for all fragments
 * that's using databinding
 * Sample usage SampleFragment<FragmentSampleBinding>
 */
abstract class BaseFragment<V : ViewDataBinding> : BaseTripKitFragment() {

    protected val gson = Gson()

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected lateinit var baseView: View

    protected lateinit var binding: V

    protected abstract fun onCreated(savedInstance: Bundle?)

    private var previouslyInitialized = false

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        if(!::binding.isInitialized) {
            binding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
        } else {
            previouslyInitialized = true
        }
        baseView = binding.root

        return baseView
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!previouslyInitialized) {
            onCreated(savedInstanceState)
        }
    }
}