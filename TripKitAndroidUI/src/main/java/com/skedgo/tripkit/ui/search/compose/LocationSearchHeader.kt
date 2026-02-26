package com.skedgo.tripkit.ui.search.compose

import androidx.appcompat.widget.SearchView
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.skedgo.tripkit.ui.R
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                    .padding(horizontal = 16.dp),
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
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(inputBackground),
                factory = {
                    SearchView(context).apply {
                        isIconified = false
                        setIconifiedByDefault(false)
                        setQueryHint(queryHint)
                        setIconified(false)
                        findViewById<android.view.View?>(androidx.appcompat.R.id.search_mag_icon)?.visibility =
                            android.view.View.GONE
                        findViewById<android.view.View?>(androidx.appcompat.R.id.search_plate)?.setBackgroundColor(
                            android.graphics.Color.TRANSPARENT
                        )
                        findViewById<android.view.View?>(androidx.appcompat.R.id.submit_area)?.setBackgroundColor(
                            android.graphics.Color.TRANSPARENT
                        )
                    }.also(onSearchViewCreated)
                },
                update = { searchView ->
                    if (searchView.queryHint != queryHint) {
                        searchView.queryHint = queryHint
                    }
                }
            )
        }
    }
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
