
package com.skedgo.tripkit.ui.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import java.lang.Exception

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
        positiveCallback: (() -> Unit)? = null, negativeLabel: String? = null,
        negativeCallback: (() -> Unit)? = null
) {
    MaterialDialog(this).show {
        title?.let { title(text = it) }
        message?.let { message(text = it) }
        positiveButton(text = positiveLabel) {
            positiveCallback?.invoke()
            it.dismiss()
        }
        negativeLabel?.let {
            negativeButton(text = negativeLabel) {
                negativeCallback?.invoke()
                it.dismiss()
            }
        }
    }
}


fun Context.getVersionCode(): Long? {
    return try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            pInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        null
    }
}


fun Context.getAccessibilityManager(): AccessibilityManager {
    return this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
}

fun Context.isTalkBackOn(): Boolean {
    val manager = getAccessibilityManager()

    if (manager.isEnabled) {
        val serviceInfoList =
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
        if (serviceInfoList.isNotEmpty()) {
            return true
        }
    }

    return false
}