package com.skedgo.tripkit.ui.tripresults.compose.styles

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles

internal object TripResultStyles {

    val BodyLarge: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyLarge

    val BodySmall: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodySmall

    val LabelLarge: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.labelLarge
}