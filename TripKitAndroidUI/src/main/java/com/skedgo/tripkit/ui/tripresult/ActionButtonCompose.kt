package com.skedgo.tripkit.ui.tripresult

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.tripresults.actionbutton.ActionButton
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun TripSegmentActionButton(
    viewModel: ActionButtonViewModel,
    listener: ActionButtonClickListener?,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val showSpinner by viewModel.showSpinner.observeAsState()
    val title by viewModel.title.observeAsState()
    val icon by viewModel.icon.observeAsState()
    val iconTint by viewModel.iconTint.observeAsState()
    val outlineTint by viewModel.outlineTint.observeAsState()
    val backgroundTint by viewModel.backgroundTint.observeAsState()
    val actionButton by viewModel.actionButton.observeAsState()

    val resolvedTitle = title.orEmpty()
    val primaryBackground = backgroundTint?.getColorForState(
        intArrayOf(android.R.attr.state_enabled),
        colorResource(R.color.colorPrimary).toArgb()
    ) ?: colorResource(R.color.colorPrimary).toArgb()
    val isPrimary = actionButton?.isPrimary == true
    val backgroundColor = if (isPrimary) {
        ComposeColor(if (primaryBackground != 0) primaryBackground else Color.WHITE)
    } else {
        colorResource(R.color.inputBackground)
    }
    val contentColor = if (isPrimary) {
        colorResource(R.color.white)
    } else {
        colorResource(R.color.labelPrimary)
    }

    val borderStroke = if (isPrimary) {
        null
    } else {
        BorderStroke(1.dp, ComposeColor(outlineTint ?: Color.TRANSPARENT))
    }

    Box(
        modifier = Modifier.padding(
            top = dimensionResource(R.dimen.spacing_12),
            bottom = dimensionResource(R.dimen.spacing_12),
            end = dimensionResource(R.dimen.spacing_small),
        )
    ) {
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius_large)),
            color = backgroundColor,
            border = borderStroke,
            modifier = Modifier
                .semantics { contentDescription = resolvedTitle }
                .testTag(viewModel.tag),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = enabled && !showSpinner) {
                        listener?.onItemClick(viewModel.tag, viewModel, context)
                    }
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_12),
                        vertical = dimensionResource(R.dimen.spacing_small)
                    )
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = dimensionResource(R.dimen.spacing_xx_small),
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_20))
                    )
                } else {
                    val iconBitmap = remember(icon, iconTint) {
                        val tint = iconTint
                        icon?.mutate()?.let { drawable ->
                            if (tint != null && tint != 0) {
                                DrawableCompat.setTint(drawable, tint)
                            } else {
                                drawable.clearColorFilter()
                            }
                            drawable.toBitmap().asImageBitmap()
                        }
                    }
                    iconBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,

                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
                        )
                    }
                }

                if (resolvedTitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(dimensionResource(R.dimen.spacing_extra_small)))
                    Text(
                        text = resolvedTitle,
                        style = ActionButtonTextStyles.Label,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ObservableBoolean.observeAsState(): State<Boolean> {
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

internal object ActionButtonTextStyles {
    val Label
        @Composable get() = TripKitComposeTextStyles.current.labelLarge
}

@Composable
private fun ActionButtonPreviewRow(
    buttons: List<ActionButtonViewModel>,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 0.dp,
            alignment = Alignment.Start
        )
    ) {
        buttons.forEach { button ->
            TripSegmentActionButton(
                viewModel = button,
                listener = null,
                enabled = enabled
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentActionButtonPrimaryPreview() {
    val context = LocalContext.current
    TripKitUITheme {
        ActionButtonPreviewRow(
            buttons = listOf(
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.go),
                        tag = "go",
                        icon = R.drawable.ic_directions,
                        isPrimary = true
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentActionButtonSecondaryPreview() {
    val context = LocalContext.current
    TripKitUITheme {
        ActionButtonPreviewRow(
            buttons = listOf(
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.share),
                        tag = "share",
                        icon = R.drawable.ic_share,
                        isPrimary = false
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentActionButtonDisabledPreview() {
    val context = LocalContext.current
    TripKitUITheme {
        ActionButtonPreviewRow(
            buttons = listOf(
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.favourite),
                        tag = "favorite",
                        icon = R.drawable.ic_bookmark,
                        isPrimary = false
                    )
                )
            ),
            enabled = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentActionButtonLoadingPreview() {
    val context = LocalContext.current
    val vm = ActionButtonViewModel(
        context,
        ActionButton(
            text = context.getString(R.string.go),
            tag = "go",
            icon = R.drawable.ic_directions,
            isPrimary = true
        )
    ).apply { showSpinner(true) }

    TripKitUITheme {
        ActionButtonPreviewRow(buttons = listOf(vm))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun TripSegmentActionButtonRowPreview() {
    val context = LocalContext.current
    TripKitUITheme {
        ActionButtonPreviewRow(
            buttons = listOf(
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.go),
                        tag = "go",
                        icon = R.drawable.ic_directions,
                        isPrimary = true
                    )
                ),
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.favourite),
                        tag = "favorite",
                        icon = R.drawable.ic_bookmark,
                        isPrimary = false
                    )
                ),
                ActionButtonViewModel(
                    context,
                    ActionButton(
                        text = context.getString(R.string.share),
                        tag = "share",
                        icon = R.drawable.ic_share,
                        isPrimary = false
                    )
                )
            )
        )
    }
}

@Composable
private fun <T> ObservableField<T>.observeAsState(): State<T?> {
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
