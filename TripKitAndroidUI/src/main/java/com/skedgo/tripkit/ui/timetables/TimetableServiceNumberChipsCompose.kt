package com.skedgo.tripkit.ui.timetables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.model.TimetableHeaderLineItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableServiceNumberChips(
    items: List<TimetableHeaderLineItem>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_extra_small))
    ) {
        items.forEach { item ->
            TimetableServiceNumberChip(
                serviceNumber = item.serviceNumber,
                serviceColor = Color(item.serviceColor)
            )
        }
    }
}

@Composable
private fun TimetableServiceNumberChip(
    serviceNumber: String,
    serviceColor: Color
) {
    Surface(
        shape = RoundedCornerShape(dimensionResource(R.dimen.button_radius_small)),
        color = serviceColor,
        modifier = Modifier.padding(
            vertical = dimensionResource(R.dimen.spacing_extra_small)
        )
    ) {
        Text(
            text = serviceNumber,
            color = colorResource(R.color.white),
            style = TripKitComposeTextStyles.current.labelLarge,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_small),
                vertical = dimensionResource(R.dimen.spacing_xx_small)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableServiceNumberChipsPreview() {
    TripKitUITheme {
        TimetableServiceNumberChips(
            items = listOf(
                TimetableHeaderLineItem("L2", 0xFFFF8D28.toInt()),
                TimetableHeaderLineItem("L4", 0xFFFF8D28.toInt()),
                TimetableHeaderLineItem("T1", 0xFFFF8D28.toInt()),
                TimetableHeaderLineItem("T8", 0xFFFF8D28.toInt()),
                TimetableHeaderLineItem("T9", 0xFFFF8D28.toInt())
            ),
            modifier = Modifier.padding(PaddingValues(horizontal = 8.dp))
        )
    }
}
