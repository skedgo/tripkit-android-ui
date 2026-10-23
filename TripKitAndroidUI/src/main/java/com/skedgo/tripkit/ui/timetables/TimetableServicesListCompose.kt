package com.skedgo.tripkit.ui.timetables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.skedgo.tripkit.ui.R

@Composable
fun TimetableServicesListCompose(
    services: List<ServiceViewModel>,
    listState: LazyListState,
    onVisibleRangeChanged: (firstVisible: Int, lastVisible: Int) -> Unit,
    onReachedEnd: () -> Unit,
    // To temporarily handle the item divider while the Trip Preview page has not yet been
    // migrated/updated to the latest design and Jetpack Compose.
    fromPreview: Boolean = false,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(listState, services.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val firstVisible = visibleItems.firstOrNull()?.index ?: 0
                val lastVisible = visibleItems.lastOrNull()?.index ?: -1
                onVisibleRangeChanged(firstVisible, lastVisible)
                if (services.isNotEmpty() && lastVisible >= services.lastIndex) {
                    onReachedEnd()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = if (fromPreview) {
            modifier.fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection())
                .background(colorResource(R.color.cardBackground))
         } else {
            modifier.fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection())
         },
        contentPadding = PaddingValues(bottom = 0.dp),
    ) {
        itemsIndexed(
            items = services,
            key = { index, item ->
                "${item.service.serviceTripId}_${item.service.startTimeInSecs}_$index"
            }
        ) { index, serviceViewModel ->

            when (index) {
                0 -> {
                    TimetableServiceItemCompose(
                        viewModel = serviceViewModel,
                        itemShape = RoundedCornerShape(
                            topStart = dimensionResource(R.dimen.cardview_corner_radius_medium),
                            topEnd = dimensionResource(R.dimen.cardview_corner_radius_medium),
                            bottomStart = dimensionResource(R.dimen.cardview_corner_radius_small),
                            bottomEnd = dimensionResource(R.dimen.cardview_corner_radius_small),
                        )
                    )
                }
                services.lastIndex -> (
                    TimetableServiceItemCompose(
                        viewModel = serviceViewModel,
                        itemShape = RoundedCornerShape(
                            bottomStart = dimensionResource(R.dimen.cardview_corner_radius_medium),
                            bottomEnd = dimensionResource(R.dimen.cardview_corner_radius_medium),
                            topStart = dimensionResource(R.dimen.cardview_corner_radius_small),
                            topEnd = dimensionResource(R.dimen.cardview_corner_radius_small),
                        )
                    )
                    )
                else -> {
                    TimetableServiceItemCompose(viewModel = serviceViewModel)
                }
            }
        }
    }
}
