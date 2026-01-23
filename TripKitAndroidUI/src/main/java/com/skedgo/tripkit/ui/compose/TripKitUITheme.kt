package com.skedgo.tripkit.ui.compose

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.content.res.ResourcesCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.TripKitUITypography
import androidx.compose.ui.text.font.FontFamily

/**
 * Compose theme that mirrors the existing XML DayNight setup.
 *
 * It intentionally reads from `R.color.*` so:
 * - `values-night/` overrides work automatically
 * - host app white-labels overriding the same resource names keep working
 */
@Composable
fun TripKitUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val primary = colorResource(id = R.color.colorPrimary)
    val primaryVariant = colorResource(id = R.color.colorPrimaryDark)
    val secondary = colorResource(id = R.color.colorAccent)

    // In this project "white"/"black" are DayNight-aware resource aliases:
    // - values/white = #FFFFFF, values-night/white = dark surface
    // - values/black = dark text, values-night/black = #FFFFFF
    val background = colorResource(id = R.color.white)
    val surface = colorResource(id = R.color.searchBarBackground)
    val onBackground = colorResource(id = R.color.black)
    val onSurface = colorResource(id = R.color.black)

    val error = colorResource(id = R.color.tripKitError)

    val fontFamily: FontFamily = remember(TripKitUITypography.getFontResId(), TripKitUITypography.getFontFamilyName()) {
        val fontResId = TripKitUITypography.getFontResId()
        val familyName = TripKitUITypography.getFontFamilyName()

        val typeface = when {
            fontResId != null -> runCatching { ResourcesCompat.getFont(context, fontResId) }.getOrNull()
            familyName != null -> Typeface.create(familyName, Typeface.NORMAL)
            else -> null
        }
        if (typeface != null) FontFamily(typeface) else FontFamily.Default
    }

    val typography = remember(fontFamily) {
        Typography(defaultFontFamily = fontFamily)
    }

    val colors = if (darkTheme) {
        darkColors(
            primary = primary,
            primaryVariant = primaryVariant,
            secondary = secondary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = onBackground,
            onSurface = onSurface,
            error = error,
            onError = Color.White
        )
    } else {
        lightColors(
            primary = primary,
            primaryVariant = primaryVariant,
            secondary = secondary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = onBackground,
            onSurface = onSurface,
            error = error,
            onError = Color.White
        )
    }

    MaterialTheme(
        colors = colors,
        typography = typography,
        content = content
    )
}

/**
 * Backwards-compatible alias. Prefer [TripKitUITheme].
 */
@Deprecated(
    message = "Renamed to TripKitUITheme",
    replaceWith = ReplaceWith("TripKitUITheme(darkTheme, content)")
)
@Composable
fun TripKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = TripKitUITheme(darkTheme = darkTheme, content = content)

