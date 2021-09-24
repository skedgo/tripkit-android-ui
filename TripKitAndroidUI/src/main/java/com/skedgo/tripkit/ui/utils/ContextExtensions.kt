package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import com.afollestad.materialdialogs.MaterialDialog
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

fun Context.showConfirmationPopUpDialog(
        title: String? = null, message: String? = null, positiveLabel: String,
        positiveCallback: () -> Unit, negativeLabel: String,
        negativeCallback: (() -> Unit)? = null
) {
    MaterialDialog(this).show {
        title?.let { title(text = it) }
        message?.let { message(text = it) }
        positiveButton(text = positiveLabel) {
            positiveCallback()
            it.dismiss()
        }
        negativeButton(text = negativeLabel) {
            negativeCallback?.invoke()
            it.dismiss()
        }
    }
}