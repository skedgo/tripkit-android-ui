package com.skedgo.tripkit.ui.trippreview.service

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme

enum class ServiceStopLineUiDirection {
    START, MIDDLE, END
}

data class ServiceAlertUiState(
    val id: String,
    val title: String,
    val text: String,
    val severity: String?,
    val url: String?
)

data class ServiceStopUiState(
    val id: String,
    val scheduledTime: String,
    val scheduledTimeTextColor: Int?,
    val stopName: String,
    val stopNameColor: Int?,
    val lineColor: Int,
    val lineDirection: ServiceStopLineUiDirection,
    val isTravelled: Boolean,
    val isWheelchairAccessible: Boolean
)

data class ServiceOccupancyCarriageUiState(
    val id: String,
    val tintColor: Int,
    val drawableRes: Int
)

data class ServicePageUiState(
    val stationName: String,
    val modeIconRes: Int?,
    val serviceColor: Int?,
    val serviceNumber: String,
    val statusText: String,
    val statusTextColor: Int?,
    val tertiaryText: String,
    val showExpandableMenu: Boolean,
    val isExpandableMenuExpanded: Boolean,
    val showWheelchairAccessible: Boolean,
    val wheelchairAccessibleText: String,
    val wheelchairIconRes: Int?,
    val showBicycleAccessible: Boolean,
    val showOccupancyInfo: Boolean,
    val occupancyIconRes: Int?,
    val occupancyText: String,
    val lastUpdatedText: String,
    val occupancyCars: List<ServiceOccupancyCarriageUiState>,
    val alerts: List<ServiceAlertUiState>,
    val stops: List<ServiceStopUiState>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val showCloseButton: Boolean,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean
)

@Composable
fun ServiceTripPreviewItemScreen(
    state: ServicePageUiState,
    onCloseClicked: () -> Unit,
    onExpandToggle: () -> Unit,
    onAlertClicked: (ServiceAlertUiState) -> Unit,
    onStopClicked: (ServiceStopUiState) -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .pointerInput(onPreviousPage, onNextPage) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent().changes.firstOrNull() ?: continue
                        if (!down.pressed) continue
                        val start = down.position
                        var end = start
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.let { change ->
                                end = change.position
                            }
                            if (event.changes.none { it.pressed }) {
                                break
                            }
                        }
                        val distanceX = end.x - start.x
                        val distanceY = end.y - start.y
                        if (kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY) &&
                            kotlin.math.abs(distanceX) > 100f
                        ) {
                            // Preserve legacy callback behavior from ServiceTripPreviewItemFragment.
                            if (distanceX > 0f) {
                                onPreviousPage()
                            } else {
                                onNextPage()
                            }
                        }
                    }
                }
            }
    ) {
        item(key = "service_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.spacing_normal))
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.stationName,
                    style = TripKitComposeTextStyles.current.titleLarge,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (state.showCloseButton) {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = context.getString(R.string.desc_close)
                        )
                    }
                }
            }
        }

        item(key = "service_meta") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.spacing_normal))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.modeIconRes?.let { iconRes ->
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(colorResource(id = R.color.labelPrimary))
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    state.serviceColor?.let { serviceColor ->
                        Card(backgroundColor = Color(serviceColor)) {
                            Text(
                                text = state.serviceNumber,
                                color = Color.White,
                                style = TripKitComposeTextStyles.current.bodyMedium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    if (state.statusText.isNotBlank()) {
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = state.statusText,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = state.statusTextColor?.let { Color(it) } ?: MaterialTheme.colors.onSurface
                        )
                    }
                }
                if (state.tertiaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                    Text(
                        text = state.tertiaryText,
                        style = TripKitComposeTextStyles.current.labelLarge,
                        color = colorResource(id = R.color.black1)
                    )
                }
            }
        }

        if (state.showExpandableMenu) {
            item(key = "service_expandable") {
                Card(
                    backgroundColor = colorResource(id = R.color.cardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(id = R.dimen.spacing_normal),
                            vertical = dimensionResource(id = R.dimen.spacing_small)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(id = R.dimen.spacing_small))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (!state.isExpandableMenuExpanded) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.showWheelchairAccessible && state.wheelchairIconRes != null) {
                                        Image(
                                            painter = painterResource(id = state.wheelchairIconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    if (state.showBicycleAccessible) {
                                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_bike),
                                            contentDescription = null,
                                            modifier = Modifier.size(dimensionResource(id = R.dimen.service_detail_icon_size))
                                        )
                                    }
                                    if (state.showOccupancyInfo && state.occupancyIconRes != null) {
                                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                                        Image(
                                            painter = painterResource(id = state.occupancyIconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(dimensionResource(id = R.dimen.service_detail_icon_size))
                                        )
                                    }
                                }
                            } else {
                                Column {
                                    if (state.showWheelchairAccessible && state.wheelchairIconRes != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = state.wheelchairIconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
                                            Text(
                                                text = state.wheelchairAccessibleText,
                                                style = TripKitComposeTextStyles.current.bodyMedium
                                            )
                                        }
                                    }
                                    if (state.showBicycleAccessible) {
                                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_xx_small)))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_bike),
                                                contentDescription = null,
                                                modifier = Modifier.size(dimensionResource(id = R.dimen.service_detail_icon_size))
                                            )
                                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
                                            Text(
                                                text = context.getString(R.string.bicycle_accessible),
                                                style = TripKitComposeTextStyles.current.bodyMedium
                                            )
                                        }
                                    }
                                    if (state.showOccupancyInfo) {
                                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_xx_small)))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            state.occupancyIconRes?.let { iconRes ->
                                                Image(
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(dimensionResource(id = R.dimen.service_detail_icon_size))
                                                )
                                            }
                                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
                                            Text(
                                                text = state.occupancyText,
                                                style = TripKitComposeTextStyles.current.bodyMedium
                                            )
                                        }
                                        if (state.lastUpdatedText.isNotBlank()) {
                                            Text(
                                                text = state.lastUpdatedText,
                                                style = TripKitComposeTextStyles.current.bodyMedium,
                                                color = colorResource(id = R.color.black1),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        if (state.occupancyCars.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.padding(top = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                state.occupancyCars.forEach { car ->
                                                    Image(
                                                        painter = painterResource(id = car.drawableRes),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        colorFilter = ColorFilter.tint(Color(car.tintColor))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = onExpandToggle) {
                                Icon(
                                    painter = painterResource(id = R.drawable.chevron_down),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.isLoading) {
            item(key = "service_loading") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (!state.isLoading && state.errorMessage != null) {
            item(key = "service_error") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.spacing_normal))
                ) {
                    Text(
                        text = state.errorMessage,
                        style = TripKitComposeTextStyles.current.bodyMedium,
                        color = MaterialTheme.colors.error
                    )
                    Text(
                        text = context.getString(R.string.retry),
                        style = TripKitComposeTextStyles.current.labelLarge,
                        modifier = Modifier
                            .padding(top = dimensionResource(id = R.dimen.spacing_small))
                            .clickable(onClick = onRetry)
                    )
                }
            }
        }

        items(items = state.alerts, key = { it.id }) { alert ->
            Card(
                backgroundColor = colorResource(id = R.color.tripKitWarning12),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.spacing_normal))
                    .padding(top = dimensionResource(id = R.dimen.spacing_small))
                    .clickable(enabled = !alert.url.isNullOrBlank()) {
                        onAlertClicked(alert)
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.spacing_small))
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        val alertIconRes = if (alert.severity == RealtimeAlert.SEVERITY_ALERT) {
                            R.drawable.ic_alert_red_overlay
                        } else {
                            R.drawable.ic_alert_yellow_overlay
                        }
                        Image(
                            painter = painterResource(id = alertIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_regular))
                        )
                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                        Text(
                            text = alert.title,
                            style = TripKitComposeTextStyles.current.bodyLarge,
                            color = colorResource(id = R.color.black),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.chevron_right),
                            contentDescription = null
                        )
                    }
                    if (alert.text.isNotBlank()) {
                        Text(
                            text = alert.text,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = colorResource(id = R.color.black),
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.spacing_small))
                        )
                    }
                }
            }
        }

        items(items = state.stops, key = { it.id }) { stop ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.spacing_normal))
                    .background(colorResource(id = R.color.cardBackground))
                    .padding(vertical = 10.dp)
                    .clickable { onStopClicked(stop) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stop.scheduledTime,
                    style = TripKitComposeTextStyles.current.bodyMedium,
                    color = stop.scheduledTimeTextColor?.let { Color(it) }
                        ?: colorResource(id = R.color.black1),
                    modifier = Modifier
                        .size(width = 64.dp, height = 24.dp)
                        .padding(start = 8.dp)
                )
                val lineDrawable = when (stop.lineDirection) {
                    ServiceStopLineUiDirection.START -> R.drawable.service_line_start
                    ServiceStopLineUiDirection.MIDDLE -> R.drawable.service_line_middle
                    ServiceStopLineUiDirection.END -> R.drawable.service_line_end
                }
                ContextCompat.getDrawable(context, lineDrawable)?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(stop.lineColor)),
                        modifier = Modifier
                            .size(width = 16.dp, height = 40.dp)
                            .padding(start = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = stop.stopName,
                        style = TripKitComposeTextStyles.current.bodyMedium,
                        color = stop.stopNameColor?.let { Color(it) }
                            ?: if (stop.isTravelled) colorResource(id = R.color.black2) else colorResource(id = R.color.black)
                    )
                    if (stop.isWheelchairAccessible) {
                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_extra_small)))
                        Image(
                            painter = painterResource(id = R.drawable.ic_wheelchair),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_small))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceTripPreviewItemScreenPreview() {
    TripKitUITheme {
        ServiceTripPreviewItemScreen(
            state = ServicePageUiState(
                stationName = "Central",
                modeIconRes = R.drawable.ic_bus,
                serviceColor = android.graphics.Color.BLUE,
                serviceNumber = "U1",
                statusText = "Scheduled",
                statusTextColor = android.graphics.Color.BLACK,
                tertiaryText = "Towards City",
                showExpandableMenu = true,
                isExpandableMenuExpanded = true,
                showWheelchairAccessible = true,
                wheelchairAccessibleText = "Wheelchair accessible",
                wheelchairIconRes = R.drawable.ic_wheelchair,
                showBicycleAccessible = true,
                showOccupancyInfo = true,
                occupancyIconRes = R.drawable.ic_occupancy_25,
                occupancyText = "Medium",
                lastUpdatedText = "Updated 1m ago",
                occupancyCars = listOf(
                    ServiceOccupancyCarriageUiState(
                        id = "car_0",
                        tintColor = android.graphics.Color.GREEN,
                        drawableRes = R.drawable.ic_train_head
                    )
                ),
                alerts = listOf(
                    ServiceAlertUiState(
                        id = "alert_1",
                        title = "Service Alert",
                        text = "Delay due to maintenance.",
                        severity = RealtimeAlert.SEVERITY_ALERT,
                        url = null
                    )
                ),
                stops = listOf(
                    ServiceStopUiState(
                        id = "stop_1",
                        scheduledTime = "11:03",
                        scheduledTimeTextColor = android.graphics.Color.BLACK,
                        stopName = "Town Hall",
                        stopNameColor = android.graphics.Color.BLACK,
                        lineColor = android.graphics.Color.BLUE,
                        lineDirection = ServiceStopLineUiDirection.START,
                        isTravelled = false,
                        isWheelchairAccessible = true
                    )
                ),
                isLoading = false,
                errorMessage = null,
                showCloseButton = true,
                canGoPrevious = true,
                canGoNext = true
            ),
            onCloseClicked = {},
            onExpandToggle = {},
            onAlertClicked = {},
            onStopClicked = {},
            onRetry = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}
