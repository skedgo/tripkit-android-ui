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
import com.skedgo.tripkit.ui.utils.DynamicAppColor


interface ActionButtonClickListener {
    fun onItemClick(tag: String, viewModel: ActionButtonViewModel, context: Context)
}

class ActionButtonViewModel constructor(context: Context, button: ActionButton) {
    val showSpinner = ObservableBoolean(false)
    val title = ObservableField<String>()
    val icon = ObservableField<Drawable>()
    val iconTint = ObservableField<Int>()
    val outlineTint = ObservableField<Int>()
    val backgroundTint = ObservableField<ColorStateList>()
    val background = ObservableField<Drawable>()
    var tag: String = ""

    init {
        update(context, button)
    }

    fun update(context: Context, button: ActionButton) {
        this.showSpinner.set(false)
        this.title.set(button.text)
        // Only set icon if it's a valid resource ID (not 0)
        if (button.icon != 0) {
            this.icon.set(ContextCompat.getDrawable(context, button.icon))
        }
        this.tag = button.tag
        val stateList = arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf())

        val appTintColor = DynamicAppColor.getAppColors()?.tintColor?.run {
            Color.rgb(red, green, blue)
        } ?: ContextCompat.getColor(context, R.color.colorPrimary)

        if (button.isPrimary) {
            this.iconTint.set(Color.WHITE)
            this.outlineTint.set(Color.TRANSPARENT)
            val backgroundColorList =
                intArrayOf(appTintColor, 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
            this.background.set(ContextCompat.getDrawable(context, R.drawable.bg_circle_primary))
        } else {
            if (button.useIconTint)
                this.iconTint.set(ContextCompat.getColor(context, R.color.black1))
            this.outlineTint.set(ContextCompat.getColor(context, R.color.black4))
            val backgroundColorList = intArrayOf(0, 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
            this.background.set(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_circle_transparent_black_border
                )
            )
        }
    }

    fun showSpinner(show: Boolean) {
        showSpinner.set(show)
    }
}