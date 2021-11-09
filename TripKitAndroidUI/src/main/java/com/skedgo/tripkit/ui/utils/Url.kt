package com.skedgo.tripkit.ui.utils

import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.URLUtil

fun String.isAppInstalledById(packageManager: PackageManager): Boolean {
    return try {
        packageManager.getPackageInfo(this, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun String.isAppInstalled(packageManager: PackageManager): Boolean {

    return getPackageNameFromStoreUrl()?.let {
        try {
            packageManager.getPackageInfo(it, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    } ?: false
}

fun String.getPackageNameFromStoreUrl(): String? {
    return try {
        Uri.parse(this).getQueryParameter("id") ?: Uri.parse(this).getQueryParameter("apn")
    }catch (e: Exception){
        e.printStackTrace()
        null
    }
}

fun String.checkUrl(): String? {
    return if (URLUtil.isNetworkUrl(this) || this.contains("://") || this.contains("tel:")) {
        this
    } else {
        val guessUrl = URLUtil.guessUrl(this)
        guessUrl
    }
}