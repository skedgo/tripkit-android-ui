package com.skedgo.tripkit.ui.search.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitUITheme
import com.skedgo.tripkit.ui.search.compose.styles.SearchTextStyles
import kotlin.math.roundToInt

@Composable
fun SearchResultRow(
    ui: SearchResultRowUiModel,
    modifier: Modifier = Modifier
) {
    val isInPreview = LocalInspectionMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val iconTint = colorResource(id = R.color.icon_tint_default)
    val cardRadius = dimensionResource(id = R.dimen.cardview_corner_radius)
    val rowShape = RoundedCornerShape(
        topStart = if (ui.isFirstInGroup) cardRadius else 0.dp,
        topEnd = if (ui.isFirstInGroup) cardRadius else 0.dp,
        bottomStart = if (ui.isLastInGroup) cardRadius else 0.dp,
        bottomEnd = if (ui.isLastInGroup) cardRadius else 0.dp
    )
    val dividerInset = dimensionResource(id = R.dimen.spacing_normal) +
        dimensionResource(id = R.dimen.tripkit_search_result_icon_size) +
        dimensionResource(id = R.dimen.spacing_12)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (ui.showGroupTopSpacing)
                dimensionResource(R.dimen.spacing_normal)
            else
                dimensionResource(R.dimen.spacing_xx_small)
            )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.subCardBackground), rowShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    onClick = ui.onRowClick
                )
                .padding(
                    start = dimensionResource(id = R.dimen.spacing_normal),
                    top = dimensionResource(id = R.dimen.spacing_12),
                    end = dimensionResource(id = R.dimen.spacing_normal),
                    bottom = dimensionResource(id = R.dimen.spacing_12)
                )
                .semantics {
                    contentDescription =
                        if (ui.subtitle.isNullOrEmpty()) ui.title else "${ui.title}, ${ui.subtitle}"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchResultRowIcon(
                icon = ui.icon,
                shouldTintIcon = ui.shouldTintIcon && !isInPreview,
                tint = iconTint
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(id = R.dimen.spacing_12)),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = highlightedText(ui.title, ui.matcher),
                    style = SearchTextStyles.Title.copy(
                        color = Color(ui.titleTextColor)
                    ),
                    maxLines = 1
                )
                if (!ui.subtitle.isNullOrBlank()) {
                    Text(
                        text = highlightedText(ui.subtitle, ui.matcher),
                        style = SearchTextStyles.SubTitle.copy(
                            color = Color(ui.subtitleTextColor)
                        ),
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.spacing_xx_small)),
                        maxLines = 1
                    )
                }
            }

            when {
                ui.showTimetableIcon -> {
                    RowActionIcon(
                        painter = painterResource(id = R.drawable.ic_timetable_search),
                        contentDescription = "Timetable",
                        tint = null,
                        onClick = ui.onSuggestionActionClick
                    )
                }
                ui.showInfoIcon -> {
                    RowActionIcon(
                        painter = painterResource(id = R.drawable.ic_material_info),
                        contentDescription = "Info",
                        tint = colorResource(id = R.color.colorPrimary),
                        onClick = ui.onInfoClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRowIcon(
    icon: Drawable?,
    shouldTintIcon: Boolean,
    tint: Color
) {
    val fallbackPainter = painterResource(id = R.drawable.ic_search_pin)
    val drawablePainter = rememberDrawablePainter(drawable = icon)
    val colorFilter = if (shouldTintIcon) ColorFilter.tint(tint) else null

    Image(
        painter = drawablePainter ?: fallbackPainter,
        contentDescription = null,
        colorFilter = if (drawablePainter == null) ColorFilter.tint(tint) else colorFilter,
        modifier = Modifier.size(dimensionResource(id = R.dimen.icon_size_24))
    )
}

@Composable
private fun RowActionIcon(
    painter: Painter,
    contentDescription: String,
    tint: Color?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .width(dimensionResource(id = R.dimen.icon_size_40))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = onClick
            )
            .padding(dimensionResource(id = R.dimen.spacing_extra_small)),
        contentAlignment = Alignment.Center
    ) {
        if (tint == null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
            )
        } else {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size_24))
            )
        }
    }
}

private fun highlightedText(text: String, matcher: String?): AnnotatedString {
    if (matcher.isNullOrBlank()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        val words = matcher.split(" ").filter { it.isNotBlank() }
        words.forEach { word ->
            var index = text.indexOf(word, startIndex = 0, ignoreCase = true)
            while (index >= 0) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = index,
                    end = (index + word.length).coerceAtMost(text.length)
                )
                index = text.indexOf(word, startIndex = index + word.length, ignoreCase = true)
            }
        }
    }
}

private class DrawablePainter(drawable: Drawable) : Painter() {
    private val drawable: Drawable = drawable.mutate()

    override val intrinsicSize: Size
        get() {
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            return if (width > 0 && height > 0) Size(width.toFloat(), height.toFloat()) else Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
        drawIntoCanvas { canvas -> drawable.draw(canvas.nativeCanvas) }
    }
}

@Composable
private fun rememberDrawablePainter(drawable: Drawable?): Painter? {
    return remember(drawable) { drawable?.let { DrawablePainter(it) } }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun SearchResultRowPreview() {
    TripKitUITheme {
        SearchResultRow(
            ui = SearchResultRowUiModel(
                id = "preview_info",
                title = "Current location",
                subtitle = null,
                matcher = "current",
                titleTextColor = 0xFF2E2F31.toInt(),
                subtitleTextColor = 0xFF585B62.toInt(),
                icon = null,
                shouldTintIcon = false,
                groupKind = SearchResultGroupKind.FIXED,
                isFirstInGroup = true,
                isLastInGroup = false,
                showGroupTopSpacing = false,
                showTimetableIcon = false,
                showInfoIcon = false,
                onRowClick = {},
                onSuggestionActionClick = {},
                onInfoClick = {}
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun SearchResultRowTimetablePreview() {
    TripKitUITheme {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(12.dp)
        ) {
            SearchResultRow(
                ui = SearchResultRowUiModel(
                    id = "preview_timetable",
                    title = "Central Station",
                    subtitle = "12 Vale street",
                    matcher = "central",
                    titleTextColor = 0xFF2E2F31.toInt(),
                    subtitleTextColor = 0xFF585B62.toInt(),
                    icon = null,
                    shouldTintIcon = true,
                    groupKind = SearchResultGroupKind.GOOGLE_AND_TRIPGO,
                    isFirstInGroup = false,
                    isLastInGroup = false,
                    showGroupTopSpacing = true,
                    showTimetableIcon = true,
                    showInfoIcon = false,
                    onRowClick = {},
                    onSuggestionActionClick = {},
                    onInfoClick = {}
                )
            )

            SearchResultRow(
                ui = SearchResultRowUiModel(
                    id = "preview_timetable",
                    title = "Central Station",
                    subtitle = "12 Vale street",
                    matcher = "central",
                    titleTextColor = 0xFF2E2F31.toInt(),
                    subtitleTextColor = 0xFF585B62.toInt(),
                    icon = null,
                    shouldTintIcon = true,
                    groupKind = SearchResultGroupKind.GOOGLE_AND_TRIPGO,
                    isFirstInGroup = false,
                    isLastInGroup = true,
                    showGroupTopSpacing = false,
                    showTimetableIcon = true,
                    showInfoIcon = false,
                    onRowClick = {},
                    onSuggestionActionClick = {},
                    onInfoClick = {},
                )
            )
        }
    }
}
