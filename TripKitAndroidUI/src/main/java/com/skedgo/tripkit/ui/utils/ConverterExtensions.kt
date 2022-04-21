package com.skedgo.tripkit.ui.utils

import java.lang.NumberFormatException


fun String.toIntSafe(): Int{
    return try {
        this.toInt()
    } catch (e: NumberFormatException){
        0
    }
}