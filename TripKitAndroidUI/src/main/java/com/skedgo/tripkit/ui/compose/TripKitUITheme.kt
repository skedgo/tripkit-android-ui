package com.skedgo.tripkit.ui.compose

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.TripKitUITypography
import androidx.compose.ui.text.font.FontFamily

/**
 * Compose theme that mirrors the existing XML DayNight setup.
 *
 * Host AppCompat/Material attributes supply the palette. Named SDK resources remain fallbacks
 * for legacy hosts; DayNight resources and public typography/style overrides remain supported.
 */
@Composable
fun TripKitUITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textTokens: TripKitComposeTextTokens = TripKitComposeTextTokensDefaults.default(),
    textTokensOverride: TripKitComposeTextTokensOverride = TripKitComposeTextTokensOverride(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Resolve through the Activity/overlay context so explicit host appearance and branding win.
    // LocalConfiguration is read by isSystemInDarkTheme, so recreation/recomposition follows it.
    val primary = Color(context.hostThemeColor(androidx.appcompat.R.attr.colorPrimary, R.color.colorPrimary))
    val primaryVariant = Color(context.hostThemeColor(androidx.appcompat.R.attr.colorPrimaryDark, R.color.colorPrimaryDark))
    val secondary = Color(context.hostThemeColor(com.google.android.material.R.attr.colorSecondary, R.color.colorAccent))
    val background = Color(context.hostThemeColor(android.R.attr.colorBackground, R.color.white))
    val surface = Color(context.hostThemeColor(com.google.android.material.R.attr.colorSurface, R.color.searchBarBackground))
    val onBackground = Color(context.hostThemeColor(android.R.attr.textColorPrimary, R.color.black))
    val onSurface = Color(context.hostThemeColor(com.google.android.material.R.attr.colorOnSurface, R.color.black))
    val error = Color(context.hostThemeColor(androidx.appcompat.R.attr.colorError, R.color.tripKitError))
    val onPrimary = Color(context.hostThemeColor(com.google.android.material.R.attr.colorOnPrimary, R.color.labelPrimary))
    val onSecondary = Color(context.hostThemeColor(com.google.android.material.R.attr.colorOnSecondary, R.color.labelPrimary))
    val onError = Color(context.hostThemeColor(com.google.android.material.R.attr.colorOnError, R.color.labelPrimary))

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
            onPrimary = onPrimary,
            onSecondary = onSecondary,
            onBackground = onBackground,
            onSurface = onSurface,
            error = error,
            onError = onError
        )
    } else {
        lightColors(
            primary = primary,
            primaryVariant = primaryVariant,
            secondary = secondary,
            background = background,
            surface = surface,
            onPrimary = onPrimary,
            onSecondary = onSecondary,
            onBackground = onBackground,
            onSurface = onSurface,
            error = error,
            onError = onError
        )
    }

    val resolvedTextTokens = remember(textTokens, textTokensOverride) {
        textTokens.withOverride(textTokensOverride)
    }

    CompositionLocalProvider(
        LocalTripKitComposeTextTokens provides resolvedTextTokens
    ) {
        MaterialTheme(
            colors = colors,
            typography = typography,
            content = content
        )
    }
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
    textTokens: TripKitComposeTextTokens = TripKitComposeTextTokensDefaults.default(),
    textTokensOverride: TripKitComposeTextTokensOverride = TripKitComposeTextTokensOverride(),
    content: @Composable () -> Unit
) = TripKitUITheme(
    darkTheme = darkTheme,
    textTokens = textTokens,
    textTokensOverride = textTokensOverride,
    content = content
)

