package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
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
    val actionButton = MutableLiveData<ActionButton>()
    var tag: String = ""

    init {
        update(context, button)
    }

    fun update(
        context: Context,
        button: ActionButton,
        preserveDynamicState: Boolean = false
    ) {
        val previousButton = actionButton.value
        val canPreserveState = preserveDynamicState && previousButton != null &&
            previousButton.icon != 0 && previousButton.tag == button.tag
        // Preserve the style with the stateful label/icon during stale realtime refreshes.
        // Otherwise a preserved Mute label can acquire the incoming Alert Me emphasis.
        val presentation = if (canPreserveState && previousButton != null) {
            button.copy(
                isPrimary = previousButton.isPrimary,
                useIconTint = previousButton.useIconTint
            )
        } else button
        this.actionButton.value = presentation
        // Favorite and alert actions can be updated asynchronously by their handlers. A
        // realtime trip refresh may ask us to apply older action metadata before the backing
        // action has caught up, so capture the user-visible state before applying that refresh.
        val preservedTitle = title.get().takeIf { canPreserveState }
        val preservedIcon = icon.get().takeIf { canPreserveState }

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

        if (presentation.isPrimary) {
            this.iconTint.set(Color.WHITE)
            this.outlineTint.set(Color.TRANSPARENT)
            val backgroundColorList =
                intArrayOf(appTintColor, 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
            this.background.set(ContextCompat.getDrawable(context, R.drawable.bg_circle_primary))
        } else {
            // A restored placeholder or previous action may have supplied a tint. Always
            // reset it so an untinted icon renders the same on creation and rebinding.
            this.iconTint.set(
                if (presentation.useIconTint) ContextCompat.getColor(context, R.color.labelPrimary)
                else null
            )
            // this.outlineTint.set(ContextCompat.getColor(context, R.color.black4))
            this.outlineTint.set(Color.TRANSPARENT)
            val backgroundColorList = intArrayOf(0, 0)
            this.backgroundTint.set(ColorStateList(stateList, backgroundColorList))
            this.background.set(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_circle_transparent_black_border
                )
            )
        }

        preservedTitle?.let(title::set)
        preservedIcon?.let(icon::set)
    }

    fun showSpinner(show: Boolean) {
        showSpinner.set(show)
    }
}
