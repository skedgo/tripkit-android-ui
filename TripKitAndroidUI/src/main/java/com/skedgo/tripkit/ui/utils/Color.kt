package com.skedgo.tripkit.ui.utils

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