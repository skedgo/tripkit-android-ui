package com.skedgo.tripkit.ui.tripresults.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.TripResultTransportItemViewModel

@BindingAdapter("tripResultsTransportItemViewModel")
fun bindTripResultsTransportFilterItemCompose(
    view: ComposeView,
    viewModel: TripResultTransportItemViewModel?
) {
    view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    view.setContent {
        TripKitUITheme {
            if (viewModel != null) {
                TripResultTransportFilterItemCompose(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TripResultTransportFilterItemCompose(
    viewModel: TripResultTransportItemViewModel
) {
    val checked by viewModel.checked.observeAsState(false)
    val modeId by viewModel.modeId.observeAsState()
    val modeIconId by viewModel.modeIconId.observeAsState()
    val currentView = LocalView.current
    val context = LocalContext.current
    val iconRes = remember(modeId, modeIconId) {
        resolveTransportIconRes(context, modeId, modeIconId)
    }

    TripResultTransportFilterItemContent(
        checked = checked,
        onClick = { viewModel.onItemClick(currentView) }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TripResultTransportFilterItemContent(
    checked: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val background = if (checked) {
        colorResource(R.color.transport_mode_selector_checked)
    } else {
        colorResource(R.color.transport_mode_selector_unchecked)
    }

    Box(
        modifier = Modifier
            .padding(end = dimensionResource(R.dimen.spacing_small), bottom = dimensionResource(R.dimen.spacing_small))
            .size(dimensionResource(R.dimen.button_size_40))
            .alpha(if (checked) 1f else 0.25f)
            .background(background, CircleShape)
            .clickable(onClick = onClick)
            .padding(dimensionResource(R.dimen.spacing_small))
    ) {
        icon()
    }
}

@Preview(showBackground = true)
@Composable
private fun TripResultTransportFilterItemCheckedPreview() {
    TripKitUITheme {
        TripResultTransportFilterItemContent(
            checked = true,
            onClick = {}
        ) {
            androidx.compose.material.Icon(
                painter = painterResource(R.drawable.ic_public_transport),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripResultTransportFilterItemUncheckedPreview() {
    TripKitUITheme {
        TripResultTransportFilterItemContent(
            checked = false,
            onClick = {}
        ) {
            androidx.compose.material.Icon(
                painter = painterResource(R.drawable.ic_public_transport),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun resolveTransportIconRes(
    context: android.content.Context,
    modeId: String?,
    modeIconId: String?
): Int {
    val byModeId = TransportMode.getLocalIconResId(modeId)
    if (byModeId != 0) {
        return byModeId
    }

    val byModeIcon = TransportMode.getLocalIconResId(modeIconId)
    if (byModeIcon != 0) {
        return byModeIcon
    }

    if (!modeIconId.isNullOrBlank()) {
        val customResId = context.resources.getIdentifier(modeIconId, "drawable", context.packageName)
        if (customResId != 0) {
            return customResId
        }
    }

    return R.drawable.ic_car_ride_share
}

