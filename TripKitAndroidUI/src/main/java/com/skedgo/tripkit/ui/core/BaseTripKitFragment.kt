package com.skedgo.tripkit.ui.core

import android.os.Bundle
import androidx.fragment.app.Fragment

open class BaseTripKitFragment : Fragment() {
    protected val autoDisposable = AutoDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoDisposable.bindTo(this.lifecycle)
    }
}