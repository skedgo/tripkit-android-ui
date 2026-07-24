package com.skedgo.tripkit.ui.tripresult.compose

import android.graphics.Color.TRANSPARENT
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skedgo.tripkit.common.model.realtimealert.ImmutableRealtimeAlert
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresult.RoadTagChart
import com.skedgo.tripkit.ui.tripresult.RoadTagChartAdapter
import com.skedgo.tripkit.ui.tripresult.RoadTagChartItem
import com.skedgo.tripkit.ui.tripresult.TripSegmentItemViewModel
import com.skedgo.tripkit.ui.tripresults.compose.styles.TripResultStyles
import com.skedgo.tripkit.ui.views.TripSegmentAlertView

@BindingAdapter("tripSegmentItemViewModel")
fun bindTripSegmentItemCompose(
    view: ComposeView,
    viewModel: TripSegmentItemViewModel?
) {
    view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    view.setContent {
        TripKitUITheme {
            if (viewModel != null) {
                TripSegmentItemCompose(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TripSegmentItemCompose(viewModel: TripSegmentItemViewModel) {
    val context = LocalContext.current
    val title by viewModel.title.observeAsState("")
    val startTime by viewModel.startTime.observeAsState()
    val showStartTime by viewModel.showStartTime.observeAsState(false)
    val endTime by viewModel.endTime.observeAsState()
    val showEndTime by viewModel.showEndTime.observeAsState(false)
    val hideExactTimes by viewModel.isHideExactTimes.observeAsState(false)
    val description by viewModel.description.observeAsState("")
    val showDescription by viewModel.showDescription.observeAsState(false)
    val notes by viewModel.notes.observeAsState("")
    val showNotes by viewModel.showNotes.observeAsState(false)
    val showTicketInfo by viewModel.showTicketInfo.observeAsState(false)
    val icon by viewModel.icon.observeAsState()
    val showBackgroundCircle by viewModel.showBackgroundCircle.observeAsState(false)
    val backgroundCircleTint by viewModel.backgroundCircleTint.observeAsState(TRANSPARENT)
    val topLineTint by viewModel.topLineTint.observeAsState(TRANSPARENT)
    val bottomLineTint by viewModel.bottomLineTint.observeAsState(TRANSPARENT)
    val showTopLine by viewModel.showTopLine.observeAsState(false)
    val showBottomLine by viewModel.showBottomLine.observeAsState(false)
    val showAlerts by viewModel.showAlerts.observeAsState(false)
    val alerts by viewModel.alerts.observeAsState(arrayListOf())
    val showBicycleAccessible by viewModel.showBicycleAccessible.observeAsState(false)
    val occupancyDrawable by viewModel.occupancyViewModel.drawableLeft.observeAsState()
    val occupancyText by viewModel.occupancyViewModel.occupancyText.observeAsState()
    val hasOccupancyInformation by viewModel.occupancyViewModel.hasOccupancyInformation.observeAsState()
    val hasOccupancySingleInformation by viewModel.occupancyViewModel.hasOccupancySingleInformation.observeAsState()
    val roadTags by viewModel.roadTagChartItems.observeAsState(emptyList())
    val isCancelled by viewModel.isCancelled.observeAsState(false)
    val cancelledMessage by viewModel.cancelledMessage.observeAsState("")

    val hasOccupancy = hasOccupancyInformation || hasOccupancySingleInformation
    val detailRows = buildList {
        if (showBicycleAccessible) {
            add(
                TripSegmentDetailRow(
                    drawable = context.getDrawable(R.drawable.ic_bike_accessible),
                    text = context.getString(R.string.bicycle_accessible)
                )
            )
        }
        if (hasOccupancy) {
            add(TripSegmentDetailRow(drawable = occupancyDrawable, text = occupancyText.orEmpty()))
        }
        if (showDescription && !description.isNullOrBlank()) {
            add(TripSegmentDetailRow(text = description.orEmpty()))
        }
        if (showNotes && !notes.isNullOrBlank()) {
            add(TripSegmentDetailRow(text = notes.orEmpty()))
        }
    }

    TripSegmentItemContent(
        state = TripSegmentItemState(
            title = title.orEmpty(),
            startTime = startTime,
            showStartTime = showStartTime,
            endTime = endTime,
            showEndTime = showEndTime,
            hideExactTimes = hideExactTimes,
            icon = icon,
            showBackgroundCircle = showBackgroundCircle,
            backgroundCircleTint = backgroundCircleTint ?: TRANSPARENT,
            topLineTint = topLineTint ?: TRANSPARENT,
            bottomLineTint = bottomLineTint ?: TRANSPARENT,
            showTopLine = showTopLine,
            showBottomLine = showBottomLine,
            detailRows = detailRows,
            showTicketInfo = showTicketInfo,
            showAlerts = showAlerts,
            alerts = alerts,
            roadTags = roadTags,
            segmentLength = viewModel.tripSegment?.metres,
            isCancelled = isCancelled,
            cancelledMessage = cancelledMessage.orEmpty()
        ),
        callbacks = TripSegmentItemCallbacks(
            onRowClick = { viewModel.onClick.perform() },
            onTicketInfoClick = { viewModel.onTicketInfoClicked.perform() },
            onAlertClick = { viewModel.onAlertClick(it) }
        )
    )
}

@Composable
private fun TripSegmentItemContent(
    state: TripSegmentItemState,
    callbacks: TripSegmentItemCallbacks = TripSegmentItemCallbacks()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.subCardBackground))
            .clickable { callbacks.onRowClick() }
            .padding(horizontal = dimensionResource(R.dimen.spacing_normal))
    ) {
        if (state.isCancelled) {
            CancelledSegment(message = state.cancelledMessage)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            TripSegmentTimeline(
                icon = state.icon,
                showBackgroundCircle = state.showBackgroundCircle,
                backgroundCircleTint = state.backgroundCircleTint,
                showTopLine = state.showTopLine,
                showBottomLine = state.showBottomLine,
                topLineTint = state.topLineTint,
                bottomLineTint = state.bottomLineTint
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = dimensionResource(R.dimen.spacing_small),
                        top = dimensionResource(R.dimen.spacing_normal),
                        end = dimensionResource(R.dimen.spacing_extra_small)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.title,
                        style = TripResultStyles.BodyLarge,
                        color = colorResource(R.color.labelPrimary),
                        modifier = Modifier.weight(1f)
                    )

                    if (state.showStartTime && !state.hideExactTimes && state.startTime != null) {
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))
                        Column(horizontalAlignment = Alignment.End) {
                            SpannableText(
                                text = state.startTime,
                                modifier = Modifier
                                    .width(72.dp)
                            )
                            if (state.showEndTime && state.endTime != null) {
                                SpannableText(
                                    text = state.endTime,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .padding(top = dimensionResource(R.dimen.spacing_extra_small))
                                )
                            }
                        }
                    }
                }

                state.detailRows.forEach { row ->
                    if (row.drawable != null) {
                        IconTextRow(drawable = row.drawable, text = row.text)
                    } else {
                        SupportingText(text = row.text)
                    }
                }

                state.primaryActionText?.let {
                    TripSegmentPrimaryActionButton(text = it, onClick = callbacks.onPrimaryActionClick)
                }

                if (state.showTicketInfo) {
                    TripSegmentTicketInfoButton(
                        onClick = callbacks.onTicketInfoClick
                    )
                }

                if (state.showAlerts) {
                    TripSegmentAlerts(alerts = state.alerts, onClick = callbacks.onAlertClick)
                }

                if (state.roadTags.isNotEmpty()) {
                    TripSegmentRoadTags(
                        items = state.roadTags,
                        segmentLength = state.segmentLength
                    )
                } else {
                    SegmentDivider()
                }
            }
        }
    }
}

private data class TripSegmentItemState(
    val title: String,
    val startTime: SpannableString? = null,
    val showStartTime: Boolean = false,
    val endTime: SpannableString? = null,
    val showEndTime: Boolean = false,
    val hideExactTimes: Boolean = false,
    val icon: Drawable? = null,
    val showBackgroundCircle: Boolean = false,
    val backgroundCircleTint: Int = TRANSPARENT,
    val topLineTint: Int = TRANSPARENT,
    val bottomLineTint: Int = TRANSPARENT,
    val showTopLine: Boolean = false,
    val showBottomLine: Boolean = false,
    val detailRows: List<TripSegmentDetailRow> = emptyList(),
    val primaryActionText: String? = null,
    val showTicketInfo: Boolean = false,
    val showAlerts: Boolean = false,
    val alerts: ArrayList<RealtimeAlert>? = null,
    val roadTags: List<RoadTagChartItem> = emptyList(),
    val segmentLength: Int? = null,
    val isCancelled: Boolean = false,
    val cancelledMessage: String = ""
)

private data class TripSegmentDetailRow(
    val text: String,
    val drawable: Drawable? = null
)

private data class TripSegmentItemCallbacks(
    val onRowClick: () -> Unit = {},
    val onTicketInfoClick: () -> Unit = {},
    val onAlertClick: (android.view.View) -> Unit = {},
    val onPrimaryActionClick: () -> Unit = {}
)

@Composable
private fun TripSegmentTimeline(
    icon: Drawable?,
    showBackgroundCircle: Boolean,
    backgroundCircleTint: Int,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    topLineTint: Int,
    bottomLineTint: Int
) {
    val iconBackgroundSize = dimensionResource(R.dimen.segment_item_icon_background_size)
    val iconTopPadding = dimensionResource(R.dimen.segment_item_spacing_top)
    val lineWidth = dimensionResource(R.dimen.segment_item_line_width)
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .width(dimensionResource(R.dimen.segment_item_spacing_start))
            .fillMaxHeight()
    ) {
        Canvas(
            modifier = Modifier
                .width(iconBackgroundSize)
                .fillMaxHeight()
        ) {
            val iconCenterY = with(density) { (iconTopPadding + iconBackgroundSize / 2).toPx() }
            val strokeWidth = with(density) { lineWidth.toPx() }
            val centerX = size.width / 2

            if (showTopLine && topLineTint != TRANSPARENT) {
                drawLine(
                    color = Color(topLineTint),
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, iconCenterY),
                    strokeWidth = strokeWidth
                )
            }

            if (showBottomLine && bottomLineTint != TRANSPARENT) {
                drawLine(
                    color = Color(bottomLineTint),
                    start = Offset(centerX, iconCenterY),
                    end = Offset(centerX, size.height),
                    strokeWidth = strokeWidth
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = iconTopPadding)
                .size(iconBackgroundSize)
        ) {
            if (showBackgroundCircle) {
                Box(
                    modifier = Modifier
                        .size(iconBackgroundSize)
                        .background(
                            Color(backgroundCircleTint),
                            RoundedCornerShape(iconBackgroundSize)
                        )
                )
            }
            DrawableIcon(icon = icon)
        }
    }
}

@Composable
private fun DrawableIcon(icon: Drawable?) {
    val iconSize = dimensionResource(R.dimen.segment_item_icon_size)
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.size(iconSize),
        factory = {
            ImageView(context).apply {
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        update = {
            it.setImageDrawable(icon)
        }
    )
}

@Composable
private fun SpannableText(
    text: SpannableString?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            TextView(context).apply {
                gravity = Gravity.END
                setTextColor(context.getColor(R.color.labelSecondary))
                textSize = 14f
                includeFontPadding = false
                maxLines = 2
                ellipsize = TextUtilsCompat.end()
            }
        },
        update = {
            it.text = text
        }
    )
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = TripResultStyles.BodyMedium,
        color = colorResource(R.color.labelSecondary),
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(
            top = dimensionResource(R.dimen.spacing_extra_small),
            end = dimensionResource(R.dimen.spacing_normal)
        )
    )
}

@Composable
private fun IconTextRow(drawable: Drawable?, text: String) {
    if (text.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_extra_small))
    ) {
        DrawableIcon(icon = drawable)
        Text(
            text = text,
            style = TripResultStyles.BodySmall,
            color = colorResource(R.color.labelSecondary),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))
        )
    }
}

@Composable
private fun TripSegmentTicketInfoButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius_large)),
        color = colorResource(R.color.inputBackground),
        modifier = Modifier
            .padding(top = dimensionResource(R.dimen.spacing_small))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = LocalContext.current.getString(R.string.label_show_ticket_info),
            style = TripResultStyles.LabelLarge,
            color = colorResource(R.color.labelPrimary),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_12),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
        )
    }
}

@Composable
private fun TripSegmentPrimaryActionButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius_large)),
        color = colorResource(R.color.inputBackground),
        modifier = Modifier
            .padding(top = dimensionResource(R.dimen.spacing_small))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = TripResultStyles.LabelLarge,
            color = colorResource(R.color.labelPrimary),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_12),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
        )
    }
}

@Composable
private fun TripSegmentAlerts(
    alerts: ArrayList<RealtimeAlert>?,
    onClick: (android.view.View) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = -dimensionResource(R.dimen.spacing_small))
            .padding(
                top = dimensionResource(R.dimen.spacing_small),
                end = dimensionResource(R.dimen.spacing_normal)
            ),
        factory = {
            TripSegmentAlertView(context).apply {
                setBackgroundResource(R.drawable.trip_segment_alert_background)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.content_padding),
                    0,
                    resources.getDimensionPixelSize(R.dimen.content_padding),
                    0
                )
                setOnClickListener { onClick(it) }
            }
        },
        update = { it.setAlerts(alerts) }
    )
}

@Composable
private fun TripSegmentRoadTags(
    items: List<RoadTagChartItem>,
    segmentLength: Int?
) {
    val max = remember(items, segmentLength) {
        (segmentLength ?: items.maxOfOrNull { it.length } ?: 0).roundToNearestHundred()
    }
    val middle = max / 2
    val chartItems = remember(items, max) {
        items.map { item ->
            item.apply { maxProgress = max }
        }.sortedBy { it.index }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = dimensionResource(R.dimen.segment_item_spacing_start)),
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                itemAnimator = null
                adapter = RoadTagChartAdapter()
            }
        },
        update = { recyclerView ->
            (recyclerView.adapter as? RoadTagChartAdapter)?.collection = listOf(
                RoadTagChart(max = max, middle = middle, items = chartItems)
            )
        }
    )
}

@Composable
private fun SegmentDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacing_12))
            .offset(x = -dimensionResource(R.dimen.spacing_small))
            .height(dimensionResource(R.dimen.divider_size))
            .background(colorResource(R.color.black4))
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentLocationPreview() {
    TripKitUITheme {
        val neutralLineColor = colorResource(R.color.black4).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Leave Location A",
                iconRes = R.drawable.ic_location_on,
                startTime = "11:58 PM",
                showBottomLine = true,
                bottomLineTint = neutralLineColor
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentWalkingActionPreview() {
    TripKitUITheme {
        val neutralLineColor = colorResource(R.color.black4).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Walk about 500m",
                iconRes = R.drawable.ic_directions,
                detailRows = listOf(TripSegmentDetailRow(text = "4 mins")),
                primaryActionText = "Open in...",
                showTopLine = true,
                topLineTint = neutralLineColor,
                showBottomLine = true,
                bottomLineTint = neutralLineColor
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentTransitPreview() {
    TripKitUITheme {
        val transitColor = colorResource(R.color.favorite_bus).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Take 379",
                iconRes = R.drawable.ic_search_bus,
                startTime = "12:02 AM",
                endTime = "12:20 AM",
                detailRows = listOf(
                    TripSegmentDetailRow(text = "PrePay-Only Banksmeadow to Central"),
                    TripSegmentDetailRow(text = "Direction: Railway sq"),
                    TripSegmentDetailRow(text = "11 stops")
                ),
                showTicketInfo = true,
                showTopLine = true,
                topLineTint = transitColor,
                showBottomLine = true,
                bottomLineTint = transitColor,
                backgroundCircleTint = transitColor
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentConnectedGroupPreview() {
    TripKitUITheme {
        val transitColor = colorResource(R.color.favorite_bus).toArgb()
        val neutralLineColor = colorResource(R.color.black4).toArgb()
        Column {
            TripSegmentItemContent(
                state = previewTripSegmentState(
                    title = "Walk to Kent St",
                    iconRes = R.drawable.ic_directions,
                    detailRows = listOf(TripSegmentDetailRow(text = "2 mins")),
                    showBottomLine = true,
                    bottomLineTint = neutralLineColor
                )
            )
            TripSegmentItemContent(
                state = previewTripSegmentState(
                    title = "Take 343",
                    iconRes = R.drawable.ic_search_bus,
                    startTime = "12:08 AM",
                    detailRows = listOf(TripSegmentDetailRow(text = "Kingsford to Chatswood")),
                    showTopLine = true,
                    topLineTint = transitColor,
                    showBottomLine = true,
                    bottomLineTint = transitColor,
                    backgroundCircleTint = transitColor
                )
            )
            TripSegmentItemContent(
                state = previewTripSegmentState(
                    title = "Arrive at Location B",
                    iconRes = R.drawable.ic_location_on,
                    endTime = "12:31 AM",
                    showStartTime = false,
                    showTopLine = true,
                    topLineTint = neutralLineColor
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentLongTextPreview() {
    TripKitUITheme {
        val transitColor = colorResource(R.color.favorite_bus).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Take the very long named bus route toward the central railway square interchange",
                iconRes = R.drawable.ic_search_bus,
                startTime = "11:58 PM",
                endTime = "12:44 AM",
                detailRows = listOf(
                    TripSegmentDetailRow(text = "This service description is intentionally long to check wrapping beside the time column."),
                    TripSegmentDetailRow(text = "Direction: Railway sq via multiple intermediate stops")
                ),
                showTopLine = true,
                topLineTint = transitColor,
                showBottomLine = true,
                bottomLineTint = transitColor,
                backgroundCircleTint = transitColor
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentCancelledPreview() {
    TripKitUITheme {
        val neutralLineColor = colorResource(R.color.black4).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Take T4",
                iconRes = R.drawable.ic_search_train,
                startTime = "12:14 AM",
                detailRows = listOf(TripSegmentDetailRow(text = "Eastern Suburbs and Illawarra Line")),
                showTopLine = true,
                topLineTint = neutralLineColor,
                showBottomLine = true,
                bottomLineTint = neutralLineColor,
                isCancelled = true,
                cancelledMessage = "This segment has been cancelled."
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentAlertsPreview() {
    TripKitUITheme {
        val transitColor = colorResource(R.color.favorite_bus).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Take 379",
                iconRes = R.drawable.ic_search_bus,
                startTime = "12:02 AM",
                endTime = "12:20 AM",
                detailRows = listOf(TripSegmentDetailRow(text = "Direction: Railway sq")),
                showTopLine = true,
                topLineTint = transitColor,
                showBottomLine = true,
                bottomLineTint = transitColor,
                backgroundCircleTint = transitColor,
                showAlerts = true,
                alerts = arrayListOf(
                    previewRealtimeAlert(
                        title = "Delays expected",
                        text = "Buses on this route are running with delays of up to 10 minutes.",
                        severity = RealtimeAlert.SEVERITY_WARNING
                    ),
                    previewRealtimeAlert(
                        title = "Stop closed",
                        text = "The Railway Square stop is temporarily closed. Please use the nearest alternative stop.",
                        severity = RealtimeAlert.SEVERITY_ALERT
                    )
                )
            )
        )
    }
}

private fun previewRealtimeAlert(
    title: String,
    text: String,
    severity: String
): RealtimeAlert = ImmutableRealtimeAlert.builder()
    .title(title)
    .text(text)
    .severity(severity)
    .remoteHashCode(title.hashCode().toLong())
    .build()

@Composable
private fun previewTripSegmentState(
    title: String,
    iconRes: Int,
    startTime: String? = null,
    endTime: String? = null,
    detailRows: List<TripSegmentDetailRow> = emptyList(),
    primaryActionText: String? = null,
    showTicketInfo: Boolean = false,
    showTopLine: Boolean = false,
    topLineTint: Int = TRANSPARENT,
    showBottomLine: Boolean = false,
    bottomLineTint: Int = TRANSPARENT,
    backgroundCircleTint: Int = TRANSPARENT,
    showStartTime: Boolean = startTime != null,
    showAlerts: Boolean = false,
    alerts: ArrayList<RealtimeAlert>? = null,
    isCancelled: Boolean = false,
    cancelledMessage: String = ""
): TripSegmentItemState {
    val context = LocalContext.current
    val circleTint = if (backgroundCircleTint == TRANSPARENT) {
        colorResource(R.color.black4).toArgb()
    } else {
        backgroundCircleTint
    }
    return TripSegmentItemState(
        title = title,
        startTime = startTime?.let(::SpannableString),
        showStartTime = showStartTime,
        endTime = endTime?.let(::SpannableString),
        showEndTime = endTime != null,
        icon = context.getDrawable(iconRes),
        showBackgroundCircle = true,
        backgroundCircleTint = circleTint,
        topLineTint = topLineTint,
        bottomLineTint = bottomLineTint,
        showTopLine = showTopLine,
        showBottomLine = showBottomLine,
        detailRows = detailRows,
        primaryActionText = primaryActionText,
        showTicketInfo = showTicketInfo,
        showAlerts = showAlerts,
        alerts = alerts,
        isCancelled = isCancelled,
        cancelledMessage = cancelledMessage
    )
}

@Composable
private fun CancelledSegment(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.spacing_normal),
                end = dimensionResource(R.dimen.spacing_normal)
            )
            .background(colorResource(R.color.tripKitWarning))
            .padding(dimensionResource(R.dimen.spacing_normal))
    ) {
        Text(
            text = message,
            style = TripResultStyles.BodyLarge,
            color = Color.Black
        )
        Text(
            text = LocalContext.current.getString(R.string.alternatives),
            style = TripResultStyles.BodyMedium,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = dimensionResource(R.dimen.spacing_small))
        )
    }
}

private fun Int.roundToNearestHundred(): Int {
    val remainder = this % 100
    return if (remainder < 50) {
        this - remainder
    } else {
        this + (100 - remainder)
    }
}

private object TextUtilsCompat {
    fun end() = android.text.TextUtils.TruncateAt.END
}

@Composable
private fun <T> ObservableField<T>.observeAsState(): State<T?> {
    val state = remember(this) { mutableStateOf(get()) }
    DisposableEffect(this) {
        val callback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                state.value = get()
            }
        }
        addOnPropertyChangedCallback(callback)
        onDispose { removeOnPropertyChangedCallback(callback) }
    }
    return state
}

@Composable
private fun ObservableBoolean.observeAsState(): State<Boolean> {
    val state = remember(this) { mutableStateOf(get()) }
    DisposableEffect(this) {
        val callback = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                state.value = get()
            }
        }
        addOnPropertyChangedCallback(callback)
        onDispose { removeOnPropertyChangedCallback(callback) }
    }
    return state
}
