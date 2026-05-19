package com.skedgo.tripkit.ui.utils

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import com.skedgo.tripkit.account.data.AppColor

fun Int.getRgbFromColorResource(): AppColor {
    val red = (this shr 16) and 0xFF
    val green = (this shr 8) and 0xFF
    val blue = this and 0xFF

    return AppColor(red, green, blue)
}

fun rgbToHex(red: Int, green: Int, blue: Int): String {
    return String.format("#%02X%02X%02X", red, green, blue)
}

@ColorInt
private fun resolveTintOrNull(@ColorInt default: Int?): Int? {
    val appTint = DynamicAppColor.getAppColors()?.tintColor?.let {
        android.graphics.Color.rgb(it.red, it.green, it.blue)
    }

    val sysTint = DynamicAppColor.getSystemColors()?.tintColor

    return appTint
        ?: sysTint
        ?: default?.takeIf { it != 0 }
}

fun resolveComposeColor(@ColorInt default: Int?): Color {
    val resolved = resolveTintOrNull(default)
    return if (resolved != null) Color(resolved) else Color.Unspecified
}