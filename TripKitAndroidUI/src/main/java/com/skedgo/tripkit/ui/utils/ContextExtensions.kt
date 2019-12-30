package com.skedgo.tripkit.ui.utils
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings

fun Context.viewAppDetailsSettingsIntent(): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", this.packageName, null)
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Context.isNetworkConnected(): Boolean {
  val cm: ConnectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  return cm.activeNetworkInfo?.isConnectedOrConnecting ?: false
}
