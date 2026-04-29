package com.skedgo.tripkit.ui.route.compose

/**
 * Lightweight UI model for the Route card shell.
 * Keeps compose UI independent from heavy routing/domain objects.
 */
data class RouteUiModel(
    val startText: String,
    val destinationText: String,
    val onStartChange: (String) -> Unit,
    val onDestinationChange: (String) -> Unit,
    val onStartFocused: () -> Unit,
    val onDestinationFocused: () -> Unit,
    val onSwap: () -> Unit,
    val onClose: () -> Unit,
    val onConfirm: () -> Unit,
    val canConfirm: Boolean
)
