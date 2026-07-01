package com.skedgo.tripkit.ui.tripresults.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.BindingAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.TripResultTripViewModel
import com.skedgo.tripkit.ui.tripresults.TripResultViewModel
import com.skedgo.tripkit.ui.utils.resolveComposeColor

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

    TripResultListItemCompose(
        ui = TripResultListItemUi(
            hasTripLabels = hasTripLabels,
            badgeVisible = badgeVisible,
            badgeDrawable = badgeDrawable,
            badgeText = badgeText.orEmpty(),
            badgeTextColor = badgeTextColor,
            rows = tripRows.map { TripResultListItemRowUi.Runtime(viewModel = it) },
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
    )
}

@Composable
fun TripResultListItemCompose(
    ui: TripResultListItemUi
) {
    val accentColor = resolveComposeColor(
        default = ContextCompat.getColor(LocalContext.current, R.color.colorAccent)
    )

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
            if (ui.hasTripLabels && ui.badgeVisible) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.spacing_normal),
                        top = dimensionResource(R.dimen.spacing_normal),
                        end = dimensionResource(R.dimen.spacing_normal),
                        bottom = dimensionResource(R.dimen.spacing_10)
                    )
                ) {
                    BadgeDrawable(ui.badgeDrawable)
                    Text(
                        text = ui.badgeText,
                        style = androidx.compose.material.MaterialTheme.typography.overline,
                        color = Color(ui.badgeTextColor ?: colorResource(R.color.black).toArgb())
                    )
                }
            }

            ui.rows.forEachIndexed { index, row ->
                when (row) {
                    is TripResultListItemRowUi.Runtime -> {
                        TripResultLegRowCompose(viewModel = row.viewModel)
                    }

                    is TripResultListItemRowUi.Static -> {
                        TripResultLegRowCompose(
                            title = row.title,
                            subtitle = row.subtitle,
                            hideExactTimes = row.hideExactTimes,
                            hasQuickBooking = row.hasQuickBooking,
                            quickBookingTitle = row.quickBookingTitle,
                            segments = row.segments
                        )
                    }
                }

                if (index < ui.rows.lastIndex) {
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.spacing_xx_small))
                    )
                }
            }

            if (ui.alternateTripVisible) {
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
                        horizontal = dimensionResource(R.dimen.spacing_normal),
                    )
            ) {
                if (ui.costVisible) {
                    Text(
                        text = ui.cost,
                        style = androidx.compose.material.MaterialTheme.typography.caption,
                        color = colorResource(R.color.black1),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (ui.moneyCostVisible) {
                    Text(
                        text = ui.moneyCost,
                        style = androidx.compose.material.MaterialTheme.typography.caption,
                        color = colorResource(R.color.black1),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (ui.availabilityInfo.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_warning),
                            contentDescription = null,
                            tint = colorResource(R.color.tripKitError),
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_small))
                        )
                        Text(
                            text = ui.availabilityInfo,
                            style = androidx.compose.material.MaterialTheme.typography.caption,
                            color = colorResource(R.color.tripKitError),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))
                        )
                    }
                }

                if (ui.moreButtonVisible) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        enabled = ui.actionEnabled,
                        onClick = ui.onMoreClick
                    ) {
                        Text(
                            text = ui.moreButtonText,
                            style = androidx.compose.material.MaterialTheme.typography.button,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

data class TripResultListItemUi(
    val hasTripLabels: Boolean,
    val badgeVisible: Boolean,
    val badgeDrawable: Drawable?,
    val badgeText: String,
    val badgeTextColor: Int? = null,
    val rows: List<TripResultListItemRowUi>,
    val alternateTripVisible: Boolean,
    val costVisible: Boolean,
    val cost: String,
    val moneyCostVisible: Boolean,
    val moneyCost: String,
    val availabilityInfo: String,
    val moreButtonVisible: Boolean,
    val moreButtonText: String,
    val actionEnabled: Boolean,
    val onMoreClick: () -> Unit = {}
)

sealed interface TripResultListItemRowUi {
    data class Runtime(
        val viewModel: TripResultTripViewModel
    ) : TripResultListItemRowUi

    data class Static(
        val title: String,
        val subtitle: String,
        val hideExactTimes: Boolean,
        val hasQuickBooking: Boolean,
        val quickBookingTitle: String,
        val segments: List<TripResultLegPreviewSegmentUi>
    ) : TripResultListItemRowUi
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

@Preview(showBackground = true, backgroundColor = 0xFF5F5F60)
@Composable
private fun TripResultListItemComposePreview() {
    TripKitUITheme {
        TripResultListItemCompose(
            ui = TripResultListItemUi(
                hasTripLabels = true,
                badgeVisible = true,
                badgeDrawable = null,
                badgeText = "FASTEST",
                rows = listOf(
                    TripResultListItemRowUi.Static(
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

