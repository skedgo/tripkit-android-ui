package com.skedgo.tripkit.ui.model

import android.content.Context
import android.view.View
import java.io.Serializable


interface TripKitButtonConfigurator : Serializable {
    fun configureButton(context: Context, view: View, data: Any)
}