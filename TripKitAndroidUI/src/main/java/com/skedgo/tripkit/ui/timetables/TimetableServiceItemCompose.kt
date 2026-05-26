package com.skedgo.tripkit.ui.timetables

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.core.binding.ImageViewBindingAdapters
import com.skedgo.tripkit.ui.timetables.styles.TimetableTextStyles

@BindingAdapter("timetableServiceViewModel")
fun bindTimetableServiceCompose(
    view: ComposeView,
    viewModel: ServiceViewModel?
) {
    // RecyclerView items can be bound before attachment; this strategy is safe in both cases.
    view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    view.setContent {
        if (viewModel != null) {
            TimetableServiceItemCompose(viewModel = viewModel)
        }
    }
}

@Composable
fun TimetableServiceItemCompose(
    viewModel: ServiceViewModel,
    itemShape: RoundedCornerShape = RoundedCornerShape(
        dimensionResource(R.dimen.cardview_corner_radius_small)
    ),
) {
    val title by viewModel.tertiaryText.observeAsState("")
    val statusText by viewModel.secondaryText.observeAsState("")
    val routeNumber by viewModel.serviceNumber.observeAsState("")
    val footer by viewModel.quaternaryText.observeAsState("")
    val countdownText by viewModel.countDownTimeText.observeAsState("")
    val countdownColorInt by viewModel.countDownTimeTextColor.observeAsState(
        colorResource(R.color.tripKitSuccess).toArgb()
    )
    val statusColorInt by viewModel.secondaryTextColor.observeAsState(
        colorResource(R.color.labelSecondary).toArgb()
    )
    val modeInfo by viewModel.modeInfo.observeAsState()
    val serviceColorInt by viewModel.serviceColor.observeAsState(
        colorResource(R.color.classification_cheapest).toArgb()
    )
    val alphaValue by viewModel.alpha.observeAsState(1f)
    val isCurrentTrip by viewModel.isCurrentTrip.observeAsState(false)
    val wheelchairIcon by viewModel.wheelchairIcon.observeAsState()
    val wheelchairTint by viewModel.wheelchairTint.observeAsState(Color.Unspecified.value.toInt())
    val wheelchairBackground by viewModel.wheelchairBackgroundTint.observeAsState()
    val showBicycleAccessible by viewModel.showBicycleAccessible.observeAsState(false)
    val showOccupancyInfo by viewModel.showOccupancyInfo.observeAsState(false)
    val isOnTime by viewModel.isOnTime.observeAsState(false)

    val countdownParts = parseCountdown(countdownText.orEmpty())
    val hasAlerts = viewModel.service.alerts.orEmpty().isNotEmpty()
    val occupancyIcon = viewModel.occupancyViewModel.drawableLeft.get()

    TimetableServiceItemCard(
        title = title.orEmpty(),
        statusText = statusText.orEmpty(),
        footerText = footer.orEmpty(),
        routeNumber = routeNumber.orEmpty(),
        modeInfo = modeInfo,
        hasAlerts = hasAlerts,
        showBicycleAccessible = showBicycleAccessible,
        showOccupancyInfo = showOccupancyInfo,
        occupancyIcon = occupancyIcon,
        wheelchairIcon = wheelchairIcon,
        wheelchairTint = wheelchairTint,
        wheelchairBackground = wheelchairBackground,
        countdownValue = countdownParts.first,
        countdownUnit = countdownParts.second,
        countdownColor = Color(countdownColorInt),
        statusColor = Color(statusColorInt),
        routeColor = Color(serviceColorInt),
        isCurrentTrip = isCurrentTrip,
        alphaValue = alphaValue,
        isOnTime = isOnTime,
        itemShape = itemShape,
        onClick = { viewModel.onItemClick.perform() }
    )
}

@Composable
private fun TimetableServiceItemCard(
    title: String,
    statusText: String,
    footerText: String,
    routeNumber: String,
    modeInfo: ModeInfo?,
    hasAlerts: Boolean,
    showBicycleAccessible: Boolean,
    showOccupancyInfo: Boolean,
    occupancyIcon: Drawable?,
    wheelchairIcon: Drawable?,
    wheelchairTint: Int,
    wheelchairBackground: Drawable?,
    countdownValue: String,
    countdownUnit: String?,
    countdownColor: Color,
    statusColor: Color,
    routeColor: Color,
    isCurrentTrip: Boolean,
    alphaValue: Float,
    isOnTime: Boolean,
    itemShape: RoundedCornerShape = RoundedCornerShape(
        dimensionResource(R.dimen.cardview_corner_radius_small)
    ),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_normal),
                vertical = dimensionResource(R.dimen.spacing_xx_small)
            )
    ) {
        if (isCurrentTrip) {
            Box(
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen.spacing_extra_small))
                    .width(dimensionResource(R.dimen.spacing_3))
                    .fillMaxHeight()
                    .background(colorResource(R.color.colorAccent), RoundedCornerShape(percent = 50))
            )
        }
        Surface(
            shape = itemShape,
            color = colorResource(R.color.subCardBackground),

            modifier = Modifier
                .fillMaxWidth()
                .alpha(alphaValue)
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_normal),
                        vertical = dimensionResource(R.dimen.spacing_12)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = dimensionResource(R.dimen.spacing_normal)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                ) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TimetableTextStyles.ServiceItemTitle,
                        color = colorResource(R.color.labelPrimary)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        ModeIcon(modeInfo = modeInfo)
                        RouteBadge(routeNumber = routeNumber, routeColor = routeColor)
                        if (hasAlerts) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_warning),
                                contentDescription = null,
                                tint = colorResource(R.color.tripKitWarning),
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_20))
                            )
                        }
                        if (wheelchairIcon != null) {
                            AccessibilityIcon(
                                icon = wheelchairIcon,
                                tint = wheelchairTint,
                                background = wheelchairBackground
                            )
                        }
                        if (showBicycleAccessible) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bike_accessible),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_20))
                            )
                        }
                        if (showOccupancyInfo && occupancyIcon != null) {
                            DrawableIcon(icon = occupancyIcon)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOnTime) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_circle),
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                            )
                            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_extra_small)))
                        }
                        Text(
                            text = statusText,
                            style = TimetableTextStyles.ServiceItemStatusText,
                            color = statusColor
                        )
                    }

                    if (footerText.isNotEmpty()) {
                        Text(
                            text = footerText,
                            style = TripKitComposeTextStyles.current.bodySmall,
                            color = colorResource(R.color.labelSecondary)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = countdownValue,
                        style = TimetableTextStyles.ServiceItemTimeValue,
                        color = countdownColor
                    )
                    countdownUnit?.let { unit ->
                        Text(
                            text = unit,
                            style = TimetableTextStyles.ServiceItemTimeUnit,
                            color = countdownColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeIcon(modeInfo: ModeInfo?) {
    val isInPreview = LocalInspectionMode.current
    if (isInPreview) {
        Icon(
            painter = painterResource(R.drawable.ic_public_transport),
            contentDescription = null,
            tint = colorResource(R.color.black2),
            modifier = Modifier.size(dimensionResource(R.dimen.icon_regular))
        )
        return
    }

    AndroidView(
        factory = { context -> ImageView(context) },
        update = { imageView ->
            ImageViewBindingAdapters.setCompatModeInfo(imageView, modeInfo)
            ImageViewCompat.setImageTintList(
                imageView,
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(imageView.context, R.color.black2)
                )
            )
        },
        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
    )
}

@Composable
private fun RouteBadge(routeNumber: String, routeColor: Color) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.button_radius_small)),
        color = routeColor
    ) {
        Text(
            text = routeNumber,
            style = TripKitComposeTextStyles.current.labelLarge,
            color = colorResource(R.color.white),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_small),
                vertical = dimensionResource(R.dimen.spacing_xx_small)
            )
        )
    }
}

@Composable
private fun AccessibilityIcon(icon: Drawable, tint: Int, background: Drawable?) {
    AndroidView(
        factory = { context ->
            ImageView(context)
        },
        update = { imageView ->
            imageView.background = background
            imageView.setImageDrawable(icon)
            ImageViewCompat.setImageTintList(
                imageView,
                android.content.res.ColorStateList.valueOf(tint)
            )
        },
        modifier = Modifier.size(dimensionResource(R.dimen.icon_20))
    )
}

@Composable
private fun DrawableIcon(icon: Drawable) {
    AndroidView(
        factory = { context -> ImageView(context) },
        update = { imageView ->
            imageView.setImageDrawable(icon)
        },
        modifier = Modifier.size(dimensionResource(R.dimen.icon_20))
    )
}

private fun parseCountdown(text: String): Pair<String, String?> {
    val trimmed = text.trim()
    val regex = Regex("^(-?\\d+)\\s*([A-Za-z]+)$")
    val match = regex.matchEntire(trimmed)
    if (match != null) {
        return match.groupValues[1] to match.groupValues[2]
    }
    val tokens = trimmed.split(" ").filter { it.isNotBlank() }
    if (tokens.size >= 2 && tokens.first().all { it.isDigit() }) {
        return tokens.first() to tokens.drop(1).joinToString(" ")
    }
    return trimmed to null
}

@Preview(showBackground = true)
@Composable
private fun TimetableServiceItemPreview() {
    TripKitUITheme {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TimetableServiceItemCard(
                title = "The Domain",
                statusText = "On time · 18:26",
                footerText = "",
                routeNumber = "T9",
                modeInfo = null,
                hasAlerts = false,
                showBicycleAccessible = false,
                showOccupancyInfo = false,
                occupancyIcon = null,
                wheelchairIcon = null,
                wheelchairTint = 0,
                wheelchairBackground = null,
                countdownValue = "55",
                countdownUnit = "mins",
                countdownColor = colorResource(R.color.tripKitSuccess),
                statusColor = colorResource(R.color.labelSecondary),
                routeColor = colorResource(R.color.classification_cheapest),
                isCurrentTrip = false,
                isOnTime = false,
                alphaValue = 1f,
                onClick = {}
            )
            TimetableServiceItemCard(
                title = "The Domain",
                statusText = "On time · 18:26",
                footerText = "",
                routeNumber = "T9",
                modeInfo = null,
                hasAlerts = false,
                showBicycleAccessible = false,
                showOccupancyInfo = false,
                occupancyIcon = null,
                wheelchairIcon = null,
                wheelchairTint = 0,
                wheelchairBackground = null,
                countdownValue = "55",
                countdownUnit = "mins",
                countdownColor = colorResource(R.color.tripKitSuccess),
                statusColor = colorResource(R.color.labelSecondary),
                routeColor = colorResource(R.color.classification_cheapest),
                isCurrentTrip = false,
                alphaValue = 1f,
                isOnTime = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableServiceItemLongTitlePreview() {
    TripKitUITheme {
        TimetableServiceItemCard(
            title = "Very Long Destination Name That Should Truncate Before Countdown Area",
            statusText = "Scheduled · 18:24",
            footerText = "Operated by Transit",
            routeNumber = "T2",
            modeInfo = null,
            hasAlerts = true,
            showBicycleAccessible = true,
            showOccupancyInfo = false,
            occupancyIcon = null,
            wheelchairIcon = null,
            wheelchairTint = 0,
            wheelchairBackground = null,
            countdownValue = "5",
            countdownUnit = "mins",
            countdownColor = colorResource(R.color.tripKitSuccess),
            statusColor = colorResource(R.color.labelSecondary),
            routeColor = colorResource(R.color.classification_cheapest),
            isCurrentTrip = true,
            alphaValue = 1f,
            isOnTime = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableServiceItemDelayedPreview() {
    TripKitUITheme {
        TimetableServiceItemCard(
            title = "Central",
            statusText = "Delayed · 18:31",
            footerText = "",
            routeNumber = "X1",
            modeInfo = null,
            hasAlerts = true,
            showBicycleAccessible = false,
            showOccupancyInfo = false,
            occupancyIcon = null,
            wheelchairIcon = null,
            wheelchairTint = 0,
            wheelchairBackground = null,
            countdownValue = "Delayed",
            countdownUnit = null,
            countdownColor = colorResource(R.color.tripKitWarning),
            statusColor = colorResource(R.color.tripKitWarning),
            routeColor = colorResource(R.color.classification_cheapest),
            isCurrentTrip = false,
            alphaValue = 1f,
            isOnTime = false,
            onClick = {}
        )
    }
}
