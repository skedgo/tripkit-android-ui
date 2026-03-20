package com.skedgo.tripkit.ui.tripresult

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme

@BindingAdapter(
    value = [
        "abListener",
        "abViewModel",
        "abTitle",
        "abIcon",
        "abShowSpinner",
        "abBackground",
    ],
    requireAll = false
)
fun bindActionButtonCompose(
    composeView: ComposeView,
    listener: ActionButtonClickListener?,
    viewModel: ActionButtonViewModel?,
    title: ObservableField<String>?,
    icon: ObservableField<Drawable>?,
    showSpinner: ObservableBoolean?,
    background: ObservableField<Drawable>?,
) {
    composeView.setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
    )
    val titleValue = title?.get().orEmpty()
    val iconValue = icon?.get()
    val showSpinnerValue = showSpinner?.get() == true
    val backgroundDrawable = background?.get()

    composeView.setContentSafely {
        TripKitUITheme {
            ActionButtonCompose(
                title = titleValue,
                icon = iconValue,
                showSpinner = showSpinnerValue,
                backgroundDrawable = backgroundDrawable,
                onClick = {
                    if (viewModel != null) {
                        listener?.onItemClick(viewModel.tag, viewModel, composeView.context)
                    }
                }
            )
        }
    }
}

private fun ComposeView.setContentSafely(content: @Composable () -> Unit) {
    post {
        if (isAttachedToWindow) {
            setContent(content)
            return@post
        }
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                removeOnAttachStateChangeListener(this)
                post {
                    if (isAttachedToWindow) {
                        setContent(content)
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        addOnAttachStateChangeListener(listener)
    }
}

@Composable
private fun ActionButtonCompose(
    title: String,
    icon: Drawable?,
    showSpinner: Boolean,
    backgroundDrawable: Drawable?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                colorResource(id = R.color.inputBackground),
                RoundedCornerShape(dimensionResource(id = R.dimen.cardview_corner_radius_medium))
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.spacing_12),
                vertical = dimensionResource(id = R.dimen.spacing_small)
            )
            .clickable(
                enabled = !showSpinner,
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_20)),
                color = colorResource(id = R.color.tripKitSuccess),
                strokeWidth = dimensionResource(id = R.dimen.spacing_xx_small)
            )
        } else if (icon != null) {
            Box(
                modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_24)),
                contentAlignment = Alignment.Center
            ) {
                if (backgroundDrawable != null) {
                    AndroidView(
                        factory = { ImageView(it) },
                        update = { imageView -> imageView.setImageDrawable(backgroundDrawable) },
                        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_24))
                    )
                }
                AndroidView(
                    factory = { ImageView(it) },
                    update = { imageView ->
                        imageView.setImageDrawable(icon)
                        imageView.imageTintList = null
                    },
                    modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_24))
                )
            }
        } else {
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_20)))
        }

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.spacing_small)))
        Text(
            text = title,
            style = TripKitComposeTextStyles.current.labelLarge.copy(
                color = colorResource(id = R.color.labelPrimary)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionButtonPreview() {
    val context = LocalContext.current
    TripKitUITheme {
        ActionButtonCompose(
            title = stringResource(id = R.string.go),
            icon = ContextCompat.getDrawable(context, R.drawable.ic_directions),
            showSpinner = false,
            backgroundDrawable = null,
            onClick = {}
        )
    }
}
