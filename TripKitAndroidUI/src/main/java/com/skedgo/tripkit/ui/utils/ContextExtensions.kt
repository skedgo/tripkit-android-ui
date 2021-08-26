package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.GridItem
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.list.listItems

fun Context.viewAppDetailsSettingsIntent(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", this.packageName, null)
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Context.isNetworkConnected(): Boolean {
    val cm: ConnectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
}

fun Context.showSingleSelectionPopUpDialog(items: List<String>, onItemSelected: (String) -> Unit) {
    MaterialDialog(this).show {
        listItems(items = items, selection = { dialog, _, text ->
            onItemSelected.invoke(text.toString())
            dialog.dismiss()
        })
    }
}