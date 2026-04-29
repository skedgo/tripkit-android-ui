package com.skedgo.tripkit.ui.route.styles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Optional style overrides for Route compose UI.
 * WL apps can override only what they need.
 */
@Immutable
data class RouteUiStyle(
    val primaryColor: Color? = null,
    val confirmButtonBackground: Color? = null,
    val titleColor: Color? = null,
    val inputBackground: Color? = null,
    val iconTint: Color? = null,
    val horizontalPadding: Dp? = null,
    val verticalSpacing: Dp? = null
)
