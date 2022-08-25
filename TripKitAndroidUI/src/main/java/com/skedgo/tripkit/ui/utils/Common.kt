package com.skedgo.tripkit.ui.utils

//To handle currency symbol if passed is words.
//Add more currency to handle
fun String.getCurrencySymbol(): String {
    return if (this == "USD") "$"
    else this
}