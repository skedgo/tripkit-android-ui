package com.skedgo.tripkit.ui.core

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

open class BaseTripKitFragment : Fragment() {
    var onCloseButtonListener: View.OnClickListener? = null

    fun setOnCloseButtonListener(listener:(View?) -> Unit) {
        this.onCloseButtonListener = View.OnClickListener { v -> listener(v) }
    }


    protected val autoDisposable = AutoDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoDisposable.bindTo(this.lifecycle)
    }
}