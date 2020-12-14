package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton


interface ActionButtonClickListener {
    fun onItemClick(tag: String, viewModel: ActionButtonViewModel)
}

class ActionButtonViewModel constructor (context: Context, button: ActionButton){
    val showSpinner = ObservableBoolean(false)
    val title = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val iconTint = ObservableField<Int>()
    val outlineTint = ObservableField<Int>()
    val backgroundTint = ObservableField<ColorStateList>()
    var tag : String = ""

    init {
        update(context, button)
    }

    fun update(context: Context, button: ActionButton) {
        this.showSpinner.set(false)
        this.title.set(button.text)
        this.icon.set(ContextCompat.getDrawable(context, button.icon))
        this.tag = button.tag
        val stateList = arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf())

        if (button.isPrimary) {
            this.iconTint.set(Color.WHITE)
            this.outlineTint.set(Color.TRANSPARENT)
            val backgroundColorList = intArrayOf(ContextCompat.getColor(context, R.color.colorPrimary), 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
        } else {
            this.iconTint.set(ContextCompat.getColor(context, R.color.black1))
            this.outlineTint.set(ContextCompat.getColor(context, R.color.black4))
            val backgroundColorList = intArrayOf(0, 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
        }
    }
}