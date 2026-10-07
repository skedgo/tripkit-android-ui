package com.skedgo.tripkit.ui.timetables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme

@Composable
fun TimetableSearchSetTimeCompose(
    onSearchTextChanged: (String) -> Unit,
    onTimeClick: () -> Unit,
    initialSearchText: String = ""
) {
    var searchText by rememberSaveable { mutableStateOf(initialSearchText) }
    val inputBackground = colorResource(R.color.inputBackground)
    val iconTextTint = colorResource(R.color.labelPrimary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_normal)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_12)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = dimensionResource(R.dimen.input_height_40))
                .background(
                    color = inputBackground,
                    shape = RoundedCornerShape(percent = 50)
                )
                .padding(
                    start = dimensionResource(R.dimen.spacing_normal),
                    end = dimensionResource(R.dimen.spacing_12)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.quantum_ic_search_grey600_24),
                contentDescription = null,
                tint = iconTextTint,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(R.dimen.spacing_12)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search),
                        color = iconTextTint,
                        style = TripKitComposeTextStyles.current.bodyLarge,
                        textAlign = TextAlign.Start,
                        maxLines = 2
                    )
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        onSearchTextChanged(it)
                    },
                    singleLine = false,
                    maxLines = 2,
                    textStyle = TripKitComposeTextStyles.current.bodyLarge.copy(color = iconTextTint),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.padding(
                        vertical = dimensionResource(R.dimen.spacing_small)
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.input_height_40))
                .background(
                    color = inputBackground,
                    shape = CircleShape
                )
                .clickable(onClick = onTimeClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_timetable_search),
                contentDescription = stringResource(R.string.set_time),
                tint = iconTextTint,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableSearchSetTimePreview() {
    TripKitUITheme {
        TimetableSearchSetTimeCompose(
            onSearchTextChanged = {},
            onTimeClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimetableSearchSetTimeLongTextPreview() {
    TripKitUITheme {
        TimetableSearchSetTimeCompose(
            onSearchTextChanged = {},
            onTimeClick = {},
            initialSearchText = "Very long search query for timetable line"
        )
    }
}
