package com.skedgo.tripkit.ui.trippreview.nearby

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection

data class NearbyModeUiState(
    val id: String,
    val iconRes: Int,
    val selected: Boolean
)

data class NearbyResultUiState(
    val id: String,
    val iconRes: Int,
    val title: String,
    val location: String,
    val distance: String
)

data class NearbyPageUiState(
    val modes: List<NearbyModeUiState>,
    val results: List<NearbyResultUiState>,
    val isLoading: Boolean,
    val isEmpty: Boolean,
    val errorMessage: String? = null
)

@Composable
fun NearbyTripPreviewItemScreen(
    viewModel: TripPreviewPagerItemViewModel,
    state: NearbyPageUiState,
    onModeToggle: (String) -> Unit,
    onResultClick: (String) -> Unit,
    onRetry: () -> Unit,
    onCloseClicked: () -> Unit
) {
    val context = LocalContext.current
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
                }
                IconButton(onClick = onCloseClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = context.getString(R.string.desc_close)
                    )
                }
            }
        }

        if (state.modes.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(id = R.color.grey2))
                        .padding(
                            start = dimensionResource(id = R.dimen.spacing_normal),
                            top = dimensionResource(id = R.dimen.spacing_normal),
                            bottom = dimensionResource(id = R.dimen.spacing_small)
                        ),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_normal))
                ) {
                    items(items = state.modes, key = { it.id }) { mode ->
                        Surface(
                            color = if (mode.selected) {
                                colorResource(id = R.color.tripKitSuccess)
                            } else {
                                colorResource(id = R.color.cardBackground)
                            },
                            modifier = Modifier
                                .clickable { onModeToggle(mode.id) }
                                .padding(2.dp)
                        ) {
                            Image(
                                bitmap = ContextCompat.getDrawable(context, mode.iconRes)
                                    ?.toBitmap()
                                    ?.asImageBitmap() ?: return@Surface,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        when {
            state.errorMessage != null -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(id = R.dimen.spacing_normal))
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = MaterialTheme.colors.error
                        )
                        Text(
                            text = context.getString(R.string.retry),
                            style = TripKitComposeTextStyles.current.labelLarge,
                            modifier = Modifier
                                .padding(top = dimensionResource(id = R.dimen.spacing_small))
                                .clickable(onClick = onRetry)
                        )
                    }
                }
            }
            state.isLoading -> {
                item {
                    Text(
                        text = context.getString(R.string.loading_dot_dot_dot),
                        style = TripKitComposeTextStyles.current.bodyMedium,
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_normal))
                    )
                }
            }
            state.isEmpty -> {
                item {
                    Text(
                        text = context.getString(R.string.no_results),
                        style = TripKitComposeTextStyles.current.bodyMedium,
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_normal))
                    )
                }
            }
            else -> {
                items(items = state.results, key = { it.id }) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(result.id) }
                            .padding(dimensionResource(id = R.dimen.spacing_normal)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (result.iconRes != 0) {
                            Image(
                                bitmap = ContextCompat.getDrawable(context, result.iconRes)
                                    ?.toBitmap()
                                    ?.asImageBitmap() ?: return@Row,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_normal)))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.title,
                                style = TripKitComposeTextStyles.current.bodyLarge,
                                color = MaterialTheme.colors.onSurface
                            )
                            if (result.location.isNotBlank()) {
                                Text(
                                    text = result.location,
                                    style = TripKitComposeTextStyles.current.bodyMedium,
                                    color = MaterialTheme.colors.onSurface
                                )
                            }
                        }
                        if (result.distance.isNotBlank()) {
                            Text(
                                text = result.distance,
                                style = TripKitComposeTextStyles.current.bodyMedium,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NearbyTripPreviewItemScreenPreview() {
    val viewModel = TripPreviewPagerItemViewModel().apply {
        title.set("Nearby")
        description.set("Choose nearby mode")
    }
    TripKitUITheme {
        NearbyTripPreviewItemScreen(
            viewModel = viewModel,
            state = NearbyPageUiState(
                modes = listOf(
                    NearbyModeUiState("bike", R.drawable.ic_bike, true),
                    NearbyModeUiState("car", R.drawable.ic_car, false)
                ),
                results = listOf(
                    NearbyResultUiState("1", R.drawable.ic_bike, "Bike Pod", "Main St", "120m")
                ),
                isLoading = false,
                isEmpty = false
            ),
            onModeToggle = {},
            onResultClick = {},
            onRetry = {},
            onCloseClicked = {}
        )
    }
}
