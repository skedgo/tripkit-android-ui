package com.skedgo.tripkit.ui.timetables.styles

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles

internal object TimetableTextStyles {

    val ServiceItemTitle: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyLarge

    val ServiceItemTimeValue: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.headlineSmall

    val ServiceItemTimeUnit: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyLarge

    val ServiceItemStatusText: TextStyle
        @Composable get() = TripKitComposeTextStyles.current.bodyMedium
}