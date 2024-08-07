package com.skedgo.tripkit.ui.core.permissions

import android.Manifest

const val REQUEST_LOCATION_PERMISSION = 11
const val REQUEST_CALENDAR_PERMISSION = 12

sealed class PermissionsRequest(val permissions: Array<String>, val requestCode: Int) {
    class Location : PermissionsRequest(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        REQUEST_LOCATION_PERMISSION
    )

    class Calendar : PermissionsRequest(
        arrayOf(Manifest.permission.READ_CALENDAR),
        REQUEST_CALENDAR_PERMISSION
    )
}
