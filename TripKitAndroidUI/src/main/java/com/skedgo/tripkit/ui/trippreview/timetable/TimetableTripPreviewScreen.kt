package com.skedgo.tripkit.ui.trippreview.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.model.TimetableHeaderLineItem
import com.skedgo.tripkit.ui.timetables.ServiceViewModel
import com.skedgo.tripkit.ui.timetables.TimetableServiceItemCompose
import com.skedgo.tripkit.ui.timetables.TimetableServiceNumberChips
import kotlinx.coroutines.launch

data class TimetableTripPreviewUiState(
    val stationName: String,
    val serviceNumbers: List<TimetableHeaderLineItem>,
    val services: List<ServiceViewModel>,
    val bookingButtonVisible: Boolean,
    val bookingButtonEnabled: Boolean,
    val bookingButtonText: String,
    val isLoading: Boolean,
    val isEmpty: Boolean,
    val errorMessage: String?,
    val firstNowPosition: Int
)

@Composable
fun TimetableTripPreviewScreen(
    state: TimetableTripPreviewUiState,
    onCloseClicked: () -> Unit,
    onBookActionClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onReachedEnd: () -> Unit,
    diagnosticsEnabled: Boolean = false
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showGoToNow by remember(state.services.size, state.firstNowPosition) { mutableStateOf(false) }
    val servicesStartIndex = 3 + if (state.bookingButtonVisible) 1 else 0

    LaunchedEffect(listState, state.services.size, servicesStartIndex, state.firstNowPosition) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val firstVisible = visibleItems.firstOrNull()?.index ?: 0
                val lastVisible = visibleItems.lastOrNull()?.index ?: -1
                if (state.services.isNotEmpty() && lastVisible >= servicesStartIndex + state.services.lastIndex) {
                    onReachedEnd()
                }
                val nowIndex = servicesStartIndex + state.firstNowPosition
                showGoToNow = state.services.isNotEmpty() && nowIndex !in firstVisible..lastVisible
            }
    }
    LaunchedEffect(diagnosticsEnabled, listState) {
        if (!diagnosticsEnabled) return@LaunchedEffect
        snapshotFlow { listState.canScrollBackward to listState.canScrollForward }
            .collect { (backward, forward) ->
                Log.d(DIAG_TAG, "page=TIMETABLE canScrollBackward=$backward canScrollForward=$forward")
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.cardBackground)),
        state = listState,
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_normal)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        item(key = "header") {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.spacing_normal),
                        end = dimensionResource(R.dimen.spacing_small),
                        top = dimensionResource(R.dimen.spacing_small)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.stationName,
                    style = TripKitComposeTextStyles.current.titleLarge,
                    color = colorResource(R.color.labelPrimary),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCloseClicked) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = colorResource(R.color.labelPrimary)
                    )
                }
            }
        }

        item(key = "service_numbers") {
            TimetableServiceNumberChips(
                items = state.serviceNumbers,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_normal))
            )
        }

        item(key = "go_to_now") {
            if (showGoToNow) {
                Button(
                    onClick = {
                        val target = (servicesStartIndex + state.firstNowPosition)
                            .coerceAtMost(servicesStartIndex + state.services.lastIndex)
                        if (target >= servicesStartIndex) {
                            coroutineScope.launch { listState.scrollToItem(target) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_normal))
                ) {
                    Text(text = androidx.compose.ui.res.stringResource(R.string.go_to_now))
                }
            }
        }

        if (state.bookingButtonVisible) {
            item(key = "book_action") {
                Button(
                    onClick = onBookActionClicked,
                    enabled = state.bookingButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_normal))
                ) {
                    Text(text = state.bookingButtonText)
                }
            }
        }

        when {
            state.isLoading -> {
                item(key = "loading") {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(dimensionResource(R.dimen.spacing_normal))
                    )
                }
            }
            state.errorMessage != null -> {
                item(key = "error") {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.spacing_normal)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = colorResource(R.color.tripKitError)
                        )
                        Button(onClick = onRetryClicked) {
                            Text(text = androidx.compose.ui.res.stringResource(R.string.retry))
                        }
                    }
                }
            }
            state.isEmpty -> {
                item(key = "empty") {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.no_results),
                        style = TripKitComposeTextStyles.current.bodyMedium,
                        color = colorResource(R.color.labelSecondary),
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_normal))
                    )
                }
            }
            else -> {
                itemsIndexed(
                    items = state.services,
                    key = { index, item ->
                        "${item.service.serviceTripId}_${item.service.startTimeInSecs}_$index"
                    }
                ) { index, service ->
                    when (index) {
                        0 -> TimetableServiceItemCompose(
                            viewModel = service,
                            itemShape = androidx.compose.foundation.shape.RoundedCornerShape(
                                topStart = dimensionResource(R.dimen.cardview_corner_radius_medium),
                                topEnd = dimensionResource(R.dimen.cardview_corner_radius_medium),
                                bottomStart = dimensionResource(R.dimen.cardview_corner_radius_small),
                                bottomEnd = dimensionResource(R.dimen.cardview_corner_radius_small)
                            )
                        )
                        state.services.lastIndex -> TimetableServiceItemCompose(
                            viewModel = service,
                            itemShape = androidx.compose.foundation.shape.RoundedCornerShape(
                                bottomStart = dimensionResource(R.dimen.cardview_corner_radius_medium),
                                bottomEnd = dimensionResource(R.dimen.cardview_corner_radius_medium),
                                topStart = dimensionResource(R.dimen.cardview_corner_radius_small),
                                topEnd = dimensionResource(R.dimen.cardview_corner_radius_small)
                            )
                        )
                        else -> TimetableServiceItemCompose(viewModel = service)
                    }
                }
            }
        }
    }
}

private const val DIAG_TAG = "TripPreviewComposeDiag"

@Preview(showBackground = true)
@Composable
private fun TimetableTripPreviewScreenPreview() {
    TripKitUITheme {
        TimetableTripPreviewScreen(
            state = TimetableTripPreviewUiState(
                stationName = "Town Hall Station",
                serviceNumbers = listOf(TimetableHeaderLineItem("T1", 0xFF2E7D32.toInt())),
                services = emptyList(),
                bookingButtonVisible = true,
                bookingButtonEnabled = true,
                bookingButtonText = "Book",
                isLoading = false,
                isEmpty = true,
                errorMessage = null,
                firstNowPosition = 0
            ),
            onCloseClicked = {},
            onBookActionClicked = {},
            onRetryClicked = {},
            onReachedEnd = {}
        )
    }
}
