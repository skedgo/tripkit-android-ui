package com.skedgo.tripkit.ui.utils

import android.content.res.Configuration
import android.content.res.Resources

/**
 * Checks if the app is currently in dark mode.
 */
fun Resources.isDarkMode(): Boolean {
    val nightModeFlags = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}

