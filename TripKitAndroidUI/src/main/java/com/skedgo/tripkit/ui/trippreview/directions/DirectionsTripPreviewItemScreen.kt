package com.skedgo.tripkit.ui.trippreview.directions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel

data class DirectionsStepUiState(
    val title: String,
    val description: String
)

@Composable
fun DirectionsTripPreviewItemScreen(
    viewModel: TripPreviewPagerItemViewModel,
    steps: List<DirectionsStepUiState>,
    onLaunchInMaps: () -> Unit,
    onCloseClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.spacing_normal)),
                verticalAlignment = Alignment.Top
            ) {
                viewModel.icon.get()?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.title.get().orEmpty(),
                        style = TripKitComposeTextStyles.current.titleLarge,
                        color = MaterialTheme.colors.onSurface
                    )
                    viewModel.description.get()?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                    if (viewModel.showLaunchInMaps.get()) {
                        OutlinedButton(
                            onClick = onLaunchInMaps,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.spacing_small))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_go),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                            Text(
                                text = "Open in",
                                style = TripKitComposeTextStyles.current.labelLarge
                            )
                        }
                    }
                }
                IconButton(onClick = onCloseClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = null
                    )
                }
            }
        }
        items(steps) { step ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.spacing_normal),
                        vertical = dimensionResource(id = R.dimen.spacing_small)
                    )
            ) {
                Text(
                    text = step.title,
                    style = TripKitComposeTextStyles.current.bodyLarge,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = step.description,
                    style = TripKitComposeTextStyles.current.bodyMedium,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
        item {
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DirectionsTripPreviewItemScreenPreview() {
    val vm = TripPreviewPagerItemViewModel().apply {
        title.set("Walk")
        description.set("Towards destination")
        showLaunchInMaps.set(true)
    }
    TripKitUITheme {
        DirectionsTripPreviewItemScreen(
            viewModel = vm,
            steps = listOf(
                DirectionsStepUiState("120 m", "Along Main St"),
                DirectionsStepUiState("240 m", "Turn right on Queen St")
            ),
            onLaunchInMaps = {},
            onCloseClicked = {}
        )
    }
}
