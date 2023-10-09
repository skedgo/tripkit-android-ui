package com.skedgo.tripkit.ui.utils

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import java.lang.Exception

fun Context.viewAppDetailsSettingsIntent(): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", this.packageName, null)
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Context.isNetworkConnected(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        isInternetAvailable()
    } else {
        val cm: ConnectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun Context.isInternetAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

    return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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
    negativeCallback: (() -> Unit)? = null,
    cancellable: Boolean = true
) {
    MaterialDialog(this).cancelable(cancellable).show {
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

fun Context.getVersionName(): String? = packageManager?.getPackageInfo(packageName, 0)?.versionName

fun Context.deFocusAndHideKeyboard(focus: View?) {
    hideKeyboard(this, focus)
    focus?.clearFocus()
}

fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission)== PackageManager.PERMISSION_GRANTED

fun Context.openAppInPlayStore() {
    val packageName = applicationContext.packageName
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse("market://details?id=$packageName")

    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        // If the Play Store app is not available, open the Play Store website.
        val webIntent = Intent(Intent.ACTION_VIEW)
        webIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        startActivity(webIntent)
    }
}