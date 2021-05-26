package com.skedgo.tripkit.ui.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Show the soft keyboard.
 * @param activity the current activity
 */
fun showKeyboard(activity: Activity?) {
    if (activity == null) return
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

/**
 * Hide the soft keyboard.
 * @param activity the current activity
 */
fun hideKeyboard(context: Context, focus: View?) {
    if (focus == null) return

    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(focus.windowToken, 0)
}

fun defocusAndHideKeyboard(context: Context, focus: View?) {
    hideKeyboard(context, focus)
    focus?.clearFocus()
}