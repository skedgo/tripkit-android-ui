package com.skedgo.tripkit.ui.search.compose

import android.util.TypedValue
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.compose.TripKitComposeTextStyles
import com.skedgo.tripkit.ui.compose.TripKitUITheme

@Composable
fun LocationSearchHeader(
    showBackButton: Boolean,
    queryHint: String?,
    onBackClick: () -> Unit,
    onSearchViewCreated: (SearchView) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val inputBackground = colorResource(R.color.inputBackground)
    val hintColor = colorResource(R.color.labelSecondary)
    val bodyLargeStyle = TripKitComposeTextStyles.current.bodyLarge

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_normal),
                end = dimensionResource(R.dimen.spacing_normal),
                top = dimensionResource(R.dimen.spacing_12),
                bottom = dimensionResource(R.dimen.spacing_small),
            ),
        horizontalArrangement =
            Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.icon_size_40))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius)))
                    .background(inputBackground)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_green),
                    contentDescription = "Back",
                    tint = Color.Unspecified
                )
            }
        }

        if (isInPreview) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(inputBackground)
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = queryHint.orEmpty(),
                    color = hintColor
                )
            }
        } else {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .height(dimensionResource(R.dimen.input_height_40))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.cardview_corner_radius)))
                    .background(inputBackground),
                factory = {
                    SearchView(context).apply {
                        isIconified = false
                        setIconifiedByDefault(false)
                        setQueryHint(queryHint)
                        setIconified(false)
                        val iconPaddingPx = (0 * context.resources.displayMetrics.density).toInt()
                        val textPaddingPx = (4 * context.resources.displayMetrics.density).toInt()

                        findViewById<android.view.View?>(androidx.appcompat.R.id.search_mag_icon)?.apply {
                            visibility = android.view.View.VISIBLE
                            setPadding(0, 0, 0, 0)
                            (layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let { params ->
                                params.marginStart = iconPaddingPx
                                layoutParams = params
                            }
                        }
                        findViewById<android.view.View?>(androidx.appcompat.R.id.search_plate)?.setBackgroundColor(
                            android.graphics.Color.TRANSPARENT
                        )
                        findViewById<android.view.View?>(androidx.appcompat.R.id.search_edit_frame)?.setPadding(
                            0,
                            0,
                            0,
                            0
                        )
                        findViewById<android.view.View?>(androidx.appcompat.R.id.submit_area)?.setBackgroundColor(
                            android.graphics.Color.TRANSPARENT
                        )
                        findViewById<SearchAutoComplete?>(androidx.appcompat.R.id.search_src_text)?.setPadding(
                            textPaddingPx,
                            0,
                            textPaddingPx,
                            0
                        )
                        findViewById<SearchAutoComplete?>(androidx.appcompat.R.id.search_src_text)?.let { textView ->
                            applyBodyLargeTextStyle(textView, bodyLargeStyle)
                            textView.setTextColor(ContextCompat.getColor(context, R.color.labelPrimary))
                            textView.setHintTextColor(ContextCompat.getColor(context, R.color.labelSecondary))
                        }
                    }.also(onSearchViewCreated)
                },
                update = { searchView ->
                    if (searchView.queryHint != queryHint) {
                        searchView.queryHint = queryHint
                    }
                    searchView.findViewById<SearchAutoComplete?>(androidx.appcompat.R.id.search_src_text)?.let { textView ->
                        applyBodyLargeTextStyle(textView, bodyLargeStyle)
                    }
                }
            )
        }
    }
}

private fun applyBodyLargeTextStyle(textView: SearchAutoComplete, style: TextStyle) {
    val fontSizeSp = style.fontSize.value
    if (fontSizeSp > 0f) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
    }

    val lineHeightSp = style.lineHeight.value
    if (lineHeightSp > 0f) {
        val lineHeightPx = (lineHeightSp * textView.resources.displayMetrics.scaledDensity).toInt()
        TextViewCompat.setLineHeight(textView, lineHeightPx)
    }

    val letterSpacingSp = style.letterSpacing.value
    if (letterSpacingSp != 0f && fontSizeSp > 0f) {
        // TextView expects letterSpacing in em; compose token is in sp.
        textView.letterSpacing = letterSpacingSp / fontSizeSp
    }

    textView.setTypeface(
        textView.typeface,
        if (style.fontWeight == FontWeight.Bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun LocationSearchHeaderPreview() {
    TripKitUITheme {
        LocationSearchHeader(
            showBackButton = true,
            queryHint = "Where do you want to go?",
            onBackClick = {},
            onSearchViewCreated = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F6)
@Composable
private fun LocationSearchHeaderNoBackPreview() {
    TripKitUITheme {
        LocationSearchHeader(
            showBackButton = false,
            queryHint = "Where do you want to go?",
            onBackClick = {},
            onSearchViewCreated = {}
        )
    }
}
