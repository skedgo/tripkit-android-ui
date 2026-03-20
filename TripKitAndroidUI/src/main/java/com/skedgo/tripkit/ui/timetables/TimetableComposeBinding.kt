package com.skedgo.tripkit.ui.timetables

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.databinding.BindingAdapter
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.core.binding.ImageViewBindingAdapters
import com.skedgo.tripkit.ui.model.TimetableHeaderLineItem

@BindingAdapter("timetableServiceViewModel")
fun bindTimetableServiceRow(
    composeView: ComposeView,
    viewModel: ServiceViewModel?
) {
    composeView.setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
    )
    composeView.setContentSafely {
        TripKitUITheme {
            if (viewModel != null) {
                TimetableServiceRow(viewModel = viewModel)
            }
        }
    }
}

@BindingAdapter("timetableHeaderItem")
fun bindTimetableHeaderItem(
    composeView: ComposeView,
    item: TimetableHeaderLineItem?
) {
    composeView.setViewCompositionStrategy(
        ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
    )
    composeView.setContentSafely {
        TripKitUITheme {
            if (item != null) {
                TimetableRouteChip(
                    serviceNumber = item.serviceNumber,
                    serviceColor = item.serviceColor
                )
            }
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
private fun TimetableServiceRow(
    viewModel: ServiceViewModel
) {
    val rowAlpha by viewModel.alpha.observeAsState(1f)
    val serviceName by viewModel.tertiaryText.observeAsState("")
    val serviceNumber by viewModel.serviceNumber.observeAsState("")
    val statusText by viewModel.secondaryText.observeAsState("")
    val statusTextColor by viewModel.secondaryTextColor.observeAsState(colorResource(id = R.color.black2).value.toInt())
    val countDownText by viewModel.countDownTimeText.observeAsState("")
    val countDownColor by viewModel.countDownTimeTextColor.observeAsState(colorResource(id = R.color.tripKitSuccess).value.toInt())
    val serviceColor by viewModel.serviceColor.observeAsState(colorResource(id = R.color.colorPrimary).value.toInt())
    val isCurrentTrip by viewModel.isCurrentTrip.observeAsState(false)
    val modeInfo by viewModel.modeInfo.observeAsState()

    TimetableServiceRowContent(
        serviceName = serviceName,
        serviceNumber = serviceNumber,
        statusText = statusText,
        statusTextColor = statusTextColor,
        countDownText = countDownText,
        countDownColor = countDownColor,
        serviceColor = serviceColor,
        isCurrentTrip = isCurrentTrip,
        modeInfo = modeInfo,
        rowAlpha = rowAlpha,
        onClick = { viewModel.onItemClick.perform() }
    )
}

@Composable
private fun TimetableServiceRowContent(
    serviceName: String,
    serviceNumber: String,
    statusText: String,
    statusTextColor: Int,
    countDownText: String,
    countDownColor: Int,
    serviceColor: Int,
    isCurrentTrip: Boolean,
    modeInfo: ModeInfo?,
    rowAlpha: Float = 1f,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.spacing_normal),
                    vertical = dimensionResource(id = R.dimen.spacing_extra_small)
                )
            .alpha(rowAlpha)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = onClick
                ),
            backgroundColor = colorResource(id = R.color.subCardBackground),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.cardview_corner_radius)),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.spacing_normal),
                        vertical = dimensionResource(id = R.dimen.spacing_small)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = serviceName,
                        style = TripKitComposeTextStyles.current.bodyLarge.copy(
                            color = colorResource(id = R.color.labelPrimary)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_xx_small)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeInfoIcon(modeInfo = modeInfo)
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.spacing_small)))
                        TimetableRouteChip(
                            serviceNumber = serviceNumber,
                            serviceColor = serviceColor
                        )
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_extra_small)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_route_check),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_20))
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.spacing_extra_small)))
                        Text(
                            text = statusText,
                            color = Color(statusTextColor),
                            style = TripKitComposeTextStyles.current.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    text = countDownText,
                    color = Color(countDownColor),
                    style = TripKitComposeTextStyles.current.labelLarge
                )
            }
        }

        if (isCurrentTrip) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(dimensionResource(id = R.dimen.icon_size_40) + dimensionResource(id = R.dimen.spacing_normal))
                    .width(dimensionResource(id = R.dimen.spacing_3))
                    .background(colorResource(id = R.color.colorAccent))
            )
        }
    }
}

@Composable
private fun TimetableRouteChip(
    serviceNumber: String,
    serviceColor: Int
) {
    Box(
        modifier = Modifier
            .background(
                Color(serviceColor),
                RoundedCornerShape(dimensionResource(id = R.dimen.cardview_corner_radius_small))
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.spacing_extra_small),
                vertical = dimensionResource(id = R.dimen.spacing_xx_small)
            )
    ) {
        Text(
            text = serviceNumber,
            color = Color.White,
            style = TripKitComposeTextStyles.current.bodySmall
        )
    }
}

@Composable
private fun ModeInfoIcon(modeInfo: ModeInfo?) {
    if (LocalInspectionMode.current) {
        Icon(
            painter = painterResource(id = R.drawable.ic_public_transport),
            contentDescription = null,
            tint = colorResource(id = R.color.black2),
            modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_20))
        )
        return
    }

    val color = colorResource(id = R.color.black2)
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { imageView ->
            ImageViewBindingAdapters.setCompatModeInfo(imageView, modeInfo)
            imageView.setColorFilter(color.value.toInt())
        },
        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_20))
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TimetableItemCardPreview() {
    TripKitUITheme {
        TimetableServiceRowContent(
            serviceName = "The Domain",
            serviceNumber = "T9",
            statusText = "On time · 18:26",
            statusTextColor = colorResource(id = R.color.labelPrimary).value.toInt(),
            countDownText = "55 min",
            countDownColor = colorResource(id = R.color.tripKitSuccess).value.toInt(),
            serviceColor = colorResource(id = R.color.colorPrimary).value.toInt(),
            isCurrentTrip = false,
            modeInfo = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TimetableHeaderPreview() {
    TripKitUITheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.white))
                .padding(dimensionResource(id = R.dimen.spacing_normal))
        ) {
            Text(
                text = "Town Hall Station",
                style = TripKitComposeTextStyles.current.titleLarge.copy(
                    color = colorResource(id = R.color.labelPrimary)
                )
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_small)))
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small))) {
                listOf("L2", "L4", "T1", "T8", "T9").forEach {
                    TimetableRouteChip(
                        serviceNumber = it,
                        serviceColor = colorResource(id = R.color.colorPrimary).value.toInt()
                    )
                }
            }
        }
    }
}
