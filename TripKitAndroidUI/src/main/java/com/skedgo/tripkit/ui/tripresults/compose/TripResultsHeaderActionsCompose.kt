package com.skedgo.tripkit.ui.tripresults.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.compose.styles.TripResultStyles

@Composable
fun TripResultsHeaderActions(
    timeLabel: String,
    showTransportModeSelection: Boolean,
    onLeaveNowClick: () -> Unit,
    onTransportsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TripResultsHeaderActionButton(
            text = timeLabel.ifBlank { stringResource(R.string.leave_now) },
            iconRes = R.drawable.ic_leave_now,
            onClick = onLeaveNowClick
        )
        if (showTransportModeSelection) {
            TripResultsHeaderActionButton(
                text = stringResource(R.string.transport),
                iconRes = R.drawable.ic_transports,
                onClick = onTransportsClick
            )
        }
    }
}

@Composable
fun TripResultsHeaderActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = colorResource(R.color.labelPrimary)

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = colorResource(R.color.inputBackground),
        modifier = modifier.heightIn(min = dimensionResource(R.dimen.button_size_40))
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    start = dimensionResource(R.dimen.spacing_12),
                    top = dimensionResource(R.dimen.spacing_small),
                    end = dimensionResource(R.dimen.spacing_normal),
                    bottom = dimensionResource(R.dimen.spacing_small)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
            )
            Text(
                text = text,
                style = TripResultStyles.LabelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_extra_small))
            )
        }
    }
}

@Composable
internal fun ObservableBoolean.observeAsState(): State<Boolean> {
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

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripResultsHeaderActionsPreview() {
    TripKitUITheme {
        TripResultsHeaderActions(
            timeLabel = "Leave now",
            showTransportModeSelection = true,
            onLeaveNowClick = {},
            onTransportsClick = {}
        )
    }
}
