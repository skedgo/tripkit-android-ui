package com.skedgo.tripkit.ui.tripresult.compose

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.common.model.realtimealert.ImmutableRealtimeAlert
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresult.RoadTagChart
import com.skedgo.tripkit.ui.tripresult.RoadTagChartItem
import com.skedgo.tripkit.ui.tripresult.TripSegmentItemViewModel
import com.skedgo.tripkit.ui.tripresults.compose.styles.TripResultStyles
import com.skedgo.tripkit.ui.views.TripSegmentAlertView
import kotlin.math.roundToInt

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

/**
 * Cycle infrastructure breakdown shown underneath a bicycle segment.
 *
 * Mirrors the production `layout_road_tags` / `item_fake_graph` / `item_road_tag_chart`
 * stack: a separator, an axis with a "middle" and "max" distance label plus their
 * grid lines, one label-and-bar row per road tag, and a closing separator.
 */
@Composable
private fun TripSegmentRoadTags(
    items: List<RoadTagChartItem>,
    segmentLength: Int?
) {
    val chart = remember(items, segmentLength) { buildRoadTagChart(items, segmentLength) }
    if (chart.items.isEmpty()) {
        SegmentDivider()
        return
    }

    // Production draws `line` above the include and `divider` below it.
    SegmentDivider()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Production anchors the chart to `leftGuide` (8dp left of the content column)
            // while leaving its end at the container edge, so the block grows to the start
            // rather than sliding across.
            .extendToStart(dimensionResource(R.dimen.spacing_small))
            // item_fake_graph root padding.
            .padding(dimensionResource(R.dimen.spacing_small))
    ) {
        RoadTagChartGraph(chart = chart)
    }
    SegmentDivider()
}

/**
 * Reproduces the production chart calculation from
 * `TripSegmentCustomRecyclerViewAdapter` and `RoadTagChartAdapter`: the segment length
 * is used verbatim as the axis maximum and only the fallback is rounded, items are
 * ordered by road-safety index, and tags sharing a label are merged by summing lengths.
 */
internal fun buildRoadTagChart(
    items: List<RoadTagChartItem>,
    segmentLength: Int?
): RoadTagChart {
    val max = segmentLength ?: (items.maxOfOrNull { it.length } ?: 0).roundToNearestHundred()
    return RoadTagChart(
        max = max,
        middle = max / 2,
        items = items.sortedBy { it.index }
            .groupBy { it.label }
            .map { (_, grouped) ->
                grouped.first().copy(
                    length = grouped.sumOf { it.length },
                    maxProgress = max
                )
            }
    )
}

/**
 * Grows a full-width child by [extra] towards the layout start without giving up any width at
 * the end, mirroring `layoutRoadTags` being constrained `start_toStartOf @id/line` and
 * `end_toEndOf parent`. Plain `offset` would slide the block instead, costing it [extra] of
 * width on the end side.
 */
private fun Modifier.extendToStart(extra: Dp) = layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extraPx,
            maxWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth + extraPx
            } else {
                constraints.maxWidth
            }
        )
    )
    layout(placeable.width - extraPx, placeable.height) {
        placeable.place(-extraPx, 0)
    }
}

private const val ROAD_TAG_AXIS_START = "axisStart"
private const val ROAD_TAG_AXIS_MIDDLE = "axisMiddle"
private const val ROAD_TAG_AXIS_MAX = "axisMax"
private const val ROAD_TAG_AXIS_DIVIDER = "axisDivider"
private const val ROAD_TAG_GRID_MIDDLE = "gridMiddle"
private const val ROAD_TAG_GRID_MAX = "gridMax"
private const val ROAD_TAG_ROWS = "rows"

/** `item_fake_graph` positions the middle axis label at this bias between "0" and the max label. */
private const val ROAD_TAG_MIDDLE_BIAS = 0.7f
private const val ROAD_TAG_LABEL_WEIGHT = 1f
private const val ROAD_TAG_BAR_WEIGHT = 2f

@Composable
private fun RoadTagChartGraph(chart: RoadTagChart) {
    val gridColor = colorResource(R.color.black4)
    val labelColor = colorResource(R.color.labelPrimary)
    // item_road_tag_chart / item_fake_graph use plain TextViews, so the tracking that
    // BodyMedium adds is dropped here; it is enough to wrap "Cycle Network" onto a
    // second line at the widths this chart runs at.
    val labelStyle = TripResultStyles.BodyMedium.copy(letterSpacing = 0.sp)

    val inset = dimensionResource(R.dimen.spacing_small)
    val dividerSize = dimensionResource(R.dimen.divider_size)
    val gridTopSpacing = dimensionResource(R.dimen.spacing_extra_small)
    val rowsTopSpacing = dimensionResource(R.dimen.spacing_small)
    val rowsBottomSpacing = dimensionResource(R.dimen.spacing_medium)

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            // Mirrors item_fake_graph's invisible "0" label, which anchors the middle bias.
            Text(
                text = "0",
                style = labelStyle,
                color = Color.Transparent,
                maxLines = 1,
                modifier = Modifier.layoutId(ROAD_TAG_AXIS_START)
            )
            Text(
                text = chart.getMiddleDistance(),
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                modifier = Modifier.layoutId(ROAD_TAG_AXIS_MIDDLE)
            )
            Text(
                text = chart.getMaxDistance(),
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                modifier = Modifier.layoutId(ROAD_TAG_AXIS_MAX)
            )
            Box(
                modifier = Modifier
                    .layoutId(ROAD_TAG_AXIS_DIVIDER)
                    .background(gridColor)
            )
            Box(
                modifier = Modifier
                    .layoutId(ROAD_TAG_GRID_MIDDLE)
                    .background(gridColor)
            )
            Box(
                modifier = Modifier
                    .layoutId(ROAD_TAG_GRID_MAX)
                    .background(gridColor)
            )
            Column(modifier = Modifier.layoutId(ROAD_TAG_ROWS)) {
                chart.items.forEach { item ->
                    RoadTagChartRow(
                        item = item,
                        labelStyle = labelStyle,
                        labelColor = labelColor
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val insetPx = inset.roundToPx()
        val dividerPx = dividerSize.roundToPx()

        fun measurableFor(id: String) = measurables.first { it.layoutId == id }

        val labelConstraints = Constraints(maxWidth = width)
        val startLabel = measurableFor(ROAD_TAG_AXIS_START).measure(labelConstraints)
        val middleLabel = measurableFor(ROAD_TAG_AXIS_MIDDLE).measure(labelConstraints)
        val maxLabel = measurableFor(ROAD_TAG_AXIS_MAX).measure(labelConstraints)

        // vTopDivider: match_parent with an 8dp start margin, sitting 8dp under the labels.
        val dividerY = maxOf(startLabel.height, middleLabel.height, maxLabel.height) + insetPx
        val dividerWidth = (width - insetPx).coerceAtLeast(0)
        val axisDivider = measurableFor(ROAD_TAG_AXIS_DIVIDER)
            .measure(Constraints.fixed(dividerWidth, dividerPx))

        // tvMax is end-aligned to vTopDivider; tvMiddle sits at ROAD_TAG_MIDDLE_BIAS between them.
        val maxLabelX = (width - maxLabel.width).coerceAtLeast(0)
        val middleSpan = (maxLabelX - startLabel.width - middleLabel.width).coerceAtLeast(0)
        val middleLabelX = startLabel.width + (middleSpan * ROAD_TAG_MIDDLE_BIAS).roundToInt()

        val rowsWidth = (width - insetPx * 2).coerceAtLeast(0)
        val rowsY = dividerY + dividerPx + rowsTopSpacing.roundToPx()
        val rows = measurableFor(ROAD_TAG_ROWS)
            .measure(Constraints(minWidth = rowsWidth, maxWidth = rowsWidth))

        val height = rowsY + rows.height + rowsBottomSpacing.roundToPx()
        val gridY = dividerY + dividerPx + gridTopSpacing.roundToPx()
        val gridConstraints = Constraints.fixed(dividerPx, (height - gridY).coerceAtLeast(0))
        val gridMiddle = measurableFor(ROAD_TAG_GRID_MIDDLE).measure(gridConstraints)
        val gridMax = measurableFor(ROAD_TAG_GRID_MAX).measure(gridConstraints)

        layout(width, height) {
            startLabel.place(0, 0)
            middleLabel.place(middleLabelX, 0)
            maxLabel.place(maxLabelX, 0)
            axisDivider.place(insetPx, dividerY)
            gridMiddle.place(middleLabelX + (middleLabel.width - dividerPx) / 2, gridY)
            gridMax.place(maxLabelX + (maxLabel.width - dividerPx) / 2, gridY)
            rows.place(insetPx, rowsY)
        }
    }
}

@Composable
private fun RoadTagChartRow(
    item: RoadTagChartItem,
    labelStyle: TextStyle,
    labelColor: Color
) {
    val barHeight = dimensionResource(R.dimen.segment_progress_height)
    val fraction = if (item.maxProgress > 0) {
        (item.length.toFloat() / item.maxProgress).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = item.label,
            style = labelStyle,
            color = labelColor,
            modifier = Modifier.weight(ROAD_TAG_LABEL_WEIGHT)
        )
        Box(
            modifier = Modifier
                .weight(ROAD_TAG_BAR_WEIGHT)
                .height(barHeight)
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(barHeight)
                        .background(Color(item.color), CircleShape)
                )
            }
        }
    }
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

@Preview(name = "Cycle - light", showBackground = true, backgroundColor = 0xFFF5F5F6)
@Preview(
    name = "Cycle - dark",
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TripSegmentCyclePreview() {
    TripKitUITheme {
        val cycleColor = colorResource(R.color.classification_greenest).toArgb()
        TripSegmentItemContent(
            state = previewTripSegmentState(
                title = "Ride Bicycle",
                iconRes = R.drawable.ic_bike_accessible,
                detailRows = listOf(TripSegmentDetailRow(text = "10mins \u00b7 3.1 km")),
                showTopLine = true,
                topLineTint = cycleColor,
                showBottomLine = true,
                bottomLineTint = cycleColor,
                backgroundCircleTint = cycleColor,
                segmentLength = 3147,
                roadTags = previewRoadTags()
            )
        )
    }
}

private fun previewRoadTags(): List<RoadTagChartItem> = listOf(
    RoadTagChartItem(label = "Cycle Lane", length = 820, color = AndroidColor.parseColor("#008000"), index = 0),
    RoadTagChartItem(label = "Cycle Track", length = 210, color = AndroidColor.parseColor("#0000b3"), index = 1),
    RoadTagChartItem(label = "Cycle Network", length = 2747, color = AndroidColor.parseColor("#0000b3"), index = 1),
    RoadTagChartItem(label = "Designated for Cyclists", length = 190, color = AndroidColor.parseColor("#0000b3"), index = 1),
    RoadTagChartItem(label = "Side Road", length = 1010, color = AndroidColor.parseColor("#8080ff"), index = 2),
    RoadTagChartItem(label = "Main Road", length = 120, color = AndroidColor.parseColor("#ffa500"), index = 3),
    RoadTagChartItem(label = "Other", length = 12, color = AndroidColor.DKGRAY, index = 4)
)

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
    cancelledMessage: String = "",
    roadTags: List<RoadTagChartItem> = emptyList(),
    segmentLength: Int? = null
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
        roadTags = roadTags,
        segmentLength = segmentLength,
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
