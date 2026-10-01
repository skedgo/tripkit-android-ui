package com.skedgo.tripkit.ui.tripresults.compose

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.TextViewCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.TripResultTripViewModel
import com.skedgo.tripkit.ui.tripresults.TripSegmentViewModel
import com.skedgo.tripkit.ui.tripresults.compose.styles.TripResultStyles
import com.skedgo.tripkit.ui.utils.resolveComposeColor

@Composable
fun TripResultLegRowCompose(
    viewModel: TripResultTripViewModel
) {
    TripResultLegRowContent(
        title = viewModel.title.value.orEmpty(),
        subtitle = viewModel.subtitle.value.orEmpty(),
        hideExactTimes = viewModel.isHideExactTimes.value == true,
        alpha = if (viewModel.isMissedPreBooking.value == true) 0.3f else 1f,
        hasQuickBooking = viewModel.hasQuickBooking.value == true,
        quickBookingTitle = viewModel.trip?.quickBookingSegment?.booking?.title.orEmpty(),
        onRowClick = { viewModel.onItemClicked() },
        onQuickBookingClick = { viewModel.onQuickBookingActionClicked() }
    ) {
        viewModel.segments.forEach { segment ->
            SegmentSummaryCompose(ui = rememberSegmentSummaryUi(segment))
        }
    }
}

@Composable
fun TripResultLegRowCompose(
    title: String,
    subtitle: String,
    hideExactTimes: Boolean,
    hasQuickBooking: Boolean,
    quickBookingTitle: String,
    segments: List<TripResultLegPreviewSegmentUi>,
    onRowClick: () -> Unit = {},
    onQuickBookingClick: () -> Unit = {}
) {
    TripResultLegRowContent(
        title = title,
        subtitle = subtitle,
        hideExactTimes = hideExactTimes,
        alpha = 1f,
        hasQuickBooking = hasQuickBooking,
        quickBookingTitle = quickBookingTitle,
        onRowClick = onRowClick,
        onQuickBookingClick = onQuickBookingClick
    ) {
        segments.forEach { segment ->
            SegmentSummaryCompose(
                ui = SegmentSummaryUi(
                    showPrimary = true,
                    primaryText = segment.primaryText,
                    secondaryText = segment.secondaryText,
                    isHideExactTimes = false,
                    isRealtime = segment.showWifi,
                    isBicycleAccessible = segment.showBike,
                    isCancelled = false,
                    iconRes = segment.iconRes
                )
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TripResultLegRowContent(
    title: String,
    subtitle: String,
    hideExactTimes: Boolean,
    alpha: Float,
    hasQuickBooking: Boolean,
    quickBookingTitle: String,
    onRowClick: () -> Unit,
    onQuickBookingClick: () -> Unit,
    segmentsContent: @Composable () -> Unit
) {
    val accentColor = resolveComposeColor(
        default = ContextCompat.getColor(
            LocalContext.current,
            R.color.colorAccent
        )
    )

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius_medium)),
        backgroundColor = Color.White,
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onRowClick)
                .alpha(alpha)
                .padding(dimensionResource(R.dimen.spacing_12)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_12))
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    style = TripResultStyles.BodyLarge,
                    color = colorResource(R.color.labelPrimary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!hideExactTimes) {
                    Text(
                        text = subtitle,
                        style = TripResultStyles.BodyLarge,
                        color = colorResource(R.color.labelSecondary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                ) {
                    segmentsContent()
                }
                if (hasQuickBooking) {
                    Button(
                        onClick = onQuickBookingClick,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = accentColor,
                            contentColor = colorResource(R.color.white)
                        ),
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))
                    ) {
                        Text(
                            text = quickBookingTitle,
                            style = TripResultStyles.LabelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSegmentSummaryUi(viewModel: TripSegmentViewModel): SegmentSummaryUi {
    val icon by viewModel.icon.observeAsState()
    val showPrimary by viewModel.showPrimary.observeAsState(false)
    val primaryText by viewModel.primaryText.observeAsState()
    val secondaryText by viewModel.secondaryText.observeAsState()
    val isHideExactTimes by viewModel.isHideExactTimes.observeAsState(false)
    val isRealtime by viewModel.isRealtime.observeAsState(false)
    val isBicycleAccessible by viewModel.isBicycleAccessible.observeAsState(false)
    val isCancelled by viewModel.isCancelled.observeAsState(false)

    return SegmentSummaryUi(
        showPrimary = showPrimary,
        primaryText = primaryText.orEmpty(),
        secondaryText = secondaryText,
        isHideExactTimes = isHideExactTimes,
        isRealtime = isRealtime,
        isBicycleAccessible = isBicycleAccessible,
        isCancelled = isCancelled,
        iconDrawable = icon
    )
}

data class TripResultLegPreviewSegmentUi(
    val primaryText: String,
    val secondaryText: String = "",
    val showWifi: Boolean = false,
    val showBike: Boolean = false,
    val iconRes: Int = R.drawable.ic_public_transport
)

@Composable
private fun SegmentSummaryCompose(
    ui: SegmentSummaryUi
) {
    val primaryTextColor = if (ui.isCancelled) {
        colorResource(R.color.light_grey_3)
    } else {
        colorResource(R.color.labelPrimary)
    }
    val secondaryTextColor = if (ui.isCancelled) {
        colorResource(R.color.light_grey_3)
    } else {
        colorResource(R.color.labelSecondary)
    }
    val iconBitmap = remember(ui.iconDrawable) { ui.iconDrawable?.toBitmap()?.asImageBitmap() }

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_extra_small)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                iconBitmap != null -> {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        colorFilter = if (ui.isCancelled) ColorFilter.tint(colorResource(R.color.light_grey_3)) else null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_20))
                    )
                }

                ui.iconRes != null -> {
                    Icon(
                        painter = painterResource(ui.iconRes),
                        contentDescription = null,
                        tint = if (ui.isCancelled) colorResource(R.color.light_grey_3) else Color.Unspecified,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_20))
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))
            ) {
                if (ui.showPrimary) {
                    Text(
                        text = ui.primaryText,
                        style = TripResultStyles.BodySmall,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!ui.isHideExactTimes && !ui.secondaryText.isNullOrEmpty()) {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                TextViewCompat.setTextAppearance(
                                    this,
                                    com.google.android.material.R.style.TextAppearance_MaterialComponents_Caption
                                )
                            }
                        },
                        update = { textView ->
                            textView.text = ui.secondaryText
                            textView.setTextColor(secondaryTextColor.toArgb())
                        }
                    )
                }
            }
        }

        if (ui.isRealtime) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi),
                contentDescription = null,
                tint = primaryTextColor,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(R.dimen.spacing_xx_small),
                        bottom = dimensionResource(R.dimen.spacing_extra_small)
                    )
                    .size(dimensionResource(R.dimen.icon_x_small))
                    .rotate(45f)
            )
        }

        if (ui.isBicycleAccessible) {
            Icon(
                painter = painterResource(R.drawable.ic_bike),
                contentDescription = null,
                tint = primaryTextColor,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(R.dimen.spacing_xx_small),
                        bottom = dimensionResource(R.dimen.spacing_extra_small)
                    )
                    .size(dimensionResource(R.dimen.icon_x_small))
            )
        }
    }
}

private data class SegmentSummaryUi(
    val showPrimary: Boolean,
    val primaryText: String,
    val secondaryText: CharSequence?,
    val isHideExactTimes: Boolean,
    val isRealtime: Boolean,
    val isBicycleAccessible: Boolean,
    val isCancelled: Boolean,
    val iconDrawable: Drawable? = null,
    val iconRes: Int? = null
)

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

@Preview(showBackground = true)
@Composable
private fun TripResultLegRowComposePreview() {
    TripKitUITheme {
        TripResultLegRowCompose(
            title = "10:11 - 10:41",
            subtitle = "30 mins",
            hideExactTimes = false,
            hasQuickBooking = true,
            quickBookingTitle = "Book",
            segments = listOf(
                TripResultLegPreviewSegmentUi(
                    primaryText = "T9",
                    secondaryText = "10:11",
                    showWifi = true
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "Walk",
                    secondaryText = "4 mins",
                    showBike = true
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "",
                    secondaryText = "4 mins",
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "",
                    secondaryText = "",
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "Walk",
                    secondaryText = "4 mins",
                    showBike = true
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "",
                    secondaryText = "",
                ),
                TripResultLegPreviewSegmentUi(
                    primaryText = "",
                    secondaryText = "4 mins",
                ),
            )
        )
    }
}

