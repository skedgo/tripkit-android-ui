package com.skedgo.tripkit.ui.trippreview.external

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.trippreview.Action
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel

data class ExternalActionRowState(
    val title: String,
    val action: Action?
)

@Composable
fun ExternalActionTripPreviewItemScreen(
    viewModel: TripPreviewPagerItemViewModel,
    actions: List<ExternalActionRowState>,
    onActionClicked: (Action) -> Unit,
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
                    androidx.compose.foundation.Image(
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
                    if (!viewModel.description.get().isNullOrBlank()) {
                        Text(
                            text = viewModel.description.get().orEmpty(),
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
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

        items(actions) { item ->
            OutlinedButton(
                onClick = { item.action?.let(onActionClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.spacing_normal),
                        vertical = dimensionResource(id = R.dimen.spacing_extra_small)
                    )
            ) {
                Text(
                    text = item.title,
                    style = TripKitComposeTextStyles.current.bodyMedium
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
private fun ExternalActionTripPreviewItemScreenPreview() {
    val vm = TripPreviewPagerItemViewModel().apply {
        title.set("External action")
        description.set("Book with partner")
    }
    TripKitUITheme {
        ExternalActionTripPreviewItemScreen(
            viewModel = vm,
            actions = listOf(
                ExternalActionRowState("Open app", null),
                ExternalActionRowState("Show website", null)
            ),
            onActionClicked = {},
            onCloseClicked = {}
        )
    }
}
