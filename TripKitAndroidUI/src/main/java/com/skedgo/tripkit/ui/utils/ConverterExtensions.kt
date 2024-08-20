package com.skedgo.tripkit.ui.utils


fun String.toIntSafe(): Int {
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        0
    }
}