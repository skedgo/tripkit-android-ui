package com.skedgo.tripkit.ui.tripresults.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.BindingAdapter
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.TripResultViewModel

@BindingAdapter("tripResultsItemViewModel")
fun bindTripResultsListItemCompose(
    view: ComposeView,
    viewModel: TripResultViewModel?
) {
    view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    view.setContent {
        TripKitUITheme {
            if (viewModel != null) {
                TripResultListItemCompose(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TripResultListItemCompose(
    viewModel: TripResultViewModel
) {
    val hasTripLabels by viewModel.hasTripLabels.observeAsState(true)
    val badgeVisible by viewModel.badgeVisible.observeAsState(false)
    val badgeDrawable by viewModel.badgeDrawable.observeAsState()
    val badgeText by viewModel.badgeText.observeAsState()
    val badgeTextColor by viewModel.badgeTextColor.observeAsState()
    val tripRows by viewModel.tripResults.observeAsState(mutableListOf())
    val alternateTripVisible by viewModel.alternateTripVisible.observeAsState(false)
    val costVisible by viewModel.costVisible.observeAsState(true)
    val cost by viewModel.cost.observeAsState("")
    val moneyCost by viewModel.moneyCost.observeAsState("")
    val moneyCostVisible by viewModel.isMoneyCostVisible.observeAsState(false)
    val availabilityInfo by viewModel.availabilityInfo.observeAsState("")
    val moreButtonVisible by viewModel.moreButtonVisible.observeAsState(false)
    val moreButtonText by viewModel.moreButtonText.observeAsState("")
    val actionEnabled by viewModel.isActionEnabled.observeAsState(true)

    TripResultListItemContent(
        hasTripLabels = hasTripLabels,
        badgeVisible = badgeVisible,
        badgeDrawable = badgeDrawable,
        badgeText = badgeText.orEmpty(),
        badgeTextColor = badgeTextColor,
        rowsCount = tripRows.size,
        renderRow = { index ->
            TripResultLegRowCompose(viewModel = tripRows[index])
        },
        alternateTripVisible = alternateTripVisible,
        costVisible = costVisible,
        cost = cost.orEmpty(),
        moneyCostVisible = moneyCostVisible,
        moneyCost = moneyCost.orEmpty(),
        availabilityInfo = availabilityInfo.orEmpty(),
        moreButtonVisible = moreButtonVisible,
        moreButtonText = moreButtonText.orEmpty(),
        actionEnabled = actionEnabled,
        onMoreClick = { viewModel.onMoreButtonClicked.perform() }
    )
}

@Composable
fun TripResultListItemCompose(
    previewUi: TripResultListItemPreviewUi
) {
    TripResultListItemContent(
        hasTripLabels = previewUi.hasTripLabels,
        badgeVisible = previewUi.badgeVisible,
        badgeDrawable = null,
        badgeText = previewUi.badgeText,
        badgeTextColor = previewUi.badgeTextColor,
        rowsCount = previewUi.rows.size,
        renderRow = { index ->
            val row = previewUi.rows[index]
            TripResultLegRowCompose(
                title = row.title,
                subtitle = row.subtitle,
                hideExactTimes = row.hideExactTimes,
                hasQuickBooking = row.hasQuickBooking,
                quickBookingTitle = row.quickBookingTitle,
                segments = row.segments
            )
        },
        alternateTripVisible = previewUi.alternateTripVisible,
        costVisible = previewUi.costVisible,
        cost = previewUi.cost,
        moneyCostVisible = previewUi.moneyCostVisible,
        moneyCost = previewUi.moneyCost,
        availabilityInfo = previewUi.availabilityInfo,
        moreButtonVisible = previewUi.moreButtonVisible,
        moreButtonText = previewUi.moreButtonText,
        actionEnabled = previewUi.actionEnabled,
        onMoreClick = {}
    )
}

@Composable
private fun TripResultListItemContent(
    hasTripLabels: Boolean,
    badgeVisible: Boolean,
    badgeDrawable: Drawable?,
    badgeText: String,
    badgeTextColor: Int?,
    rowsCount: Int,
    renderRow: @Composable (Int) -> Unit,
    alternateTripVisible: Boolean,
    costVisible: Boolean,
    cost: String,
    moneyCostVisible: Boolean,
    moneyCost: String,
    availabilityInfo: String,
    moreButtonVisible: Boolean,
    moreButtonText: String,
    actionEnabled: Boolean,
    onMoreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius_medium)),
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_normal),
                top = dimensionResource(R.dimen.spacing_extra_small),
                end = dimensionResource(R.dimen.spacing_normal),
                bottom = dimensionResource(R.dimen.spacing_small)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasTripLabels && badgeVisible) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.spacing_normal),
                        top = dimensionResource(R.dimen.spacing_normal),
                        end = dimensionResource(R.dimen.spacing_normal),
                        bottom = dimensionResource(R.dimen.spacing_10)
                    )
                ) {
                    BadgeDrawable(badgeDrawable)
                    Text(
                        text = badgeText,
                        style = androidx.compose.material.MaterialTheme.typography.overline,
                        color = Color(badgeTextColor ?: colorResource(R.color.black).toArgb())
                    )
                }
            }

            repeat(rowsCount) { index ->
                renderRow(index)
                if (index < rowsCount - 1) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.spacing_normal))
                            .fillMaxWidth()
                            .height(dimensionResource(R.dimen.divider_size))
                            .background(colorResource(R.color.black3))
                    )
                }
            }

            if (alternateTripVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.divider_size))
                        .background(colorResource(R.color.black3))
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.spacing_normal),
                        top = dimensionResource(R.dimen.spacing_12),
                        end = dimensionResource(R.dimen.spacing_xx_small),
                        bottom = dimensionResource(R.dimen.spacing_12)
                    )
            ) {
                if (costVisible) {
                    Text(
                        text = cost,
                        style = androidx.compose.material.MaterialTheme.typography.caption,
                        color = colorResource(R.color.black1),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (moneyCostVisible) {
                    Text(
                        text = moneyCost,
                        style = androidx.compose.material.MaterialTheme.typography.caption,
                        color = colorResource(R.color.black1),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (availabilityInfo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_warning),
                            contentDescription = null,
                            tint = colorResource(R.color.tripKitError),
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                        )
                        Text(
                            text = availabilityInfo,
                            style = androidx.compose.material.MaterialTheme.typography.caption,
                            color = colorResource(R.color.tripKitError),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))
                        )
                    }
                }

                if (moreButtonVisible) {
                    TextButton(
                        enabled = actionEnabled,
                        onClick = onMoreClick
                    ) {
                        Text(
                            text = moreButtonText,
                            style = androidx.compose.material.MaterialTheme.typography.button,
                            color = colorResource(R.color.colorAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeDrawable(
    drawable: Drawable?
) {
    val badgeBitmap = remember(drawable) { drawable?.toBitmap()?.asImageBitmap() }
    badgeBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_small))
                .padding(end = dimensionResource(R.dimen.spacing_small))
        )
    }
}

data class TripResultListItemPreviewUi(
    val hasTripLabels: Boolean,
    val badgeVisible: Boolean,
    val badgeText: String,
    val badgeTextColor: Int? = null,
    val rows: List<TripResultListItemPreviewRowUi>,
    val alternateTripVisible: Boolean,
    val costVisible: Boolean,
    val cost: String,
    val moneyCostVisible: Boolean,
    val moneyCost: String,
    val availabilityInfo: String,
    val moreButtonVisible: Boolean,
    val moreButtonText: String,
    val actionEnabled: Boolean
)

data class TripResultListItemPreviewRowUi(
    val title: String,
    val subtitle: String,
    val hideExactTimes: Boolean,
    val hasQuickBooking: Boolean,
    val quickBookingTitle: String,
    val segments: List<TripResultLegPreviewSegmentUi>
)

@Preview(showBackground = true, backgroundColor = 0xFF5F5F60)
@Composable
private fun TripResultListItemComposePreview() {
    TripKitUITheme {
        TripResultListItemCompose(
            previewUi = TripResultListItemPreviewUi(
                hasTripLabels = true,
                badgeVisible = true,
                badgeText = "FASTEST",
                rows = listOf(
                    TripResultListItemPreviewRowUi(
                        title = "10:11 - 10:41",
                        subtitle = "30 mins",
                        hideExactTimes = false,
                        hasQuickBooking = true,
                        quickBookingTitle = "Book",
                        segments = listOf(
                            TripResultLegPreviewSegmentUi("T9", "10:11", showWifi = true),
                            TripResultLegPreviewSegmentUi("Walk", "4 mins", showBike = true)
                        )
                    )
                ),
                alternateTripVisible = false,
                costVisible = true,
                cost = "\$20 · 120 cal · 2.3 CO₂",
                moneyCostVisible = true,
                moneyCost = "10 USD",
                availabilityInfo = "",
                moreButtonVisible = true,
                moreButtonText = "More",
                actionEnabled = true
            )
        )
    }
}

