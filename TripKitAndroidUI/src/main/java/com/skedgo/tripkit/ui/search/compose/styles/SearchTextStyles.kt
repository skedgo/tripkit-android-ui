package com.skedgo.tripkit.ui.search.compose.styles

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles

internal object SearchTextStyles {

    val Title: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyLarge

    val SubTitle: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyMedium
}