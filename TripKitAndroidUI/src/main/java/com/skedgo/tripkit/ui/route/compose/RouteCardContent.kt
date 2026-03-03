package com.skedgo.tripkit.ui.route.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.route.styles.RouteUiStyle

private val DefaultHorizontalPadding = 16.dp
private val DefaultTopPadding = 12.dp

@Composable
fun RouteCardContent(
    model: RouteUiModel,
    modifier: Modifier = Modifier,
    style: RouteUiStyle = RouteUiStyle()
) {
    val primaryColor = style.primaryColor ?: colorResource(R.color.colorPrimary)
    val confirmBackground = style.confirmButtonBackground ?: primaryColor
    val titleColor = style.titleColor ?: colorResource(R.color.labelPrimary)
    val inputBackground = style.inputBackground ?: colorResource(R.color.inputBackground)
    val iconTint = style.iconTint ?: colorResource(R.color.labelSecondary)
    val horizontalPadding = style.horizontalPadding ?: DefaultHorizontalPadding
    val verticalSpacing = style.verticalSpacing ?: DefaultTopPadding

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorResource(R.color.cardBackground))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = model.onClose,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_40))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = stringResource(id = R.string.desc_close),
                    tint = titleColor,
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacing_extra_small))
                )
            }

            Text(
                text = stringResource(id = R.string.route),
                style = TripKitComposeTextStyles.current.titleLarge,
                color = titleColor
            )

            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_size_40))
                    .clip(CircleShape)
                    .background(confirmBackground)
                    .clickable(
                        enabled = model.canConfirm,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(color = MaterialTheme.colors.onPrimary),
                        onClick = model.onConfirm
                    )
                    .semantics { contentDescription = "Confirm route" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_route_check),
                    contentDescription = null,
                    tint = colorResource(R.color.white),
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_20))
                )
            }
        }

        Spacer(modifier = Modifier.height(verticalSpacing))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_12),
                    end = dimensionResource(R.dimen.spacing_normal)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val routeInputGap = dimensionResource(R.dimen.spacing_12)
            val routeInputHeight = dimensionResource(R.dimen.icon_size_40)
            val iconSize = dimensionResource(R.dimen.icon_size_14)

            Row(
                modifier = Modifier.weight(1f)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.width(iconSize)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.width(iconSize)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.small_circle),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize)
                        )

                        Icon(
                            painter = painterResource(id = R.drawable.ic_location_on_2),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.dots_vertical),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize),
                    )
                }

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_small)))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(routeInputGap)
                ) {
                    RouteInputField(
                        text = model.startText,
                        hint = stringResource(id = R.string.start_location),
                        onTextChange = model.onStartChange,
                        onFocus = model.onStartFocused,
                        backgroundColor = inputBackground,
                        modifier = Modifier.height(routeInputHeight)
                    )

                    RouteInputField(
                        text = model.destinationText,
                        hint = stringResource(id = R.string.where_do_you_want_to_go_question),
                        onTextChange = model.onDestinationChange,
                        onFocus = model.onDestinationFocused,
                        backgroundColor = inputBackground,
                        modifier = Modifier.height(routeInputHeight)
                    )
                }
            }

            IconButton(
                onClick = model.onSwap,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(dimensionResource(R.dimen.icon_size_20))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.swap_vertical),
                    contentDescription = "Swap locations",
                    tint = iconTint
                )
            }
        }
    }
}

@Composable
private fun RouteInputField(
    text: String,
    hint: String,
    onTextChange: (String) -> Unit,
    onFocus: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clip(
                RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius))
            )
            .background(backgroundColor)
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_normal),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
            .semantics { contentDescription = hint },
        singleLine = true,
        textStyle = TripKitComposeTextStyles.current.bodyLarge.copy(
            color = colorResource(R.color.labelPrimary)
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        decorationBox = { innerTextField ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = hint,
                        style = TripKitComposeTextStyles.current.bodyLarge,
                        color = colorResource(R.color.labelSecondary).copy(
                            alpha = 0.6f
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun RouteCardContentPreview() {
    TripKitUITheme {
        RouteCardContent(
            model = RouteUiModel(
                startText = "",
                destinationText = "",
                onStartChange = {},
                onDestinationChange = {},
                onStartFocused = {},
                onDestinationFocused = {},
                onSwap = {},
                onClose = {},
                onConfirm = {},
                canConfirm = true
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun RouteCardContentFilledPreview() {
    TripKitUITheme {
        RouteCardContent(
            model = RouteUiModel(
                startText = "Current location",
                destinationText = "Central Station",
                onStartChange = {},
                onDestinationChange = {},
                onStartFocused = {},
                onDestinationFocused = {},
                onSwap = {},
                onClose = {},
                onConfirm = {},
                canConfirm = true
            )
        )
    }
}
