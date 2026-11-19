package com.skedgo.tripkit.ui.controller.homeviewcontroller

/** Keep a non-empty map viewport even when an expanded sheet covers the entire map. */
internal fun controllerMapBottomPadding(mapHeight: Int, sheetTop: Int): Int =
    (mapHeight - sheetTop).coerceIn(0, (mapHeight - 1).coerceAtLeast(0))
