package com.skedgo.tripkit.ui.trippreview.standard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.skedgo.tripkit.booking.LinkFormField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel

@Composable
fun StandardTripPreviewScreen(
    viewModel: TripPreviewPagerItemViewModel,
    messageTitle: String,
    message: String,
    messageVisible: Boolean,
    actionButtons: List<LinkFormField>,
    onActionClicked: (LinkFormField) -> Unit,
    onCloseClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val title = viewModel.title.get().orEmpty()
    val description = viewModel.description.get().orEmpty()
    val notes = viewModel.notes.get().orEmpty()
    val showDescription = viewModel.showDescription.get()
    val showOpenIn = viewModel.showLaunchInMaps.get()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = dimensionResource(id = R.dimen.spacing_normal),
                    ),
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = TripKitComposeTextStyles.current.titleLarge,
                        color = MaterialTheme.colors.onSurface,
                    )
                    if (showDescription && description.isNotBlank()) {
                        Text(
                            text = description,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showOpenIn) {
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_small)))
                        OutlinedButton(
                            onClick = { viewModel.showLaunchInMapsClicked.perform() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colors.primary
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_go),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
                            Text(
                                text = context.getString(R.string.open_in),
                                style = TripKitComposeTextStyles.current.labelLarge
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onCloseClicked?.invoke() ?: viewModel.closeClicked.perform() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = context.getString(R.string.desc_close)
                    )
                }
            }
        }

        item {
            if (messageVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.spacing_normal))
                        .background(
                            color = colorResource(id = R.color.tripKitSuccess),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(dimensionResource(id = R.dimen.spacing_small))
                ) {
                    Text(
                        text = messageTitle,
                        style = TripKitComposeTextStyles.current.bodyLarge,
                        color = Color.Black
                    )
                    if (message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = message,
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        if (notes.isNotBlank()) {
            item {
                Text(
                    text = notes,
                    style = TripKitComposeTextStyles.current.bodyLarge,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_normal))
                )
            }
        }

        items(actionButtons) { formField ->
            OutlinedButton(
                onClick = { onActionClicked(formField) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.spacing_normal),
                        vertical = dimensionResource(id = R.dimen.spacing_small)
                    )
            ) {
                Text(
                    text = formField.title.orEmpty(),
                    style = TripKitComposeTextStyles.current.bodyMedium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_normal)))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StandardTripPreviewScreenPreview() {
    val viewModel = TripPreviewPagerItemViewModel().apply {
        title.set("Trip Preview Item")
        description.set("Sample segment description for preview")
        notes.set("Some kind of instruction goes here for preview.")
        showDescription.set(true)
        showLaunchInMaps.set(true)
    }
    val firstAction = LinkFormField().apply {
        title = "End Booking"
        id = "end_booking"
        setValue("https://example.com/booking/end")
    }
    val secondAction = LinkFormField().apply {
        title = "More Details"
        id = "end_booking"
        setValue("https://example.com/booking/details")
    }

    TripKitUITheme {
        StandardTripPreviewScreen(
            viewModel = viewModel,
            messageTitle = "Status",
            message = "Booking confirmed",
            messageVisible = true,
            actionButtons = listOf(firstAction, secondAction),
            onActionClicked = {}
        )
    }
}
