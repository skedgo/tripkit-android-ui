package com.skedgo.tripkit.ui.tripresults

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import com.skedgo.tripkit.ui.utils.isDarkMode
import com.skedgo.tripkit.ui.utils.tint
import com.skedgo.tripkit.routing.ServiceColor

/**
 * Theme-aware tint strategy that applies white tint in dark mode when service colors are disabled.
 * In light mode, returns drawable as-is. In dark mode, tints to white for visibility.
 */
class ThemeAwareApplyTintStrategy(private val resources: Resources) : TransportTintStrategy {
    
    override fun apply(
        remoteIconIsTemplate: Boolean,
        remoteIconIsBranding: Boolean,
        serviceColor: ServiceColor?,
        drawable: Drawable
    ): Drawable {
        // Don't tint branding icons (like Beam) - they should show original logo colors
        if (remoteIconIsBranding) {
            return drawable
        }
        
        if (resources.isDarkMode()) {
            // Dark mode: tint to white for visibility
            return drawable.tint(Color.WHITE)
        }
        // Light mode: return drawable as-is (no tint)
        return drawable
    }
}

