package com.skedgo.tripkit.ui.utils

fun Int.ordinalSuffix(): String {
    val j = this % 10
    val k = this % 100
    if (j == 1 && k != 11) {
        return String.format("%dst", this)
    }
    if (j == 2 && k != 12) {
        return String.format("%dnd", this)
    }
    if (j == 3 && k != 13) {
        return String.format("%drd", this)
    }
    return String.format("%dth", this)
}