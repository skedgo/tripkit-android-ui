package com.skedgo.tripkit.ui.controller.routeviewcontroller

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.route.compose.RouteCardContent
import com.skedgo.tripkit.ui.route.compose.RouteUiModel
import com.skedgo.tripkit.ui.route.styles.RouteUiStyle

@Composable
fun TKUIRouteCardCompose(
    viewModel: TKUIRouteViewModel,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onStartChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSwap: () -> Unit,
    onStartFocused: () -> Unit = {},
    onDestinationFocused: () -> Unit = {},
    style: RouteUiStyle = RouteUiStyle()
) {
    val start by viewModel.start.observeAsState(initial = "")
    val destination by viewModel.destination.observeAsState(initial = "")

    TripKitUITheme {
        RouteCardContent(
            model = RouteUiModel(
                startText = start ?: "",
                destinationText = destination ?: "",
                onStartChange = onStartChange,
                onDestinationChange = onDestinationChange,
                onStartFocused = {
                    viewModel.focusedField = TKUIRouteViewModel.FocusedField.START
                    onStartFocused()
                },
                onDestinationFocused = {
                    viewModel.focusedField = TKUIRouteViewModel.FocusedField.DESTINATION
                    onDestinationFocused()
                },
                onSwap = onSwap,
                onClose = onClose,
                onConfirm = onConfirm,
                canConfirm = viewModel.bothLocationsAreValid()
            ),
            style = style
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun TKUIRouteCardComposePreview() {
    val viewModel = TKUIRouteViewModel().apply {
        startLocation = Location(0.0, 0.0).apply { name = "Current location" }
        destinationLocation = Location(-33.883, 151.206).apply { name = "Central Station" }
    }
    TripKitUITheme {
        TKUIRouteCardCompose(
            viewModel = viewModel,
            onClose = {},
            onConfirm = {},
            onStartChange = {},
            onDestinationChange = {},
            onSwap = {}
        )
    }
}
