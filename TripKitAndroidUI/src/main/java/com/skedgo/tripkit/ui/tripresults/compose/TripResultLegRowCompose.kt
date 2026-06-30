package com.skedgo.tripkit.ui.tripresults.compose

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
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
            SegmentSummaryCompose(viewModel = segment)
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
            PreviewSegmentSummaryCompose(segment = segment)
        }
    }
}

@Composable
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
                segmentsContent()
                Spacer(
                    modifier = Modifier.weight(1f)
                )
                if (hasQuickBooking) {
                    Button(
                        onClick = onQuickBookingClick,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colorResource(R.color.colorAccent),
                            contentColor = colorResource(R.color.white)
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = dimensionResource(R.dimen.trip_result_action_height))
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
private fun SegmentSummaryCompose(
    viewModel: TripSegmentViewModel
) {
    val icon by viewModel.icon.observeAsState()
    val showPrimary by viewModel.showPrimary.observeAsState(false)
    val primaryText by viewModel.primaryText.observeAsState()
    val secondaryText by viewModel.secondaryText.observeAsState()
    val isHideExactTimes by viewModel.isHideExactTimes.observeAsState(false)
    val isRealtime by viewModel.isRealtime.observeAsState(false)
    val isBicycleAccessible by viewModel.isBicycleAccessible.observeAsState(false)
    val isCancelled by viewModel.isCancelled.observeAsState(false)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    ) {
        val iconBitmap = remember(icon) { icon?.toBitmap()?.asImageBitmap() }
        iconBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = null,
                colorFilter = if (isCancelled) {
                    ColorFilter.tint(colorResource(R.color.light_grey_3))
                } else {
                    null
                },
                modifier = Modifier.size(dimensionResource(R.dimen.icon_regular))
            )
        }

        Column(
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))
        ) {
            if (showPrimary) {
                Text(
                    text = primaryText.orEmpty(),
                    style = androidx.compose.material.MaterialTheme.typography.caption,
                    color = if (isCancelled) {
                        colorResource(R.color.light_grey_3)
                    } else {
                        colorResource(R.color.black)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isHideExactTimes && !secondaryText.isNullOrEmpty()) {
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
                        textView.text = secondaryText
                        textView.setTextColor(
                            ContextCompat.getColor(
                                textView.context,
                                if (isCancelled) R.color.light_grey_3 else R.color.black1
                            )
                        )
                    }
                )
            }
        }

        if (isRealtime) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi),
                contentDescription = null,
                tint = colorResource(R.color.icon_tint_default),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacing_extra_small))
                    .size(dimensionResource(R.dimen.icon_x_small))
            )
        }

        if (isBicycleAccessible) {
            Icon(
                painter = painterResource(R.drawable.ic_bike),
                contentDescription = null,
                tint = colorResource(R.color.icon_tint_default),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacing_extra_small))
                    .size(dimensionResource(R.dimen.icon_x_small))
            )
        }
    }
}

data class TripResultLegPreviewSegmentUi(
    val primaryText: String,
    val secondaryText: String = "",
    val showWifi: Boolean = false,
    val showBike: Boolean = false,
    val iconRes: Int = R.drawable.ic_public_transport
)

@Composable
private fun PreviewSegmentSummaryCompose(
    segment: TripResultLegPreviewSegmentUi
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_extra_small))
        ) {
            Icon(
                painter = painterResource(segment.iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_20))
            )
            Column(modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))) {
                Text(
                    text = segment.primaryText,
                    style = TripResultStyles.BodySmall,
                    color = colorResource(R.color.labelPrimary)
                )
                if (segment.secondaryText.isNotEmpty()) {
                    Text(
                        text = segment.secondaryText,
                        style = TripResultStyles.BodySmall,
                        color = colorResource(R.color.labelSecondary)
                    )
                }
            }
        }
        if (segment.showWifi) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi),
                contentDescription = null,
                tint = colorResource(R.color.icon_tint_default),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacing_extra_small))
                    .size(dimensionResource(R.dimen.icon_x_small))
            )
        }
        if (segment.showBike) {
            Icon(
                painter = painterResource(R.drawable.ic_bike),
                contentDescription = null,
                tint = colorResource(R.color.icon_tint_default),
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacing_extra_small))
                    .size(dimensionResource(R.dimen.icon_x_small))
            )
        }
    }
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
                )
            )
        )
    }
}

