package com.skedgo.tripkit.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class TripKitComposeTextTokens(
    // Figma-aligned canonical names
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val labelLarge: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    // Tokens that don't have a matching Figma name/spec in this batch
    val bodyMdCompact: TextStyle,
    val actionMd: TextStyle,

    val valueLg: TextStyle,
    val labelLg: TextStyle,
    val countXl: TextStyle
)
@Immutable
data class TripKitComposeTextTokensOverride(
    val titleLarge: TextStyle? = null,
    val titleMedium: TextStyle? = null,
    val labelLarge: TextStyle? = null,
    val bodyLarge: TextStyle? = null,
    val bodySmall: TextStyle? = null,
    val bodyMdCompact: TextStyle? = null,
    val actionMd: TextStyle? = null,
    val valueLg: TextStyle? = null,
    val labelLg: TextStyle? = null,
    val countXl: TextStyle? = null
)

internal val LocalTripKitComposeTextTokens = staticCompositionLocalOf {
    TripKitComposeTextTokensDefaults.default()
}

object TripKitComposeTextTokensDefaults {
    fun default(): TripKitComposeTextTokens = TripKitComposeTextTokens(
        titleLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        bodyMdCompact = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp
        ),
        actionMd = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        valueLg = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp
        ),
        labelLg = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        ),
        countXl = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
    )
}

object TripKitComposeTextStyles {
    val current: TripKitComposeTextTokens
        @Composable get() = LocalTripKitComposeTextTokens.current
}

internal fun TripKitComposeTextTokens.withOverride(
    override: TripKitComposeTextTokensOverride
): TripKitComposeTextTokens {
    return copy(
        titleLarge = override.titleLarge ?: titleLarge,
        titleMedium = override.titleMedium ?: titleMedium,
        labelLarge = override.labelLarge ?: labelLarge,
        bodyLarge = override.bodyLarge ?: bodyLarge,
        bodySmall = override.bodySmall ?: bodySmall,
        bodyMdCompact = override.bodyMdCompact ?: bodyMdCompact,
        actionMd = override.actionMd ?: actionMd,
        valueLg = override.valueLg ?: valueLg,
        labelLg = override.labelLg ?: labelLg,
        countXl = override.countXl ?: countXl,
    )
}
